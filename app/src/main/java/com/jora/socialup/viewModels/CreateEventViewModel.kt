package com.jora.socialup.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jora.socialup.adapters.CalendarAdapter
import com.jora.socialup.fragments.createEvent.FriendInfo
import com.jora.socialup.models.Event

class CreateEventViewModel : ViewModel() {
    private var eventData = MutableLiveData<Event>()
    val event : LiveData<Event>
            get() = eventData

    private var eventHistoryData = MutableLiveData<ArrayList<Event>>()
    val eventHistory : LiveData<ArrayList<Event>>
        get() = eventHistoryData

    private var eventDateTimeData = MutableLiveData<ArrayList<CalendarAdapter.DateTimeInfo>>()
    val eventDateTime : LiveData<ArrayList<CalendarAdapter.DateTimeInfo>>
        get() = eventDateTimeData

    private var friendsData = MutableLiveData<ArrayList<FriendInfo>>()
    val friends : LiveData<ArrayList<FriendInfo>>
        get() = friendsData

    private val friendIDsArrayListData = MutableLiveData<ArrayList<String>>()
    val friendsIDsArray : LiveData<ArrayList<String>>
        get() = friendIDsArrayListData

    private var friendsMapData = MutableLiveData<MutableMap<String, FriendInfo>>()
    val friendsMap : LiveData<MutableMap<String, FriendInfo>>
            get() = friendsMapData

    fun updateFriendsData(updateTo: ArrayList<FriendInfo>) {
        friendsData.value = updateTo
    }

    fun updateFriendsIDsArrayData(updateTo: ArrayList<String>) {
        friendIDsArrayListData.value = updateTo
    }

    fun updateFriendsMapData(updateTo: MutableMap<String, FriendInfo>) {
        friendsMapData.value = updateTo
    }

    fun updateEventToBeCreated(updateTo: Event) {
        eventData.value = updateTo
    }

    fun updateEventHistory(updateTo: ArrayList<Event>) {
        eventHistoryData.value = updateTo
    }

    fun updateEventDateTime(updateTo: ArrayList<CalendarAdapter.DateTimeInfo>) {
        eventDateTimeData.value = updateTo
    }

}