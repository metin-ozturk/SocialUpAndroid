package com.jora.socialup.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jora.socialup.models.Event

class CreateEventViewModel : ViewModel() {
    private var eventData = MutableLiveData<Event>()
    val event : LiveData<Event>
            get() = eventData

    private var eventHistoryData = MutableLiveData<ArrayList<Event>>()
    val eventHistory : LiveData<ArrayList<Event>>
        get() = eventHistoryData

    fun updateEventToBeCreated(updateTo: Event) {
        eventData.value = updateTo
    }

    fun updateEventHistory(updateTo: ArrayList<Event>) {
        eventHistoryData.value = updateTo
    }
}