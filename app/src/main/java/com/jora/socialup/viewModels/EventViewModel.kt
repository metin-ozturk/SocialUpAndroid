package com.jora.socialup.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jora.socialup.models.Event
import com.jora.socialup.models.EventResponseStatus
import com.jora.socialup.models.FriendInfo
import com.jora.socialup.models.User


class EventViewModel : ViewModel() {

    var favoriteEvents = ArrayList<Event>()
    val favoriteEventsCount : Int
        get() = favoriteEvents.size

    var isFavoriteEventsToBeDownloaded = true

    var isFavorite = false

    var friends : ArrayList<FriendInfo>? = null

    private var eventsArrayData = MutableLiveData<ArrayList<Event>>()
    val eventsArray : LiveData<ArrayList<Event>>
        get() = eventsArrayData

    private val eventData = MutableLiveData<Event>()
    val event : LiveData<Event>
        get() = eventData

    private val currentUserImageData = MutableLiveData<Bitmap>()
    val currentUserImage : LiveData<Bitmap>
        get() = currentUserImageData

    private val currentUserInfoData = MutableLiveData<User>()
    val currentUserInfo : LiveData<User>
        get() = currentUserInfoData

    private val votedForDatesData = MutableLiveData<MutableMap<String, Boolean>>()
    val votedForDates : LiveData<MutableMap<String, Boolean>>
        get() = votedForDatesData

    private val lastFocusedRowData = MutableLiveData<Int>()
    val lastFocusedRow : LiveData<Int>
        get() = lastFocusedRowData

    private val eventResponseStatusData = MutableLiveData<EventResponseStatus>()
    val eventResponseStatus : LiveData<EventResponseStatus>
        get() = eventResponseStatusData


    private val isDownloadingMoreEventsData = MutableLiveData<Boolean>()
    val isDownloadingMoreEvents : LiveData<Boolean>
        get() = isDownloadingMoreEventsData


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

    fun assertEventsArray(updateTo: ArrayList<Event>) {
        eventsArrayData.value  = updateTo
    }

    fun updateUserInfo(updateTo: User) {
        currentUserInfoData.value = updateTo
    }

    fun updateUserImage(updateTo: Bitmap) {
        currentUserImageData.value = updateTo
    }


    fun downloadCurrentUserInfo(userID : String) {
        User.downloadUserInfo(userID) { userInfo, userImage ->
            currentUserImageData.value = userImage
            currentUserInfoData.value = userInfo
        }
    }

    fun downloadEvents(startIndex: Int, endIndex: Int) {
        isDownloadingMoreEventsData.value = true
        Event.downloadEventIDs { docIDs ->
            val lastItemIndexToBeDownloaded = if (docIDs.size < endIndex) docIDs.size else endIndex
            docIDs.subList(startIndex, lastItemIndexToBeDownloaded).forEach { docID ->
                Event.downloadEventInformation(docID) {
                    val arrayToBeUpdated = eventsArrayData.value ?: ArrayList()
                    arrayToBeUpdated.add(it)
                    eventsArrayData.value = arrayToBeUpdated
                    if (arrayToBeUpdated.size == lastItemIndexToBeDownloaded) isDownloadingMoreEventsData.value = false
                }
            }
        }
    }


}
