package com.jora.socialup.fragments.createEvent


import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.SearchManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.jora.socialup.models.Event
import com.jora.socialup.viewModels.CreateEventViewModel
import kotlinx.android.synthetic.main.fragment_create_event_where.view.*
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.jora.socialup.R
import com.jora.socialup.adapters.LocationSearchRecyclerViewAdapter
import com.jora.socialup.helpers.OnGestureTouchListener
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.models.LocationSelectionStatus
import kotlin.properties.Delegates

// DEAL WITH "windowSoftInputMode" in MANIFEST
// FIX IT WHEN IT UPLOADS FROM A PAST EVENT

// LINE 370 - BE TIDY


class CreateEventWhereFragment : Fragment(){

    private var viewToBeCreated : View? = null

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }

    private var customSearchAdapter : LocationSearchRecyclerViewAdapter? = null
    private var searchedLocations = ArrayList<LocationInfo>()

    private var lastLocation : Location? = null

    private var eventMapFragment : EventMapFragment? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_create_event_where, container, false)

        setSearchView()
        setSearchViewListeners()
        setSearchRecyclerView()
        setSwipeGestures()
        setEventMapFragment()

        return viewToBeCreated
    }

    private fun setEventMapFragment(){
        eventMapFragment = EventMapFragment.newInstance(object: EventMapFragment.EventMapFragmentInterface {

            override fun lastLocationRetrieved(lastLocation: Location?) {
                this@CreateEventWhereFragment.lastLocation = lastLocation
            }

            override fun updateEventData(updateTo: Event) {
                createEventViewModel.updateEventData(updateTo)
            }

            override fun updateLocationSelectionStatus(updateTo: LocationSelectionStatus) {
                createEventViewModel.updateLocationSelectionStatus(updateTo)
            }

            override fun onFragmentDestroyed() {
                eventMapFragment = null
            }
        }, createEventViewModel.event.value?.copy(),
            createEventViewModel.locationSelectionStatus.value)

        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.add(R.id.createEventWhereFrameLayout, eventMapFragment ?: return)
        transaction?.commit()
    }
 

    private fun setSwipeGestures() {
        viewToBeCreated?.createEventWhereRootConstraintLayout?.setOnTouchListener( OnGestureTouchListener(activity!!,
            object: OnGestureTouchListener.OnGestureInitiated {
                override fun swipedLeft() {

                    createEventViewModel.updateEventData(createEventViewModel.event.value?.copy())

                    val createEventWhenFragment = CreateEventWhenFragment()
                    val transaction = activity?.supportFragmentManager?.beginTransaction()
                    transaction?.replace(R.id.eventCreateFrameLayout, createEventWhenFragment)
                    transaction?.commit()
                }
            })
        )
    }


    private fun setSearchView() {
        viewToBeCreated?.createEventWhereSearchView?.apply {
            val searchManager = context?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
            val searchableInfo = searchManager.getSearchableInfo(activity?.componentName)
            queryHint = "Search Locations..."
            setSearchableInfo(searchableInfo)

            isIconified = false
            isIconifiedByDefault = false
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            clearFocus()
        }
    }

    private fun setSearchViewListeners() {
        viewToBeCreated?.apply {

            createEventWhereSearchView.setOnClickListener {
                createEventWhereSearchView.requestFocus()
            }

            val searchCloseButtonID =
                createEventWhereSearchView.context.resources.getIdentifier("android:id/search_close_btn", null, null)
            val searchCloseButton = createEventWhereSearchView.findViewById<ImageView>(searchCloseButtonID)

            searchCloseButton.setOnClickListener {
                createEventWhereSearchView.setQuery("", true)
                createEventWhereSearchView.clearFocus()
            }

            createEventWhereSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (!searchedLocations.isNullOrEmpty()) {
                        val locationID = searchedLocations.first().locationID ?: return true
                        getLocationInformationWithPlaceID(locationID)
                    }

                    createEventWhereSearchView.clearFocus()
                    createEventWhereSearchView.setQuery("", true)

                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrEmpty()) {
                        searchedLocations = ArrayList()
                        customSearchAdapter?.updateSearchedLocations(searchedLocations)
                        createEventWhereSearchRecyclerView?.layoutParams?.height =  0
                        createEventWhereSearchView.clearFocus()
                        return false
                    }


                    getAutoCompletePredictions(newText)

                    // Put android:windowSoftInputMode="adjustNothing" to activity's field in manifest, if you like to disable auto resize when keyboard appears.

                    val searchResultsHeight = resources.getDimension(R.dimen.search_location_results_height)
                    createEventWhereSearchRecyclerView?.layoutParams?.height = searchResultsHeight.toInt()

                    return false
                }
            })
        }
    }

    private fun setSearchRecyclerView() {
        viewToBeCreated?.createEventWhereSearchRecyclerView?.apply {
            customSearchAdapter = LocationSearchRecyclerViewAdapter()
            adapter = customSearchAdapter
            itemAnimator = DefaultItemAnimator()
            layoutManager = LinearLayoutManager(context!!)


            addOnItemTouchListener(
                RecyclerItemClickListener(
                    context!!,
                    createEventWhereSearchRecyclerView,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int) {
                            val locationID = searchedLocations[position].locationID ?: return
                            getLocationInformationWithPlaceID(locationID)
                        }
                    })
            )
        }

    }


    private fun getAutoCompletePredictions(query: String) {
        Places.initialize(activity!!.applicationContext, "AIzaSyBm3I6KSFsi_usniA7PC9JALxiIBtCw1JA")
        val placesClient = Places.createClient(activity!!)

        val latitude = lastLocation?.latitude ?: return
        val longitude = lastLocation?.longitude ?: return

        val token = AutocompleteSessionToken.newInstance()
        val bounds = RectangularBounds.newInstance(
            LatLng(latitude - 0.1, longitude - 0.1),
            LatLng(latitude + 0.1, longitude + 0.1)
        )

        val request = FindAutocompletePredictionsRequest.builder()
            // Call either setLocationBias() OR setLocationRestriction().
            .setLocationBias(bounds)
//            .setLocationRestriction(bounds)
//            .setCountry("tr")
//            .setTypeFilter(TypeFilter.GEOCODE)
            .setSessionToken(token)
            .setQuery(query)
            .build()


//        val autoCompletePredictions =  placesClient.findAutocompletePredictions(request)
//
//        GlobalScope.launch {
//            autoCompletePredictions.await()
//            Log.d("OSMAN", "2" + autoCompletePredictions.result.toString())
//        }

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->

            searchedLocations = response.autocompletePredictions.map {
                LocationInfo(
                    it.getPrimaryText(null).toString(), it.getSecondaryText(null).toString(),
                    null, null, null, it.placeId
                )
            } as ArrayList<LocationInfo>

            customSearchAdapter?.updateSearchedLocations(searchedLocations)

        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e("CreateEventWhereFrag", "Place not found: " + exception.statusCode)
            }
        }
    }

    private fun getLocationInformationWithPlaceID(placeID: String) {
        Places.initialize(activity!!,"AIzaSyBm3I6KSFsi_usniA7PC9JALxiIBtCw1JA")
        val placesClient = Places.createClient(activity!!)

        val placeFields = listOf(Place.Field.ADDRESS, Place.Field.ADDRESS_COMPONENTS, Place.Field.LAT_LNG, Place.Field.NAME)

        val request = FetchPlaceRequest.newInstance(placeID, placeFields )

        placesClient.fetchPlace(request).addOnSuccessListener {
            customSearchAdapter?.updateSearchedLocations(arrayListOf())
            searchedLocations = arrayListOf()
            viewToBeCreated?.createEventWhereSearchView?.clearFocus()
            eventMapFragment?.showLocationDetailDialogFragment(it, placeID)
        }.addOnFailureListener {
            if (it is ApiException)
                Log.d("CreateEventWhereFrag","PLACE NOT FOUND" , it)
        }
    }



}