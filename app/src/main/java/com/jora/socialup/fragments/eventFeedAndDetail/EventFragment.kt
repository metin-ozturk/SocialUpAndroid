package com.jora.socialup.fragments.eventFeedAndDetail

// FIX : WHEN DOWNLOADING SEARCH EVENTS, DOWNLOAD EVENT AND EVENT SPECIFIC INFORMATION TOGETHER

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.opengl.Visibility
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.SearchView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.algolia.search.saas.Client
import com.algolia.search.saas.Query
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jora.socialup.adapters.EventsRecyclerViewAdapter
import com.jora.socialup.adapters.EventSearchRecyclerViewAdapter
import com.jora.socialup.R
import com.jora.socialup.activities.EventCreateActivity
import com.jora.socialup.activities.HomeActivity
import com.jora.socialup.activities.HomeFeedActivity
import com.jora.socialup.helpers.*
import com.jora.socialup.models.Event
import com.jora.socialup.models.EventResponseStatus
import com.jora.socialup.viewModels.EventViewModel
import kotlinx.android.synthetic.main.fragment_event.view.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


class EventFragment : Fragment() {
    private val eventTag = "EventTag"

    private val viewModel : EventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(EventViewModel::class.java)
    }
    private var eventsRecyclerViewAdapter : EventsRecyclerViewAdapter? =  null

    private var eventsArray = ArrayList<Event>()
    private var combinedSearchResult = ArrayList<ArrayList<Any>>()
    private var feedLayoutManager : LinearLayoutManager? = null
    private val eventsNumberToDownloadPerRefresh = 5

    private val index = Client("3UQQK7YRC5", "2b6893313fb23c6d06eed7c75730d41e").getIndex( "usersAndEvents")

    private val firebaseAuthentication : FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val favoriteEventsMenuTag = "favoriteEventsMenu"


    private var viewToBeCreated: View? = null

    private var progressBarFragmentDialog: ProgressBarFragmentDialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context !is HomeFeedActivity)
            throw RuntimeException(context.toString() + " must implement an Activity")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previouslyDownloadedEvents = viewModel.eventsArray.value ?: ArrayList()
        if (previouslyDownloadedEvents.size != 0) return
        viewModel.downloadEvents(0, 5)
        viewModel.downloadCurrentUserProfilePhoto(firebaseAuthentication.currentUser?.uid ?: "")

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_event, container,false)

        val userID = firebaseAuthentication.currentUser?.uid


        userID?.also {
            FirebaseStorage.getInstance().reference.child("Images/Users/$userID/profilePhoto.jpeg").getBytes(1024 * 1024).addOnSuccessListener {
                val profilePhotoAsBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                viewToBeCreated?.eventFeedFounderImageView?.setImageBitmap(profilePhotoAsBitmap)
            }
        }

        setEventRecyclerView()
        setSearchView()

        val customSearchAdapter = EventSearchRecyclerViewAdapter(ArrayList())
        viewToBeCreated?.eventFeedSearchRecycleView?.adapter = customSearchAdapter

        setSearchRecyclerView()
        setSearchViewListeners(customSearchAdapter)

        setEventRecyclerViewListener()
        getEventsLiveDataFromEventViewModel()
        setLogOut()
        setCreateEventFavoriteEventsButton()
        setProgressBar()
        getFavoriteEvents()

        viewToBeCreated?.eventFeedProgressBar?.visibility = View.GONE

        return viewToBeCreated
    }

    override fun onPause() {
        super.onPause()
        if (progressBarFragmentDialog?.isLoadingInProgress == true) progressBarFragmentDialog?.dismiss()

        viewModel.eventsArray.removeObservers(activity!!)
        viewToBeCreated = null


        // If favoriteEventsMenu fragment is being shown, remove it
        activity?.supportFragmentManager?.findFragmentByTag(favoriteEventsMenuTag)?.also {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.remove(it)
            transaction?.commit()
        }
    }

    private fun setProgressBar() {
        progressBarFragmentDialog = ProgressBarFragmentDialog.newInstance(
            object: ProgressBarFragmentDialog.ProgressBarFragmentDialogInterface {
                override fun onCancel() {

                }

                override fun onDialogFragmentDestroyed() {
                    progressBarFragmentDialog = null
                }
            })
    }

    private fun setCreateEventFavoriteEventsButton() {
        viewToBeCreated?.eventFeedCreateEventImageView?.setOnTouchListener(OnGestureTouchListener(activity!!, object: OnGestureTouchListener.OnGestureInitiated {
            override fun longPressed() {
                super.longPressed()

                val transaction = activity?.supportFragmentManager?.beginTransaction()

                if (activity?.supportFragmentManager?.findFragmentByTag(favoriteEventsMenuTag) == null) {
                    val favoriteEventsMenu = FavoriteEventsMenu.newInstance(object: FavoriteEventsMenu.FavoriteEventsMenuInterface {
                        override fun favoriteEventClicked() {
                            progressBarFragmentDialog?.show(fragmentManager ?: return, null)
                            downloadEventSpecificInformationAndUpdateViewModel()
                        }

                    })
                    transaction?.add(R.id.favorite_events_menu_frame_layout, favoriteEventsMenu,
                        favoriteEventsMenuTag)
                    transaction?.commit()

                } else {
                    val fragment = activity?.supportFragmentManager?.findFragmentByTag(favoriteEventsMenuTag) as FavoriteEventsMenu
                    fragment.makeFavoriteEventsDisappear {
                        transaction?.remove(fragment)
                        transaction?.commit()
                    }
                }

            }

            override fun singleTappedConfirmed() {
                super.singleTappedConfirmed()
                startActivity(Intent(activity!!, EventCreateActivity::class.java))
            }
        }))
    }

    private fun setLogOut() {
        viewToBeCreated?.eventFeedFounderImageView?.setOnTouchListener(OnGestureTouchListener(activity!!, object: OnGestureTouchListener.OnGestureInitiated {
            override fun longPressed() {
                super.longPressed()
                // SIGN OUT FROM GOOGLE
                val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.google_sign_in_server_client_id))
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(activity!!, googleSignInOptions).signOut()

                // SIGN OUT FROM FACEBOOK
                LoginManager.getInstance().logOut()

                // SIGN OUT FROM FIREBASE
                FirebaseAuth.getInstance().signOut()

                startActivity(Intent(activity, HomeActivity::class.java))
            }
        }))
    }

    private fun getFavoriteEvents() {
        if (!viewModel.isFavoriteEventsToBeDownloaded) return
        else viewModel.isFavoriteEventsToBeDownloaded = false

        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users").document(userID)
            .collection("events").whereEqualTo("EventIsFavorite", true).get()
            .addOnSuccessListener { querySnapshot ->
                viewModel.favoriteEvents = ArrayList()
                querySnapshot.documents.forEach { documentSnapshot ->
                    documentSnapshot.id.also { documentId ->
                        Event.downloadEventInformation(documentId) { retrievedEvent ->
                            viewModel.favoriteEvents.add(retrievedEvent)
                        }
                    }
                }
            }
    }


    private fun getEventsLiveDataFromEventViewModel() {

        viewModel.currentUserImage.observeOnce(activity!!) {
            viewToBeCreated?.eventFeedFounderImageView?.setImageBitmap(it)
        }


        viewModel.eventsArray.observe(activity!!, Observer<ArrayList<Event>> { retrievedEventsArray ->
            if (retrievedEventsArray.size == eventsArray.size + 1) {
                // if a new event is downloaded, add it to the events array
                eventsArray.add(retrievedEventsArray.last())
                eventsRecyclerViewAdapter?.notifyItemChanged(eventsArray.count() - 1)
            } else {
                retrievedEventsArray.forEach {
                    eventsArray.add(it)
                    eventsRecyclerViewAdapter?.notifyItemChanged(eventsArray.count() - 1)
                }

                // When returning to the feed, display last focused row at top
                val positionToBeScrolled = viewModel.lastFocusedRow.value ?: 0
                viewToBeCreated?.eventFeedRecyclerView?.scrollToPosition(positionToBeScrolled)
            }

        })

    }

    private fun downloadSearchResult(searchText: String, completion: (ArrayList<ArrayList<Any>>) -> Unit) {
        index.searchAsync(Query(searchText)) { jsonObject, algoliaException ->
            if (algoliaException != null) {
                Log.d(tag, "ERROR WHILE DOWNLOADING SEARCH", algoliaException)
                return@searchAsync
            }
            val resultArray = ArrayList<String>()
            val objectIDsArray = ArrayList<String>()
            val isEvent = ArrayList<Boolean>()

            val hits = jsonObject?.getJSONArray("hits")

            for (hitCount in 0 until (hits?.length() ?: 0) ) {
                val downloadedJSONObject = hits?.getJSONObject(hitCount)
                val downloadedResult : String

                downloadedResult = when {
                    downloadedJSONObject?.isNull("UserName") == false -> {
                        isEvent.add(false)
                        downloadedJSONObject.getString("UserName")
                    }
                    downloadedJSONObject?.isNull("EventName") == false -> {
                        isEvent.add(true)
                        downloadedJSONObject.getString("EventName")
                    } else -> ""
                }

                val downloadedObjectID = downloadedJSONObject?.getString("objectID") ?: return@searchAsync

                resultArray.add(downloadedResult)
                objectIDsArray.add(downloadedObjectID)
            }
            val combinedArray =  ArrayList<ArrayList<Any>>()
            combinedArray.add(resultArray as ArrayList<Any>) //  event or user name
            combinedArray.add(isEvent as ArrayList<Any>) //  true if is event
            combinedArray.add(objectIDsArray as ArrayList<Any>) //  event's or user's id
            completion(combinedArray)
        }

    }

    private fun setEventRecyclerView() {

        feedLayoutManager = LinearLayoutManager(activity)

        val point = Point()
        activity?.windowManager?.defaultDisplay?.getSize(point)
        val heightOfEventsRecycler = point.x * 9 / 16

        viewToBeCreated?.eventFeedRecyclerView?.apply {
            layoutManager = feedLayoutManager
            itemAnimator = DefaultItemAnimator()
            eventsRecyclerViewAdapter =
                EventsRecyclerViewAdapter(eventsArray, heightOfEventsRecycler)
            adapter = eventsRecyclerViewAdapter
        }

        viewToBeCreated?.eventFeedRecyclerView?.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && viewModel.isDownloadingMoreEvents.value == false && eventsArray.size == (feedLayoutManager?.findLastVisibleItemPosition() ?: 0) + 1) {
                    // Scrolling Up, No other event is being downloaded and user viewed the bottom event.
                    viewModel.downloadEvents(eventsArray.size, eventsArray.size + eventsNumberToDownloadPerRefresh)
                    showProgressBarWhenDownloadingMoreEvents()
                }
            }
        })
    }

    private fun showProgressBarWhenDownloadingMoreEvents() {
        // To make layout animate, add: " android:animateLayoutChanges="true" " to layout's xml.
        val constraintSetTo = ConstraintSet()
        val progressBarHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics)

        constraintSetTo.connect(viewToBeCreated?.eventFeedRecyclerView?.id ?: return, ConstraintSet.START, viewToBeCreated?.eventFeedFragmentConstraintLayout?.id ?: return, ConstraintSet.START, 8)
        constraintSetTo.connect(viewToBeCreated?.eventFeedRecyclerView?.id ?: return, ConstraintSet.END, viewToBeCreated?.eventFeedFragmentConstraintLayout?.id ?: return, ConstraintSet.END, 8)
        constraintSetTo.connect(viewToBeCreated?.eventFeedRecyclerView?.id ?: return, ConstraintSet.TOP, viewToBeCreated?.eventFeedSearchView?.id ?: return, ConstraintSet.BOTTOM, 8)
        constraintSetTo.connect(viewToBeCreated?.eventFeedRecyclerView?.id ?: return, ConstraintSet.BOTTOM, viewToBeCreated?.eventFeedFragmentConstraintLayout?.id ?: return, ConstraintSet.BOTTOM, progressBarHeight.toInt())
        constraintSetTo.applyTo(viewToBeCreated?.eventFeedFragmentConstraintLayout)

        viewToBeCreated?.eventFeedProgressBar?.visibility = View.VISIBLE
        viewToBeCreated?.eventFeedCreateEventImageView?.visibility = View.INVISIBLE

        val bgScope = CoroutineScope(Dispatchers.IO)
        bgScope.launch {
            delay(1000)

            val constraintSetFrom = ConstraintSet()
            constraintSetFrom.connect(viewToBeCreated?.eventFeedRecyclerView?.id ?: return@launch, ConstraintSet.START, viewToBeCreated?.eventFeedFragmentConstraintLayout?.id ?: return@launch, ConstraintSet.START, 8)
            constraintSetFrom.connect(viewToBeCreated?.eventFeedRecyclerView?.id ?: return@launch, ConstraintSet.END, viewToBeCreated?.eventFeedFragmentConstraintLayout?.id ?: return@launch, ConstraintSet.END, 8)
            constraintSetFrom.connect(viewToBeCreated?.eventFeedRecyclerView?.id ?: return@launch, ConstraintSet.TOP, viewToBeCreated?.eventFeedSearchView?.id ?: return@launch, ConstraintSet.BOTTOM, 8)
            constraintSetFrom.connect(viewToBeCreated?.eventFeedRecyclerView?.id ?: return@launch, ConstraintSet.BOTTOM, viewToBeCreated?.eventFeedFragmentConstraintLayout?.id ?: return@launch, ConstraintSet.BOTTOM, 8)

            withContext(Dispatchers.Main) {
                constraintSetFrom.applyTo(viewToBeCreated?.eventFeedFragmentConstraintLayout)
                viewToBeCreated?.eventFeedProgressBar?.visibility = View.GONE
                viewToBeCreated?.eventFeedCreateEventImageView?.visibility = View.VISIBLE
                cancel()
            }
        }
    }



    private fun setEventRecyclerViewListener() {
        val recyclerView = viewToBeCreated?.eventFeedRecyclerView ?: return
        recyclerView.addOnItemTouchListener(
            RecyclerItemClickListener(
                context!!,
                recyclerView,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int) {
                        if (progressBarFragmentDialog?.isLoadingInProgress == false) {
                            progressBarFragmentDialog?.show(fragmentManager ?: return, null)
                            downloadEventSpecificInformationAndUpdateViewModel(position)
                        }
                    }

                })
        )
    }

    private fun downloadEventSpecificInformationAndUpdateViewModel(position: Int? = null) {

        val event : Event?

        // If event is selected from search results - it is else block, else if it is selected from feed - it is the first block.
        if (position != null) {
            val eventsArray = viewModel.eventsArray.value ?: ArrayList()
            event = eventsArray[position]
            viewModel.assertWhichViewToBeShowed(event)
        } else {
            event = viewModel.event.value
        }


        // If position is -1 then event was searched otherwise it was selected from the feed
        viewModel.assertWhichRowToBeFocused(position ?: -1)

        val eventID = event?.iD ?: return


        FirebaseFirestore.getInstance().collection("users").document(firebaseAuthentication.currentUser?.uid ?: "")
            .collection("events").document(eventID).get().addOnSuccessListener {
                val data = it.data

                val eventDates = event.date
                val eventResponseStatusAsInt = (data?.get("EventResponseStatus") as Long?)?.toInt() ?: 0
                val votedDates = mutableMapOf<String, Boolean>() // [DATE, WHETHER THIS DATE IS VOTED AS BOOLEAN]

                eventDates?.forEach { date ->
                    votedDates[date] = (data?.get(date) as Boolean?) ?: false
                }

                val isFavorite = data?.get("EventIsFavorite") as Boolean

                viewModel.isFavorite = isFavorite
                viewModel.assertWhichDatesToBeUpdated(votedDates)

                when(eventResponseStatusAsInt) {
                    1 -> EventResponseStatus.NotGoing
                    2 -> EventResponseStatus.Maybe
                    3 -> EventResponseStatus.Going
                    else -> EventResponseStatus.NotResponded
                }.also {eventResponseStatus -> viewModel.assertEventResponseStatus(eventResponseStatus) }


                val eventDetailFragment = EventDetailFragment()
                val transaction = activity?.supportFragmentManager?.beginTransaction()
                transaction?.replace(R.id.homeRootFrameLayout, eventDetailFragment)
                transaction?.commit()

                progressBarFragmentDialog?.dismiss()
            }
    }

    private fun setSearchRecyclerView() {

        viewToBeCreated?.eventFeedSearchRecycleView?.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = LinearLayoutManager(context!!)
            val searchRecyclerView = eventFeedSearchRecycleView

            addOnItemTouchListener(
                RecyclerItemClickListener(
                    context!!,
                    searchRecyclerView,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int) {
                            val isEvent = combinedSearchResult[1] as ArrayList<Boolean> // true if it is event
                            val searchedEventID = combinedSearchResult[2] as ArrayList<String>

                            if (!isEvent[position]) return

                            searchRecyclerView.clearFocus()

                            Event.downloadEventInformation(searchedEventID[position]) {
                                viewModel.assertWhichViewToBeShowed(it)
                                progressBarFragmentDialog?.show(fragmentManager ?: return@downloadEventInformation, null)
                                downloadEventSpecificInformationAndUpdateViewModel()

                            }

                        }
                    })
            )
        }

    }

    private fun setSearchView(){
        val searchManager = context?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchableInfo = searchManager.getSearchableInfo(activity?.componentName)

        viewToBeCreated?.eventFeedSearchView?.apply {
            setSearchableInfo(searchableInfo)
            isIconified = false
            isIconifiedByDefault = false
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            clearFocus()
        }
    }

    private fun setSearchViewListeners(eventSearchRecyclerViewAdapter: EventSearchRecyclerViewAdapter) {

        viewToBeCreated?.apply {

            eventFeedSearchView.setOnClickListener {
                eventFeedSearchView.requestFocus()
            }

            val searchCloseButtonID = eventFeedSearchView.context.resources.getIdentifier("android:id/search_close_btn", null, null)
            val searchCloseButton = eventFeedSearchView.findViewById<ImageView>(searchCloseButtonID)
            searchCloseButton.setOnClickListener {
                eventFeedSearchView.setQuery("", true)
                eventFeedSearchView.clearFocus()
            }


            eventFeedSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    val isEvent = combinedSearchResult[1] as ArrayList<Boolean>
                    val searchedEventID = combinedSearchResult[2] as ArrayList<String>

                    if (isEvent.run { isNotEmpty() && first() }) {
                        progressBarFragmentDialog?.show(fragmentManager ?: return true, null)
                        Event.downloadEventInformation(searchedEventID.first()) {
                            viewModel.assertWhichViewToBeShowed(it)
                            downloadEventSpecificInformationAndUpdateViewModel()
                        }
                    }

                    eventFeedSearchView.clearFocus()
                    eventFeedSearchView.setQuery("", true)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText?.isEmpty() == true) {
                        eventFeedSearchView.clearFocus()
                        eventSearchRecyclerViewAdapter.showResults(ArrayList())
                        return false

                    }

                    downloadSearchResult(newText ?: "") {
                        combinedSearchResult = it
                        eventSearchRecyclerViewAdapter.showResults(combinedSearchResult[0] as ArrayList<String>)
                    }

                    return false
                }


            })
        }
    }
}