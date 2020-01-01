package com.jora.socialup.fragments.createEvent

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.jora.socialup.R
import com.jora.socialup.activities.EventCreateActivity
import com.jora.socialup.activities.HomeActivity
import com.jora.socialup.fragments.eventFeedAndDetail.EventCreationFinishedRequestCode
import com.jora.socialup.helpers.ProgressBarFragmentDialog
import com.jora.socialup.helpers.isInPortraitMode
import com.jora.socialup.models.Event
import com.jora.socialup.models.EventStatus
import com.jora.socialup.models.LocationSelectionStatus
import com.jora.socialup.viewModels.CreateEventViewModel
import kotlinx.android.synthetic.main.fragment_create_event_summary.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.asDeferred
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CreateEventSummaryFragment : Fragment() {
    private var viewToBeCreated : View? = null

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }

    private var eventToBePassed : Event? = null

    private val userID : String? by lazy {
        FirebaseAuth.getInstance().currentUser?.uid
    }

    private var progressBarFragmentDialog: ProgressBarFragmentDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fillEventFields()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_create_event_summary, container, false)

        createEventViewModel.event.observe(viewLifecycleOwner, Observer<Event> {
            fillEventFields()
            fillSummaryTexts()
        })

        createEventViewModel.locationSelectionStatus.observe(viewLifecycleOwner, Observer {
            if (it == LocationSelectionStatus.Confirmed) {
                viewToBeCreated?.apply {
                    createEventSummaryLocationNameInput.text = eventToBePassed?.locationName
                    createEventSummaryLocationDescriptionInput.text =
                        eventToBePassed?.locationDescription
                    createEventSummaryLocationAddressInput.text = eventToBePassed?.locationAddress
                }
            }
        })

        fillSummaryTexts()
        setEventImage()
        setConfirmEventFunction()
        setFavoriteEventButton()
        setFavoriteEventButtonImage()

        return viewToBeCreated
    }


    override fun onResume() {
        super.onResume()

        if (activity!!.window.isInPortraitMode()) {
            viewToBeCreated?.createEventSummaryScrollView?.layoutParams?.height = ConstraintSet.MATCH_CONSTRAINT_SPREAD
        } else {
            viewToBeCreated?.createEventSummaryScrollView?.layoutParams?.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        }
    }

    override fun onPause() {
        super.onPause()
        createEventViewModel.updateEventData(eventToBePassed)
    }


    private fun setProgressBar() {
        progressBarFragmentDialog = ProgressBarFragmentDialog.newInstance(object: ProgressBarFragmentDialog.ProgressBarFragmentDialogInterface {
            override fun onCancel() {

            }

            override fun onDialogFragmentDestroyed() {
                progressBarFragmentDialog = null
            }
        })
    }

    private fun fillEventFields() {
        eventToBePassed = createEventViewModel.event.value
        eventToBePassed?.status = EventStatus.Default.value
        eventToBePassed?.founderID = userID
        eventToBePassed?.founderName = FirebaseAuth.getInstance().currentUser?.displayName
        eventToBePassed?.eventWithWhomID?.add(userID ?: return)
        eventToBePassed?.eventWithWhomNames?.add(FirebaseAuth.getInstance().currentUser?.displayName ?: return)
    }

    private fun fillSummaryTexts() {
        viewToBeCreated?.apply {
            createEventSummaryEventNameInput?.text = eventToBePassed?.name
            createEventSummaryDescriptionInput.text = eventToBePassed?.description
            createEventSummaryPrivacyInput.text = if (eventToBePassed?.isPrivate == true) "True" else "False"
            createEventSummaryWhoInvitedInput.text = eventToBePassed?.eventWithWhomNames?.toString()
            if (createEventViewModel.locationSelectionStatus.value == LocationSelectionStatus.Confirmed) {
                createEventSummaryLocationNameInput.text = eventToBePassed?.locationName
                createEventSummaryLocationDescriptionInput.text = eventToBePassed?.locationDescription
                createEventSummaryLocationAddressInput.text = eventToBePassed?.locationAddress
            }
            var dateAsString = ""
            eventToBePassed?.date?.map {  Event.convertDateToReadableFormat(it) }?.forEach { dateAsString += it}
            createEventSummaryWhenInput.text = dateAsString
        }
    }


    private fun setConfirmEventFunction() {
        viewToBeCreated?.createEventSummaryConfirmButton?.setOnClickListener {

            if (progressBarFragmentDialog?.isLoadingInProgress == true) return@setOnClickListener

            setProgressBar()
            progressBarFragmentDialog?.show(fragmentManager ?: return@setOnClickListener, null)

            eventToBePassed?.timeStamp = FieldValue.serverTimestamp()

            val eventID = FirebaseFirestore.getInstance().collection("events").document().id
            eventToBePassed?.iD = eventID

            val userEventReference = FirebaseFirestore.getInstance().collection("users").document(userID ?: "").collection("events").document(eventID)
            val eventImageStorageReference = FirebaseStorage.getInstance().reference.child("Images/Events/$eventID/eventPhoto.jpeg")
            val eventReference = FirebaseFirestore.getInstance().collection("events").document(eventID)

            var eventImageAsInputStream : ByteArrayInputStream? = null
            if (eventToBePassed?.hasImage == true) {
                val eventImageToBeUploadedAsBitmap = viewToBeCreated?.createEventSummaryEventImage?.drawToBitmap() ?: return@setOnClickListener
                val outputStream = ByteArrayOutputStream()
                eventImageToBeUploadedAsBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                val eventImageToBeUploadedAsByteArray = outputStream.toByteArray()
                eventImageAsInputStream = ByteArrayInputStream(eventImageToBeUploadedAsByteArray)
            }


            val votedForDate = mutableMapOf<String, Boolean>()
            eventToBePassed?.date?.forEach {
                votedForDate[it.substring(0, it.length)] = false
            }

            var eventImageTaskDeferred : Deferred<UploadTask.TaskSnapshot>? = null
            if (eventToBePassed?.hasImage == true)
                eventImageTaskDeferred = eventImageStorageReference.putStream(eventImageAsInputStream ?: return@setOnClickListener).asDeferred()


            val eventInformationTaskDeferred = eventReference.set(eventToBePassed?.returnEventAsMap() ?: return@setOnClickListener).asDeferred()

            val eventUserSpecificInformationMap = mutableMapOf("EventResponseStatus" to 0,
                "EventIsFavorite" to (eventToBePassed?.isFavorite ?: false))
            votedForDate.map { eventUserSpecificInformationMap[it.key] = it.value }
            val eventUserSpecificInformationTaskDeferred = userEventReference.set(eventUserSpecificInformationMap).asDeferred()

            // For Invited Persons, event is never favorite.
            eventUserSpecificInformationMap["EventIsFavorite"] = false
            val invitedPersonsUserSpecificEventInformationTaskDeferred = eventToBePassed?.eventWithWhomID?.map {
                FirebaseFirestore.getInstance().collection("users").document(it)
                    .collection("events").document(eventID).set(eventUserSpecificInformationMap).asDeferred()
            }

            val bgScope = CoroutineScope(Dispatchers.IO)
            bgScope.launch {
                eventUserSpecificInformationTaskDeferred.await()
                eventInformationTaskDeferred.await()
                eventImageTaskDeferred?.await()
                invitedPersonsUserSpecificEventInformationTaskDeferred?.map {it.await()}

                withContext(Dispatchers.Main) {
                    progressBarFragmentDialog?.dismiss()
                    activity!!.setResult(EventCreationFinishedRequestCode)
                    activity!!.finish()
                    bgScope.cancel()
                }
            }


        }
    }


    private fun setFavoriteEventButton() {
        viewToBeCreated?.createEventSummaryFavoriteImage?.setOnClickListener {
            eventToBePassed?.isFavorite = !(eventToBePassed?.isFavorite ?: false)
            setFavoriteEventButtonImage()
        }
    }

    private fun setFavoriteEventButtonImage() {
        if (eventToBePassed?.isFavorite == true) {
            viewToBeCreated?.createEventSummaryFavoriteImage?.setImageResource(R.drawable.favorite_selected)
            eventToBePassed?.isFavorite = true
        }
        else {
            viewToBeCreated?.createEventSummaryFavoriteImage?.setImageResource(R.drawable.favorite)
            eventToBePassed?.isFavorite = false
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