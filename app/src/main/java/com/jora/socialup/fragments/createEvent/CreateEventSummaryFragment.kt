package com.jora.socialup.fragments.createEvent

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.firestore.FieldValue
import com.jora.socialup.R
import com.jora.socialup.models.Event
import com.jora.socialup.viewModels.CreateEventViewModel
import kotlinx.android.synthetic.main.fragment_create_event_summary.view.*

class CreateEventSummaryFragment : Fragment() {
    private var viewToBeCreated : View? = null

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }

    private var eventToBePassed : Event? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_create_event_summary, container, false)
        eventToBePassed = createEventViewModel.event.value
        eventToBePassed?.status = 0

        setEventImage()
        fillSummaryTexts()
        setConfirmEventFunction()
        setFavoriteEventButton()

        return viewToBeCreated
    }


    private fun setConfirmEventFunction() {
        viewToBeCreated?.createEventSummaryConfirmButton?.setOnClickListener {
            eventToBePassed?.timeStamp = FieldValue.serverTimestamp()
            Log.d("OSMAN", eventToBePassed.toString())
        }
    }

    private fun setFavoriteEventButton() {
        viewToBeCreated?.createEventSummaryFavoriteImage?.setOnClickListener {
            eventToBePassed?.isFavorite = !(eventToBePassed?.isFavorite ?: false)

            if (eventToBePassed?.isFavorite == true)
                viewToBeCreated?.createEventSummaryFavoriteImage?.setImageResource(R.drawable.favorite_selected)
            else
                viewToBeCreated?.createEventSummaryFavoriteImage?.setImageResource(R.drawable.favorite)
        }
    }

    private fun fillSummaryTexts() {
        viewToBeCreated?.apply {
            createEventSummaryEventNameInput?.text = eventToBePassed?.name
            createEventSummaryDescriptionInput.text = eventToBePassed?.description
            createEventSummaryPrivacyInput.text = if (eventToBePassed?.isPrivate == true) "True" else "False"
            createEventSummaryWhoInvitedInput.text = eventToBePassed?.eventWithWhomNames?.toString()
            createEventSummaryLocationNameInput.text = eventToBePassed?.locationName
            createEventSummaryLocationDescriptionInput.text = eventToBePassed?.locationDescription
            createEventSummaryLocationAddressInput.text = eventToBePassed?.locationAddress
            var dateAsString = ""
            eventToBePassed?.date?.map {  Event.convertDateToReadableFormat(it) }?.forEach { dateAsString += it}
            createEventSummaryWhenInput.text = dateAsString
        }
    }

    private fun setEventImage() {
        val point = Point()
        activity?.windowManager?.defaultDisplay?.getSize(point)

        val heightOfEventDetailImageView = (point.x - 16) * 9 / 16 // THIS IS HARDCODED - NOT NICE

        viewToBeCreated?.createEventSummaryEventImage?.apply {
            layoutParams?.height = heightOfEventDetailImageView
            setImageBitmap(eventToBePassed?.image)
        }

    }


}