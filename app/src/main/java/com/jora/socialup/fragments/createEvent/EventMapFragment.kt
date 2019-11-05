package com.jora.socialup.fragments.createEvent

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.jora.socialup.R
import com.jora.socialup.models.Event
import com.jora.socialup.models.LocationSelectionStatus
import com.jora.socialup.viewModels.CreateEventViewModel
import kotlinx.android.synthetic.main.fragment_event_map.view.*


private const val LOCATION_PERMISSION_REQUEST_CODE = 1


class EventMapFragment : Fragment(), OnMapReadyCallback {

    interface EventMapFragmentInterface {
        fun updateLocationSelectionStatus(updateTo: LocationSelectionStatus) {}
        fun updateEventData(updateTo: Event) {}
        fun lastLocationRetrieved(lastLocation: Location?) {}
        fun onFragmentDestroyed()
    }


    private var viewToBeCreated : View? = null

    private var googleMap : GoogleMap? = null
    private var locationSelectionStatus = LocationSelectionStatus.NotSelected

    private var mapView : MapView? = null
    private var fusedLocationClient : FusedLocationProviderClient? = null

    private var eventToBePassed : Event? = null
    private var latLongOfEventLocation : LatLng? = null
    private var lastLocation : Location? = null

    var listener : EventMapFragmentInterface? = null

    private var markers = ArrayList<Marker>()
    private var customMarker : Marker? = null

    private val confirmInterfaceFadeInFadeOutAnimation = 750L

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

