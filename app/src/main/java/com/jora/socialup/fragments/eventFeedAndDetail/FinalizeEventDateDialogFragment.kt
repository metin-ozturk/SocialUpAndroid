package com.jora.socialup.fragments.eventFeedAndDetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.jora.socialup.R
import com.jora.socialup.adapters.FinalizeEventDateAdapter
import com.jora.socialup.helpers.HelperFunctions
import kotlinx.android.synthetic.main.fragment_dialog_finalize_event_date.view.*


class FinalizeEventDateDialogFragment : DialogFragment() {
    interface FinalizeEventDateDialogFragmentInterface {
        fun onDateSelected(result: String)
        fun onDialogFragmentDestroyed()
    }

    private var listener : FinalizeEventDateDialogFragmentInterface? = null
    private var viewToBeCreated : View? = null
    private var finalizeEventDateAdapter : FinalizeEventDateAdapter? = null
    private var eventDates : ArrayList<String>? = null

    private val finalizedDate : String?
        get() = finalizeEventDateAdapter?.finalizedDate

    companion object {
        fun newInstance(listener: FinalizeEventDateDialogFragmentInterface) : FinalizeEventDateDialogFragment {
            val dialogFragment = FinalizeEventDateDialogFragment()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_finalize_event_date, container, false)

        finalizeEventDateAdapter = FinalizeEventDateAdapter()
        finalizeEventDateAdapter?.loadEventDates(eventDates)

        viewToBeCreated?.finalizeEventDateConstraintLayout?.apply {
            adapter = finalizeEventDateAdapter
            layoutManager = LinearLayoutManager(this@FinalizeEventDateDialogFragment.context)
        }

        setFinalizeButton()

        return viewToBeCreated
    }

    private fun setFinalizeButton() {
        viewToBeCreated?.finalizeEventDateFinalizeButton?.setOnClickListener {
            if (finalizedDate == null) {
                Toast.makeText(context!!, "Please select a date to finalize", Toast.LENGTH_SHORT).show()
            } else {
                listener?.onDateSelected(finalizedDate ?: return@setOnClickListener)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.onDialogFragmentDestroyed()
    }

    fun loadEventDates(eventDates: ArrayList<String>) {
        this.eventDates = eventDates
    }


}