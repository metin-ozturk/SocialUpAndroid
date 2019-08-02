package com.jora.socialup.fragments.eventFeedAndDetail

// FIX : WHEN DOWNLOADING SEARCH EVENTS, DOWNLOAD EVENT AND EVENT SPECIFIC INFORMATIN TOGETHER

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import com.google.firebase.firestore.FirebaseFirestore
import com.jora.socialup.adapters.EventsRecyclerViewAdapter
import com.jora.socialup.adapters.EventSearchRecyclerViewAdapter
import com.jora.socialup.R
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.activities.EventCreateActivity
import com.jora.socialup.activities.MainActivity
import com.jora.socialup.models.Event
import com.jora.socialup.viewModels.EventViewModel
import kotlinx.android.synthetic.main.fragment_event.*

class EventFragment : Fragment() {
    private val eventTag = "EventTag"

    private val eventsRecyclerView : RecyclerView by lazy {
        view!!.findViewById<RecyclerView>(R.id.eventFeedRecyclerView)
    }

    private val searchRecyclerView : RecyclerView by lazy {
        view!!.findViewById<RecyclerView>(R.id.eventFeedSearchRecycleView)
    }

    private val viewModel : EventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(EventViewModel::class.java)
    }
    private var eventsRecyclerViewAdapter : EventsRecyclerViewAdapter? =  null

    private var eventsArray = ArrayList<Event>()
    private var combinedSearchResult = ArrayList<ArrayList<Any>>()

    private val index = Client("3UQQK7YRC5", "2b6893313fb23c6d06eed7c75730d41e").getIndex( "usersAndEvents")

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context !is MainActivity)
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
        return inflater.inflate(R.layout.fragment_event, container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEventRecyclerView()
        setSearchView()

        val customSearchAdapter = EventSearchRecyclerViewAdapter(ArrayList())
        searchRecyclerView.adapter = customSearchAdapter

        setSearchRecyclerView()

        setSearchViewListeners(customSearchAdapter)
        setEventRecyclerViewListener()
        getEventsLiveDataFromEventViewModel()

        eventFeedCreateEventImageView.setOnClickListener {
            startActivity(Intent(activity!!, EventCreateActivity::class.java))
        }

    }

    private fun getEventsLiveDataFromEventViewModel() {

        if (viewModel.currentUserImage.value == null) {
            viewModel.currentUserImage.observe(activity!!, Observer<Bitmap> { userImage ->
                eventFeedFounderImageView.setImageBitmap(userImage)
            })
        } else {
            val userImageToBeRetrieved = viewModel.currentUserImage.value!!
            eventFeedFounderImageView.setImageBitmap(userImageToBeRetrieved)
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

            eventsRecyclerView.scrollToPosition(viewModel.lastFocusedRow.value ?: 0) // Set which row to focused when fragment is reloaded
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

        eventsRecyclerView.layoutManager = layoutManager
        eventsRecyclerView.itemAnimator = DefaultItemAnimator()

        val point = Point()
        activity?.windowManager?.defaultDisplay?.getSize(point)
        val heightOfEventsRecycler = point.x * 9 / 16

        eventsRecyclerViewAdapter =
            EventsRecyclerViewAdapter(eventsArray, heightOfEventsRecycler)
        eventsRecyclerView.adapter = eventsRecyclerViewAdapter
    }

    private fun setEventRecyclerViewListener() {
        eventsRecyclerView.addOnItemTouchListener(
            RecyclerItemClickListener(
                context!!,
                eventsRecyclerView,
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

        FirebaseFirestore.getInstance().collection("users").document("MKbCN5M1gnZ9Yi427rPf2SzyvqM2")
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
        searchRecyclerView.itemAnimator = DefaultItemAnimator()
        searchRecyclerView.layoutManager = LinearLayoutManager(context!!)

        searchRecyclerView.addOnItemTouchListener(
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

    private fun setSearchView(){
        val searchManager = context?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = view?.findViewById<SearchView>(R.id.eventFeedSearchView)
        val searchableInfo = searchManager.getSearchableInfo(activity?.componentName)
        searchView?.setSearchableInfo(searchableInfo)

        searchView?.isIconified = false
        searchView?.setIconifiedByDefault(false)
        searchView?.clearFocus()
    }

    private fun setSearchViewListeners(eventSearchRecyclerViewAdapter: EventSearchRecyclerViewAdapter) {
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