            override fun onDialogFragmentDestroyed() {
                locationDetailDialogFragment = null
            }
        }
    }

    companion object {
        fun newInstance(listener: EventMapFragmentInterface,
                        event: Event?,
                        locationSelectionStatus: LocationSelectionStatus?) : EventMapFragment {
            val fragment = EventMapFragment()
            fragment.listener = listener
            fragment.eventToBePassed = event
            fragment.locationSelectionStatus = locationSelectionStatus ?: LocationSelectionStatus.NotSelected
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_event_map, container, false)

        setMapView(savedInstanceState)

        return viewToBeCreated
    }


    override fun onMapReady(googleMapRetrieved: GoogleMap?) {
        googleMap = googleMapRetrieved?.apply {
            uiSettings?.isZoomControlsEnabled = true
            isMyLocationEnabled = true
            uiSettings?.setAllGesturesEnabled(true)
        }

        setMarkerListener()

        if (locationSelectionStatus == LocationSelectionStatus.Confirmed) {
            placeMarkerOnMap(LatLng(eventToBePassed?.locationLatitude?.toDouble() ?: return, eventToBePassed?.locationLongitude?.toDouble() ?: return), false)
            goToCurrentLocationAndUpdateMapViewDetails()
            return
        }

        if (locationSelectionStatus == LocationSelectionStatus.AboutToBeConfirmed) {
            placeMarkerOnMap(LatLng(eventToBePassed?.locationLatitude?.toDouble() ?: return, eventToBePassed?.locationLongitude?.toDouble() ?: return), false)
        } else {

            if (locationSelectionStatus == LocationSelectionStatus.SettingNameAndDescription) {
                locationDetailDialogFragment = eventToBePassed?.run {
                    LocationDetailDialogFragment.newInstance(LocationInfo(locationName, locationDescription, locationLatitude.toString(),
                        locationLongitude.toString(), locationAddress, null), locationDetailDialogListener)
                }
                locationDetailDialogFragment?.show(fragmentManager ?: return, null)

            }

            // Check if user grants permission to access fine location and if not, ask for it. Then go to current location.
            if (checkFineLocationPermission()) {
                goToCurrentLocationAndUpdateMapViewDetails()
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }

        setGoogleMapListener()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            goToCurrentLocationAndUpdateMapViewDetails()
        }
    }

    private fun setMapView(savedInstanceState: Bundle?) {
        mapView = viewToBeCreated?.eventMapFragmentMapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun setMarkerListener() {
        googleMap?.setOnMarkerClickListener {
            it.title = eventToBePassed?.locationName
            it.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            it.alpha = 0.8f
            false
        }


        googleMap?.setOnInfoWindowCloseListener {
            it.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            goToCurrentLocationAndUpdateMapViewDetails()
        }

    }

    private fun setGoogleMapListener() {
        googleMap?.setOnMapLongClickListener {
            latLongOfEventLocation = it
            locationDetailDialogFragment = LocationDetailDialogFragment.newInstance(LocationInfo(null, null,
                latLongOfEventLocation?.latitude.toString(), latLongOfEventLocation?.longitude.toString(), null, null),
                locationDetailDialogListener)
            locationDetailDialogFragment?.show(fragmentManager ?: return@setOnMapLongClickListener, null)
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
                if (locationSelectionStatus == LocationSelectionStatus.Confirmed) markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                else markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
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


        // If user views map from event detail then don't show confirm interface
        if (locationSelectionStatus == LocationSelectionStatus.Confirmed) return


        setConfirmInterface()

    }

    private fun checkFineLocationPermission():Boolean {
        return ContextCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun goToCurrentLocationAndUpdateMapViewDetails() {
        if (!checkFineLocationPermission()) return

        fusedLocationClient?.lastLocation?.addOnSuccessListener { retrievedLocation ->

            if (retrievedLocation == null) {

                // If there is no know last location, request current location
                val locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(10000).setFastestInterval(1000)

                val locationCallback = object: LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        super.onLocationResult(locationResult)

                        if (locationResult == null) {
                            fusedLocationClient?.removeLocationUpdates(this)
                            return
                        }

                        lastLocation = locationResult.lastLocation.also { location ->
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12f))
                            listener?.lastLocationRetrieved(location)
                        }

                        fusedLocationClient?.removeLocationUpdates(this)
                    }
                }

                fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, null)
            } else {
                lastLocation = retrievedLocation.also { location ->
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12f))
                    listener?.lastLocationRetrieved(location)
                }
            }


        }

    }

    private fun setConfirmInterface() {

        googleMap?.uiSettings?.setAllGesturesEnabled(false)

        locationSelectionStatus = LocationSelectionStatus.AboutToBeConfirmed

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
            viewToBeCreated?.eventMapFragmentRootConstraintLayout?.addView(it)
            it.animate().alpha(1f).duration = confirmInterfaceFadeInFadeOutAnimation // Fade-In animation for Confirm Interface
        }

        val constraintSet = ConstraintSet()

        constraintSet.connect(confirmLocation.id, ConstraintSet.START, viewToBeCreated?.eventMapFragmentMapView?.id ?: return, ConstraintSet.START, (viewToBeCreated?.eventMapFragmentMapView?.measuredWidth ?: 0 ) / 2 - confirmLocation.measuredWidth / 2 )
        constraintSet.connect(confirmLocation.id, ConstraintSet.TOP, viewToBeCreated?.eventMapFragmentMapView?.id ?: return, ConstraintSet.TOP, (viewToBeCreated?.eventMapFragmentMapView?.measuredHeight ?: 0 ) / 2)
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

        constraintSet.applyTo(viewToBeCreated?.eventMapFragmentRootConstraintLayout)

        setConfirmInterfaceListeners(confirmLocationTick, confirmLocationCancel, confirmLocation)

    }

    private fun setConfirmInterfaceListeners(confirmLocationTick: ImageView, confirmLocationCancel: ImageView, confirmLocation: TextView) {

        confirmLocationTick.setOnClickListener {
            val createEventSummaryFragment = CreateEventSummaryFragment()
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.eventCreateFrameLayout, createEventSummaryFragment)
            transaction?.commit()

            googleMap?.uiSettings?.setAllGesturesEnabled(true)

        }

        confirmLocationCancel.setOnClickListener {
            locationSelectionStatus = LocationSelectionStatus.NotSelected
            clearEventToBeCreated()

            // Prevent double clicks
            confirmLocationCancel.isClickable = false
            confirmLocationTick.isClickable = false

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
            googleMap?.uiSettings?.setAllGesturesEnabled(true)
        }
    }

    fun showLocationDetailDialogFragment(fetchPlaceResponse: FetchPlaceResponse, placeID : String) {
        locationDetailDialogFragment = fetchPlaceResponse.run {
            LocationDetailDialogFragment.newInstance(LocationInfo(place.name,
                place.address, place.latLng?.latitude.toString(), place.latLng?.longitude.toString(), place.address, placeID), locationDetailDialogListener)
        }

        locationDetailDialogFragment?.show(fragmentManager ?: return, null)
    }

    private fun updateEventToBePassedByLocationDetailDialogData(locationToBePassed: LocationInfo) {
        eventToBePassed?.apply {
            locationName = locationToBePassed.name
            locationDescription = locationToBePassed.description
            locationLatitude = locationToBePassed.latitude
            locationLongitude = locationToBePassed.longitude
            locationAddress = locationToBePassed.address
        }

        listener?.updateEventData(eventToBePassed ?: return)

    }

    private fun clearEventToBeCreated() {
        eventToBePassed?.apply {
            locationName = null
            locationDescription = null
            locationLatitude = null
            locationLongitude = null
            locationAddress = null
        }

        listener?.updateEventData(eventToBePassed ?: return)
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
            locationSelectionStatus = LocationSelectionStatus.SettingNameAndDescription
            listener?.updateEventData(locationDetailDialogFragment?.returnEventWithUpdatedLocation(eventToBePassed ?: return) ?: return)
            locationDetailDialogFragment?.dismiss()
        }

        if ( locationSelectionStatus == LocationSelectionStatus.NotSelected
            && eventToBePassed?.locationLatitude != null) {

            clearEventToBeCreated()
        }

        listener?.updateLocationSelectionStatus(locationSelectionStatus)

    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }


    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        listener?.onFragmentDestroyed()
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