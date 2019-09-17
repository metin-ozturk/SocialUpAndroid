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
import kotlin.properties.Delegates

// DEAL WITH "windowSoftInputMode" in MANIFEST
// FIX IT WHEN IT UPLOADS FROM A PAST EVENT

// LINE 370 - BE TIDY

private const val LOCATION_PERMISSION_REQUEST_CODE = 1

class CreateEventWhereFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener{

    private var viewToBeCreated : View? = null

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }

    private var fusedLocationClient : FusedLocationProviderClient? = null
    private var lastLocation : Location? = null
    private var googleMap : GoogleMap? = null

    private var eventToBePassed : Event? = null

    private var mapView : MapView? = null

    private var latLongOfEventLocation : LatLng? = null

    private var customSearchAdapter : LocationSearchRecyclerViewAdapter? = null

    private var searchedLocations = ArrayList<LocationInfo>()

    private var locationDetailDialogFragment : LocationDetailDialogFragment? = null

    private val locationDetailDialogListener: LocationDetailDialogFragment.LocationDetailDialogFragmentInterface by lazy {
        object: LocationDetailDialogFragment.LocationDetailDialogFragmentInterface {
            override fun onConfirmed(locationToBePassed: LocationInfo) {
                updateEventToBePassedByLocationDetailDialogData(locationToBePassed)
                placeMarkerOnMap(
                    LatLng(
                        locationToBePassed.latitude?.toDouble() ?: 0.0,
                        locationToBePassed.longitude?.toDouble() ?: 0.0
                    )
                )
                locationDetailDialogFragment?.dismiss()
            }
        }
    }

    private var isLocationTickMenuPresent = false

    private var markers = ArrayList<Marker>()
    private var customMarker : Marker? = null

    private val confirmInterfaceFadeInFadeOutAnimation = 750L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_create_event_where, container, false)
        eventToBePassed = createEventViewModel.event.value ?: Event()

        isLocationTickMenuPresent = createEventViewModel.isLocationTickMenuPresent.value ?: false

        setSearchView()
        setSearchViewListeners()
        setMapView(savedInstanceState)
        setSearchRecyclerView()
        setSwipeGestures()

        return viewToBeCreated
    }

    private fun setSwipeGestures() {
        viewToBeCreated?.createEventWhereRootConstraintLayout?.setOnTouchListener( OnGestureTouchListener(activity!!,
            object: OnGestureTouchListener.OnGestureInitiated {
                override fun swipedLeft() {

                    createEventViewModel.updateEventData(eventToBePassed)

                    val createEventWhenFragment = CreateEventWhenFragment()
                    val transaction = activity?.supportFragmentManager?.beginTransaction()
                    transaction?.replace(R.id.eventCreateFrameLayout, createEventWhenFragment)
                    transaction?.commit()
                }
            })
        )
    }

    override fun onMapReady(googleMapRetrieved: GoogleMap?) {
        googleMap = googleMapRetrieved
        googleMap?.setOnMarkerClickListener(this)

        if (isLocationTickMenuPresent) {
            placeMarkerOnMap(LatLng(eventToBePassed?.locationLatitude?.toDouble() ?: return, eventToBePassed?.locationLongitude?.toDouble() ?: return), false)
        } else {

            if (eventToBePassed?.locationLongitude != null && eventToBePassed?.locationLatitude != null) {
                locationDetailDialogFragment = eventToBePassed?.run {
                    LocationDetailDialogFragment.newInstance(LocationInfo(locationName, locationDescription, locationLatitude.toString(),
                        locationLongitude.toString(), locationAddress, null), locationDetailDialogListener)
                }
                locationDetailDialogFragment?.show(fragmentManager ?: return, null)

            }

            // Check if user grants permission to access fine location and if not, ask for it. Then go to current location.
            if (checkFineLocationPermission()) {
                goToCurrentLocationAndUpdateMapViewDetails()
            } else if (!isLocationTickMenuPresent) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }


        googleMap?.setOnMapLongClickListener {
            latLongOfEventLocation = it
            locationDetailDialogFragment = LocationDetailDialogFragment.newInstance(LocationInfo(null, null,
                latLongOfEventLocation?.latitude.toString(), latLongOfEventLocation?.longitude.toString(), null, null),
                locationDetailDialogListener)
            locationDetailDialogFragment?.show(fragmentManager ?: return@setOnMapLongClickListener, null)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            goToCurrentLocationAndUpdateMapViewDetails()
        }
    }

    private fun setMapView(savedInstanceState: Bundle?) {
        mapView = viewToBeCreated?.createEventWhereMapView

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
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

    private fun checkFineLocationPermission():Boolean {
        return ContextCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
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

            locationDetailDialogFragment = it.run {
                LocationDetailDialogFragment.newInstance(LocationInfo(place.name,
                        place.address, place.latLng?.latitude.toString(), place.latLng?.longitude.toString(), place.address, placeID), locationDetailDialogListener)
            }

            locationDetailDialogFragment?.show(fragmentManager ?: return@addOnSuccessListener, null)
        }.addOnFailureListener {
            if (it is ApiException)
                Log.d("CreateEventWhereFrag","PLACE NOT FOUND" , it)
        }
    }


    private fun updateEventToBePassedByLocationDetailDialogData(locationToBePassed: LocationInfo) {
        eventToBePassed?.apply {
            locationName = locationToBePassed.name
            locationDescription = locationToBePassed.description
            locationLatitude = locationToBePassed.latitude
            locationLongitude = locationToBePassed.longitude
            locationAddress = locationToBePassed.address
        }

        createEventViewModel.updateEventData(eventToBePassed)

    }

    private fun clearEventToBeCreated() {
        eventToBePassed?.apply {
            locationName = null
            locationDescription = null
            locationLatitude = null
            locationLongitude = null
            locationAddress = null
        }
    }


    private fun goToCurrentLocationAndUpdateMapViewDetails() {
        if (!checkFineLocationPermission()) return

        googleMap?.apply {
            uiSettings?.isZoomControlsEnabled = true
            isMyLocationEnabled = true
        }


        fusedLocationClient?.lastLocation?.addOnSuccessListener { retrievedLocation ->
            if (retrievedLocation == null) {
                // If there is no know last location, request current location
                val locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(10000).setFastestInterval(1000)

                val locationCallback = object: LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        super.onLocationResult(locationResult)

                        if (locationResult == null) return

                        lastLocation = locationResult.lastLocation.also { location ->
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12f))
                        }

                        fusedLocationClient?.removeLocationUpdates(this)
                    }
                }

                fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, null)
            } else {
                lastLocation = retrievedLocation.also { location ->
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12f))
                }
            }


        }

    }


    private fun placeMarkerOnMap(location: LatLng, animateCamera: Boolean = true) {
        if (animateCamera)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12f))
        else
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12f))

        val markerOptions = MarkerOptions().position(location)

        when(markers.size) {
            0 -> {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                markerOptions.alpha(0.8f)
            }

            else -> {
                markers.forEach { it.remove() }
                markers = ArrayList()

                placeMarkerOnMap(location)
                return
            }
        }

        customMarker = googleMap?.addMarker(markerOptions) ?: return
        markers.add(customMarker ?: return)

        setConfirmInterface()

    }

    private fun setConfirmInterface() {
        isLocationTickMenuPresent = true

        val confirmLocation = TextView(context!!)
        confirmLocation.text = "Confirm?"
        confirmLocation.textSize = 20f
        confirmLocation.typeface = Typeface.DEFAULT_BOLD
        confirmLocation.measure(0, 0)

        val confirmLocationTick = ImageView(context!!)
        confirmLocationTick.setImageResource(R.drawable.tick)
        confirmLocationTick.scaleType = ImageView.ScaleType.CENTER_CROP

        val confirmLocationCancel = ImageView(context!!)
        confirmLocationCancel.setImageResource(R.drawable.cancel)
        confirmLocationCancel.scaleType = ImageView.ScaleType.CENTER_CROP

        arrayOf(confirmLocation, confirmLocationTick, confirmLocationCancel).forEach {
            it.id = View.generateViewId()
            it.alpha = 0f
            viewToBeCreated?.createEventWhereRootConstraintLayout?.addView(it)
            it.animate().alpha(1f).duration = confirmInterfaceFadeInFadeOutAnimation // Fade-In animation for Confirm Interface
        }

        val constraintSet = ConstraintSet()

        constraintSet.connect(confirmLocation.id, ConstraintSet.START, viewToBeCreated?.createEventWhereMapView?.id ?: return, ConstraintSet.START, (viewToBeCreated?.createEventWhereMapView?.measuredWidth ?: 0 ) / 2 - confirmLocation.measuredWidth / 2 )
        constraintSet.connect(confirmLocation.id, ConstraintSet.TOP, viewToBeCreated?.createEventWhereMapView?.id ?: return, ConstraintSet.TOP, (viewToBeCreated?.createEventWhereMapView?.measuredHeight ?: 0 ) / 2)
        constraintSet.constrainHeight(confirmLocation.id, ConstraintSet.WRAP_CONTENT)
        constraintSet.constrainWidth(confirmLocation.id, ConstraintSet.WRAP_CONTENT)

        constraintSet.connect(confirmLocationTick.id, ConstraintSet.START, confirmLocation.id, ConstraintSet.START, 0)
        constraintSet.connect(confirmLocationTick.id, ConstraintSet.TOP, confirmLocation.id, ConstraintSet.BOTTOM, 0 )
        constraintSet.constrainHeight(confirmLocationTick.id, 100)
        constraintSet.constrainWidth(confirmLocationTick.id, 100)

        constraintSet.connect(confirmLocationCancel.id, ConstraintSet.END, confirmLocation.id, ConstraintSet.END, 0)
        constraintSet.connect(confirmLocationCancel.id, ConstraintSet.TOP, confirmLocation.id, ConstraintSet.BOTTOM, 0 )
        constraintSet.constrainHeight(confirmLocationCancel.id, 100)
        constraintSet.constrainWidth(confirmLocationCancel.id, 100)

        constraintSet.applyTo(viewToBeCreated?.createEventWhereRootConstraintLayout)

        setConfirmInterfaceListeners(confirmLocationTick, confirmLocationCancel, confirmLocation)

    }

    private fun setConfirmInterfaceListeners(confirmLocationTick: ImageView, confirmLocationCancel: ImageView, confirmLocation: TextView) {

        confirmLocationTick.setOnClickListener {
            isLocationTickMenuPresent = false
            val createEventSummaryFragment = CreateEventSummaryFragment()
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.eventCreateFrameLayout, createEventSummaryFragment)
            transaction?.commit()
        }

        confirmLocationCancel.setOnClickListener {
            isLocationTickMenuPresent = false
            clearEventToBeCreated()

            arrayOf(confirmLocation, confirmLocationCancel, confirmLocationTick, customMarker).forEach {viewToBeRemoved ->

                val fadeOutAnimation = ObjectAnimator.ofFloat(viewToBeRemoved, "alpha", 1f, 0f).setDuration(confirmInterfaceFadeInFadeOutAnimation)
                fadeOutAnimation.addListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)

                        if (viewToBeRemoved != customMarker ) {
                            ((viewToBeRemoved as View).parent as ViewGroup).removeView(viewToBeRemoved)
                        } else {
                            (viewToBeRemoved as Marker).remove()
                        }
                    }
                })
                fadeOutAnimation.start()
            }
        }
    }


    override fun onMarkerClick(marker: Marker?) : Boolean {

        marker?.title = eventToBePassed?.locationName
        marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        marker?.alpha = 0.8f
        return false
    }


    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()

        if (locationDetailDialogFragment?.isAdded == true) {
            createEventViewModel.updateEventData(locationDetailDialogFragment?.returnEventWithUpdatedLocation(eventToBePassed ?: return) ?: return)
            locationDetailDialogFragment?.dismiss()
        }

        createEventViewModel.updateIsLocationTickMenuPresent(isLocationTickMenuPresent)
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }


    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
}