package com.jora.socialup.viewModels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.storage.FirebaseStorage
import com.jora.socialup.fragments.eventFeedAndDetail.EventResponseStatus
import com.jora.socialup.models.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.collections.ArrayList


class EventViewModel : ViewModel() {

    private var eventsArrayData = MutableLiveData<ArrayList<Event>>()
    val eventsArray : LiveData<ArrayList<Event>>
        get() = eventsArrayData

    private val eventData = MutableLiveData<Event>()
    val event : LiveData<Event>
        get() = eventData

    private val currentUserImageData = MutableLiveData<Bitmap>()
    val currentUserImage : LiveData<Bitmap>
        get() = currentUserImageData

    private val votedForDatesData = MutableLiveData<MutableMap<String, Boolean>>()
    val votedForDates : LiveData<MutableMap<String, Boolean>>
        get() = votedForDatesData

    private val lastFocusedRowData = MutableLiveData<Int>()
    val lastFocusedRow : LiveData<Int>
        get() = lastFocusedRowData

    private val eventResponseStatusData = MutableLiveData<EventResponseStatus>()
    val eventResponseStatus : LiveData<EventResponseStatus>
        get() = eventResponseStatusData

    private val isFavoriteData = MutableLiveData<Boolean>()
    val isFavorite : LiveData<Boolean>
        get() = isFavoriteData


    fun assertWhichViewToBeShowed(eventToBeShowed: Event) {
        eventData.value = eventToBeShowed
    }

    fun assertWhichDatesToBeUpdated(datesToBeUpdated: MutableMap<String, Boolean>) {
        votedForDatesData.value = datesToBeUpdated
    }

    fun assertWhichRowToBeFocused(row: Int) {
        lastFocusedRowData.value = row
    }

    fun assertEventResponseStatus(status: EventResponseStatus) {
        eventResponseStatusData.value = status
    }

    fun assertIsFavorite(isEventFavorite: Boolean) {
        isFavoriteData.value = isEventFavorite
    }

    fun updateEventsArrayWithViewedEvent(updateEventsArrayTo:Event) {
        //updates events array by adding changes that were made to event which was viewed in detail.

        val arrayToBeUpdated = eventsArrayData.value ?: return
        val position = lastFocusedRowData.value ?: return

        // If position is -1 then event was searched otherwise it was selected from the feed

        if (position != -1) arrayToBeUpdated[position] = updateEventsArrayTo
        else {
            for (index in 0 until arrayToBeUpdated.size) {
                if (arrayToBeUpdated[index].iD == updateEventsArrayTo.iD) {
                    arrayToBeUpdated[index] = updateEventsArrayTo
                    break
                }
            }
        }

        eventsArrayData.value = arrayToBeUpdated
    }

    fun downloadCurrentUserProfilePhoto(userID : String) {

        FirebaseStorage.getInstance().reference.child("Images/Users/$userID/profilePhoto.jpeg").getBytes(1024 * 1024).addOnSuccessListener {
            currentUserImageData.value = BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }

    fun downloadEvents() {

        Event.downloadEventIDs { docIDs ->
            docIDs.forEach { docID ->

                Event.downloadEventInformation(docID) {
                    val arrayToBeUpdated = eventsArrayData.value ?: ArrayList()
                    arrayToBeUpdated.add(it)
                    eventsArrayData.value = arrayToBeUpdated
                }
            }
        }
    }


}
