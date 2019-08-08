package com.jora.socialup.fragments

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.fragment_dialog_location_detail.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException


class LocationInfo(internal val name : String? = null,
                   internal val description : String? = null,
                   internal val latitude : String? = null,
                   internal val longitude: String? = null,
                   internal val address: String? = null) {
    override fun toString(): String {
        return "$name $description $latitude $longitude $address"
    }
}


class LocationDetailDialogFragment(private val latLong: LatLng, private val listener : LocationDetailDialogFragmentInterface) : DialogFragment() {

    interface LocationDetailDialogFragmentInterface {
        fun onConfirmed(locationToBePassed: LocationInfo)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(com.jora.socialup.R.layout.fragment_dialog_location_detail, container, false)

        view.apply {
            fragmentDialogLocationDetailLatitudeInput.text = latLong.latitude.toString().substring(0,8)
            fragmentDialogLocationDetailLongitudeInput.text = latLong.longitude.toString().substring(0,8)

            var address = ""
            GlobalScope.launch {
                address = getAddress(latLong)
            }

            fragmentDialogLocationDetailConfirmButton?.setOnClickListener {
                if (address.isNotEmpty()) {
                    val locationToBePassed = LocationInfo(
                        fragmentDialogLocationDetailNameInput.text.toString(),
                        fragmentDialogLocationDetailDescriptionInput.text.toString(),
                        latLong.latitude.toString(),
                        latLong.longitude.toString(),
                        address
                    )

                    listener.onConfirmed(locationToBePassed)
                }
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        // SET HEIGHT AND WIDTH OF DIALOG HERE

        val attributes = dialog?.window?.attributes
        attributes?.width = 800
        attributes?.height = 1000
        dialog?.window?.attributes = attributes
    }

    fun getAddress(latLng: LatLng) : String {
        val geoCoder = Geocoder(activity!!)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            addresses = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                address = addresses[0]
                addressText = address.getAddressLine(0)
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return addressText
    }
}

