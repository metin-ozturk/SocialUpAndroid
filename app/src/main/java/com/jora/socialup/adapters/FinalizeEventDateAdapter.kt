package com.jora.socialup.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import com.jora.socialup.models.Event
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class FinalizeEventDateAdapter:RecyclerView.Adapter<BaseViewHolder>() {

    private var eventDates : ArrayList<String>? = null
    private var isEventDatesChecked = mutableMapOf<String, Boolean>()
    val finalizedDate : String?
        get() { return isEventDatesChecked.filter { it.value }.keys.firstOrNull() }

    internal class DatesItemHolder(view: View) : BaseViewHolder(view) {
        internal var dateCheckBox = view.findViewById<CheckBox>(R.id.finalizeEventDateAdapterCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_finalize_event_date, parent,false)
        eventDates?.forEach { isEventDatesChecked[it] = false }
        return DatesItemHolder(itemView)
    }

    override fun getItemCount(): Int {
        return eventDates?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val castedHolder = holder as DatesItemHolder

        castedHolder.dateCheckBox.text = Event.convertDateToReadableFormat(eventDates?.get(position) ?: "")
        castedHolder.dateCheckBox.isChecked = isEventDatesChecked[eventDates?.get(position)] == true

        castedHolder.dateCheckBox.setOnClickListener {
            isEventDatesChecked.forEach {
                if (it.key == eventDates?.get(position)) {
                    isEventDatesChecked[it.key] = isEventDatesChecked[it.key] == false
                } else {
                    isEventDatesChecked[it.key] = false
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                notifyDataSetChanged()
                cancel()
            }
        }
    }

    fun loadEventDates(eventDates: ArrayList<String>?){
        this.eventDates = eventDates
        notifyDataSetChanged()
    }
}