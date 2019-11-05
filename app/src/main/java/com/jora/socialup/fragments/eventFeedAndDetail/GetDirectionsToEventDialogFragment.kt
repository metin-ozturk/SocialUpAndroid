package com.jora.socialup.fragments.eventFeedAndDetail

import android.app.Dialog
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.jora.socialup.R
import com.jora.socialup.fragments.createEvent.EventMapFragment
import com.jora.socialup.models.Event
import com.jora.socialup.models.LocationSelectionStatus
import com.jora.socialup.viewModels.EventViewModel
import kotlinx.android.synthetic.main.fragment_dialog_get_directions_to_event.view.*

class GetDirectionsToEventDialogFragment : DialogFragment() {

    interface GetDirectionsToEventDialogFragmentInterface {
        fun onDialogFragmentDestroyed()
    }

    private var viewToBeCreated : View? = null
    private var eventMapFragment : EventMapFragment? = null

    private var listener : GetDirectionsToEventDialogFragmentInterface? = null

    private val viewModel : EventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(EventViewModel::class.java)
    }

    companion object {
        fun newInstance(listener: GetDirectionsToEventDialogFragmentInterface) : GetDirectionsToEventDialogFragment {
            val dialogFragment = GetDirectionsToEventDialogFragment()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_get_directions_to_event, container, false)

        setEventMapFragment()

        return viewToBeCreated
    }

    override fun onResume() {
        super.onResume()

        val params = dialog?.window?.attributes
        params?.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400f, resources.displayMetrics).toInt()
        params?.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400f, resources.displayMetrics).toInt()
        dialog?.window?.attributes = params as android.view.WindowManager.LayoutParams

    }



    private fun setEventMapFragment(){
        eventMapFragment = EventMapFragment.newInstance(object: EventMapFragment.EventMapFragmentInterface {
            override fun onFragmentDestroyed() {
                eventMapFragment = null
            }
        }, viewModel.event.value?.copy(),
            LocationSelectionStatus.Confirmed)


        val transaction = this.childFragmentManager.beginTransaction()
        transaction.add(R.id.fragmentDialogGetDirectionsToEventFrameLayout, eventMapFragment ?: return)
        transaction.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.onDialogFragmentDestroyed()
    }
}