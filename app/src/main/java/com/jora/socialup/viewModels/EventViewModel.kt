package com.jora.socialup.viewModels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.storage.FirebaseStorage
import com.jora.socialup.models.Event
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

    private val eventResponseStatusData = MutableLiveData<Int>()
    val eventResponseStatus : LiveData<Int>
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

    fun assertEventResponseStatus(status: Int) {
        eventResponseStatusData.value = status
    }

    fun assertIsFavorite(isEventFavorite: Boolean) {
        isFavoriteData.value = isEventFavorite
    }


    fun updateEventsArrayWithViewedEvent(updateEventsArrayTo:Event) {
        //updates events array by adding changes that were made to event which was viewed in detail.

        val arrayToBeUpdated = eventsArrayData.value ?: return
        val position = lastFocusedRowData.value ?: return
        arrayToBeUpdated[position] = updateEventsArrayTo
        eventsArrayData.value = arrayToBeUpdated
    }

    fun downloadCurrentUserProfilePhoto() {

        FirebaseStorage.getInstance().reference.child("Images/Users/MKbCN5M1gnZ9Yi427rPf2SzyvqM2/profilePhoto.jpeg").getBytes(1024 * 1024).addOnSuccessListener {
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
