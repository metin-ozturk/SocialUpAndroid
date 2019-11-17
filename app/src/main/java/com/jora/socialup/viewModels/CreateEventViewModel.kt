package com.jora.socialup.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jora.socialup.models.DateTimeInfo
import com.jora.socialup.models.Event
import com.jora.socialup.models.LocationSelectionStatus

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


    private var locationSelectionStatusData = MutableLiveData<LocationSelectionStatus>()
    val locationSelectionStatus : LiveData<LocationSelectionStatus>
        get() = locationSelectionStatusData

    private var isPagingEnabledData = MutableLiveData<Boolean>(true)
    val isPagingEnabled : LiveData<Boolean>
        get() = isPagingEnabledData


    fun updateEventDateTime(updateTo: ArrayList<DateTimeInfo>) {
        eventDateTimeData.value = updateTo
    }

    fun updateLocationSelectionStatus(updateTo : LocationSelectionStatus) {
        locationSelectionStatusData.value = updateTo
    }

    fun updateEventData(updateTo: Event?) {
        updateTo?.let { eventData.value = it }
    }

    fun updateIsPagingEnabled(updateTo: Boolean) {
        isPagingEnabledData.value = updateTo
    }

}