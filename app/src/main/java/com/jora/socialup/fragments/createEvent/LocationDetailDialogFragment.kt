package com.jora.socialup.fragments.createEvent

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.jora.socialup.models.Event
import kotlinx.android.synthetic.main.fragment_dialog_location_detail.*
import kotlinx.android.synthetic.main.fragment_dialog_location_detail.view.*
import kotlinx.coroutines.*
import java.io.IOException


class LocationInfo(internal val name : String? = null,
                   internal val description : String? = null,
                   internal val latitude : String? = null,
                   internal val longitude: String? = null,
                   internal val address: String? = null,
                   internal val locationID : String? = null) {
    override fun toString(): String {
        return "$name $description $latitude $longitude $address $locationID"
    }

}


class LocationDetailDialogFragment : DialogFragment() {

    interface LocationDetailDialogFragmentInterface {
        fun onConfirmed(locationToBePassed: LocationInfo)
        fun onDialogFragmentDestroyed()
    }

    private var locationInfo : LocationInfo? = null
    private var listener : LocationDetailDialogFragmentInterface? = null

    companion object {
        fun newInstance(locationInfo: LocationInfo, listener: LocationDetailDialogFragmentInterface) : LocationDetailDialogFragment {
            val dialogFragment = LocationDetailDialogFragment()
            dialogFragment.listener = listener
            dialogFragment.locationInfo = locationInfo
            return dialogFragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(com.jora.socialup.R.layout.fragment_dialog_location_detail, container, false)

        view.apply {
            fragmentDialogLocationDetailLatitudeInput.text = if (locationInfo?.latitude?.count() ?: 0 < 8) locationInfo?.latitude.toString() else locationInfo?.latitude.toString().substring(0,8)
            fragmentDialogLocationDetailLongitudeInput.text = if (locationInfo?.longitude?.count() ?: 0 < 8) locationInfo?.longitude.toString() else locationInfo?.longitude.toString().substring(0,8)
            fragmentDialogLocationDetailDescriptionInput.text.insert(0, locationInfo?.description ?: "")
            fragmentDialogLocationDetailNameInput.text.insert(0, locationInfo?.name ?: "")

            var address = ""

            if (locationInfo?.address.isNullOrEmpty()) {
                val bgScope = CoroutineScope(Dispatchers.IO)

                bgScope.launch {
                    address = getAddress(locationInfo?.latitude?.toDouble() ?: 0.0,
                        locationInfo?.longitude?.toDouble() ?: 0.0)
                    bgScope.cancel()
                }
            } else address = locationInfo?.address ?: "ERROR"


            fragmentDialogLocationDetailConfirmButton?.setOnClickListener {
                if (address.isNotEmpty()) {
                    val locationToBePassed = LocationInfo(
                        fragmentDialogLocationDetailNameInput.text.toString(),
                        fragmentDialogLocationDetailDescriptionInput.text.toString(),
                        locationInfo?.latitude,
                        locationInfo?.longitude,
                        address
                    )

                    listener?.onConfirmed(locationToBePassed)
                }
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        // SET HEIGHT AND WIDTH OF DIALOG HERE

        val attributes = dialog?.window?.attributes
        attributes?.width = 800
        attributes?.height = 1000
        dialog?.window?.attributes = attributes
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.onDialogFragmentDestroyed()
    }

    fun returnEventWithUpdatedLocation(event: Event) : Event {
        return event.apply {
            locationName = fragmentDialogLocationDetailNameInput?.text.toString()
            locationDescription = fragmentDialogLocationDetailDescriptionInput?.text.toString()
            locationLatitude = fragmentDialogLocationDetailLatitudeInput?.text.toString()
            locationLongitude = fragmentDialogLocationDetailLongitudeInput?.text.toString()
            locationAddress = ""
        }
     }

    private fun getAddress(latitude: Double, longitude: Double) : String {
        val geoCoder = Geocoder(activity!!)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            addresses = geoCoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                address = addresses[0]
                addressText = address.getAddressLine(0)
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", "ERROR ${e.localizedMessage}")
        }

        return addressText
    }

}

