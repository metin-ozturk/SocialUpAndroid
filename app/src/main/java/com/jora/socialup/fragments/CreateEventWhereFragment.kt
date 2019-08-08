package com.jora.socialup.fragments


import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.jora.socialup.models.Event
import com.jora.socialup.viewModels.CreateEventViewModel
import kotlinx.android.synthetic.main.fragment_create_event_where.view.*
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest




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

    private val locationDetailDialogFragment : LocationDetailDialogFragment by lazy {
        LocationDetailDialogFragment(latLongOfEventLocation ?: LatLng(0.0, 0.0),
            object : LocationDetailDialogFragment.LocationDetailDialogFragmentInterface {
                override fun onConfirmed(locationToBePassed: LocationInfo) {
                    eventToBePassed?.locationName = locationToBePassed.name
                    eventToBePassed?.locationDescription = locationToBePassed.description
                    eventToBePassed?.locationLatitude = locationToBePassed.latitude
                    eventToBePassed?.locationLongitude = locationToBePassed.longitude
                    eventToBePassed?.locationAddress = locationToBePassed.address

                    placeMarkerOnMap(latLongOfEventLocation ?: LatLng(0.0, 0.0))
                    locationDetailDialogFragment.dismiss()
                }
            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(com.jora.socialup.R.layout.fragment_create_event_where, container, false)
        eventToBePassed = createEventViewModel.event.value

        Places.initialize(activity!!.applicationContext, com.jora.socialup.R.string.google_android_map_api_key.toString())
        val placesClient = Places.createClient(activity!!)

        val token = AutocompleteSessionToken.newInstance()
        val bounds = RectangularBounds.newInstance(
            LatLng(41.0082, 28.9784),
            LatLng(41.20, 29.2)
        )

        val request = FindAutocompletePredictionsRequest.builder()
            // Call either setLocationBias() OR setLocationRestriction().
//            .setLocationBias(bounds)
            .setLocationRestriction(bounds)
            .setCountry("tr")
            .setTypeFilter(TypeFilter.ADDRESS)
            .setSessionToken(token)
            .setQuery("Kadikoy")
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            for (prediction in response.autocompletePredictions) {
                Log.i("OSMAN", prediction.placeId)
                Log.i("OSMAN", prediction.getPrimaryText(null).toString())
            }
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e("OSMAN", "Place not found: " + exception.statusCode)
            }
        }


        mapView = viewToBeCreated?.createEventWhereMapView

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        return viewToBeCreated
    }

    override fun onMapReady(googleMapRetrieved: GoogleMap?) {
        googleMap = googleMapRetrieved
        googleMap?.setOnMarkerClickListener(this)

        googleMap?.setOnMapLongClickListener {
            latLongOfEventLocation = it
            locationDetailDialogFragment.show(activity?.supportFragmentManager, null)
        }


        // Check if user grants permission to access fine location and if not, ask for it. Then go to current location.
        if (checkFineLocationPermission()) {
            goToCurrentLocationAndUpdateMapViewDetails()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            goToCurrentLocationAndUpdateMapViewDetails()
        }
    }

    private fun checkFineLocationPermission():Boolean {
        return ContextCompat.checkSelfPermission(activity!!, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }


    private fun goToCurrentLocationAndUpdateMapViewDetails() {
        if (!checkFineLocationPermission()) return

        googleMap?.apply {
            uiSettings?.isZoomControlsEnabled = true
            isMyLocationEnabled = true
        }

        fusedLocationClient?.lastLocation?.addOnSuccessListener {
            lastLocation = it
            val currentLatLng = LatLng(it.latitude, it.longitude)

            placeMarkerOnMap(currentLatLng)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

        }
    }


    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        markerOptions.alpha(0.8f)

        googleMap?.addMarker(markerOptions)
    }

    override fun onMarkerClick(marker: Marker?) : Boolean {
        marker?.title = if (eventToBePassed?.locationAddress == null) "Current Location" else eventToBePassed?.locationAddress
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

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
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