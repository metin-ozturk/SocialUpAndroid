package com.jora.socialup.fragments.eventFeedAndDetail

// FIX : WHEN DOWNLOADING SEARCH EVENTS, DOWNLOAD EVENT AND EVENT SPECIFIC INFORMATION TOGETHER

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import androidx.fragment.app.Fragment
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
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.activities.EventCreateActivity
import com.jora.socialup.activities.HomeActivity
import com.jora.socialup.activities.HomeFeedActivity
import com.jora.socialup.helpers.OnGestureTouchListener
import com.jora.socialup.models.Event
import com.jora.socialup.viewModels.EventViewModel
import kotlinx.android.synthetic.main.fragment_event.view.*

class EventFragment : Fragment() {
    private val eventTag = "EventTag"

    private val viewModel : EventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(EventViewModel::class.java)
    }
    private var eventsRecyclerViewAdapter : EventsRecyclerViewAdapter? =  null

    private var eventsArray = ArrayList<Event>()
    private var combinedSearchResult = ArrayList<ArrayList<Any>>()

    private val index = Client("3UQQK7YRC5", "2b6893313fb23c6d06eed7c75730d41e").getIndex( "usersAndEvents")

    private val firebaseAuthentication : FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private var viewToBeCreated: View? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context !is HomeFeedActivity)
            throw RuntimeException(context.toString() + " must implement an Activity")

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previouslyDownloadedEvents = viewModel.eventsArray.value ?: ArrayList()
        if (previouslyDownloadedEvents.size != 0) return
        viewModel.downloadEvents()
        viewModel.downloadCurrentUserProfilePhoto()

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


        viewToBeCreated?.eventFeedCreateEventImageView?.setOnClickListener {
            startActivity(Intent(activity!!, EventCreateActivity::class.java))
        }


        return viewToBeCreated
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


    private fun getEventsLiveDataFromEventViewModel() {

        if (viewModel.currentUserImage.value == null) {
            viewModel.currentUserImage.observe(activity!!, Observer<Bitmap> { userImage ->
                viewToBeCreated?.eventFeedFounderImageView?.setImageBitmap(userImage)
            })
        } else {
            val userImageToBeRetrieved = viewModel.currentUserImage.value!!
            viewToBeCreated?.eventFeedFounderImageView?.setImageBitmap(userImageToBeRetrieved)
        }

        if (viewModel.eventsArray.value == null) {
            viewModel.eventsArray.observe(activity!!, Observer<ArrayList<Event>> { retrievedEventsArray ->
                if (!retrievedEventsArray.isNullOrEmpty()) {
                    eventsArray.add(retrievedEventsArray.last())
                    eventsRecyclerViewAdapter?.notifyItemChanged(eventsArray.count() - 1)
                }

            })
        } else {
            val eventsRetrieved = viewModel.eventsArray.value ?: ArrayList()
            eventsRetrieved.forEach {
                eventsArray.add(it)
                eventsRecyclerViewAdapter?.notifyItemChanged(eventsArray.count() - 1)
            }

            viewToBeCreated?.eventFeedSearchRecycleView?.scrollToPosition(viewModel.lastFocusedRow.value ?: 0) // Set which row to focused when fragment is reloaded
        }
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

        val layoutManager = LinearLayoutManager(activity)

        val point = Point()
        activity?.windowManager?.defaultDisplay?.getSize(point)
        val heightOfEventsRecycler = point.x * 9 / 16

        viewToBeCreated?.eventFeedRecyclerView?.apply {
            this.layoutManager = layoutManager
            itemAnimator = DefaultItemAnimator()
            eventsRecyclerViewAdapter =
                EventsRecyclerViewAdapter(eventsArray, heightOfEventsRecycler)
            adapter = eventsRecyclerViewAdapter
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
                        downloadEventSpecificInformationAndUpdateViewModel(position)
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


        viewModel.assertWhichRowToBeFocused(position ?: 0)

        val eventID = event?.iD ?: return


        FirebaseFirestore.getInstance().collection("users").document(firebaseAuthentication?.currentUser?.uid ?: "")
            .collection("events").document(eventID).get().addOnSuccessListener {
                val data = it.data

                val eventDates = event.date
                val eventResponseStatus = (data?.get("EventStatus") as Long?)?.toInt() ?: 0
                val votedDates = mutableMapOf<String, Boolean>() // [DATE, WHETHER THIS DATE IS VOTED AS BOOLEAN]

                eventDates?.forEach { date ->
                    votedDates[date] = (data?.get(date) as Boolean?) ?: false
                }

                val isFavorite = data?.get("EventIsFavorite") as Boolean

                viewModel.assertIsFavorite(isFavorite)
                viewModel.assertWhichDatesToBeUpdated(votedDates)
                viewModel.assertEventResponseStatus(eventResponseStatus)


                val eventDetailFragment = EventDetailFragment()
                val transaction = activity?.supportFragmentManager?.beginTransaction()
                transaction?.replace(R.id.homeRootFrameLayout, eventDetailFragment)
                transaction?.commit()
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

                            eventFeedSearchView.clearFocus()
                            Event.downloadEventInformation(searchedEventID[position]) {
                                viewModel.assertWhichViewToBeShowed(it)
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
            setIconifiedByDefault(false)
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
                    eventFeedSearchView.clearFocus()
                    eventFeedSearchView.setQuery("", true)
                    return false
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