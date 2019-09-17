package com.jora.socialup.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jora.socialup.models.DateTimeInfo
import com.jora.socialup.models.Event
import com.jora.socialup.models.FriendInfo

class CreateEventViewModel : ViewModel() {
    private var eventData = MutableLiveData<Event>()
    val event : LiveData<Event>
        get() = eventData


    private var eventHistoryData = MutableLiveData(ArrayList<Event>())
    val eventHistory : LiveData<ArrayList<Event>>
        get() = eventHistoryData

    private var eventDateTimeData = MutableLiveData<ArrayList<DateTimeInfo>>()
    val eventDateTime : LiveData<ArrayList<DateTimeInfo>>
        get() = eventDateTimeData

    private var friendsData = MutableLiveData<ArrayList<FriendInfo>>()
    val friends : LiveData<ArrayList<FriendInfo>>
        get() = friendsData

    private var friendIDsArrayListData = MutableLiveData<ArrayList<String>>()
    val friendsIdsArrayList : LiveData<ArrayList<String>>
        get() = friendIDsArrayListData

    private var friendsMapData = MutableLiveData<MutableMap<String, FriendInfo>>()
    val friendsMap : LiveData<MutableMap<String, FriendInfo>>
        get() = friendsMapData

    private var isLocationTickMenuPresentData = MutableLiveData<Boolean>()
    val isLocationTickMenuPresent : LiveData<Boolean>
        get() = isLocationTickMenuPresentData



    fun updateEventDateTime(updateTo: ArrayList<DateTimeInfo>) {
        eventDateTimeData.value = updateTo
    }

    fun updateIsLocationTickMenuPresent(isPresent : Boolean) {
        isLocationTickMenuPresentData.value = isPresent
    }

    fun updateEventData(updateTo: Event?) {
        updateTo?.let { eventData.value = it }
    }

    fun updateFriendsData(updateTo: ArrayList<FriendInfo>?) {
        updateTo?.let { friendsData.value = it }
    }

    fun updateFriendIDsArrayListData(updateTo: ArrayList<String>?) {
        updateTo?.let { friendIDsArrayListData.value = it }
    }

    fun updateFriendsMapData(updateTo: MutableMap<String, FriendInfo>?) {
        updateTo?.let { friendsMapData.value = it }
    }

}