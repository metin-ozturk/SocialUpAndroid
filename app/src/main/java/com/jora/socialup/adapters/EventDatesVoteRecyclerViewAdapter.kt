package com.jora.socialup.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import com.jora.socialup.models.Event
import com.jora.socialup.models.EventStatus

class EventDatesVoteRecyclerViewAdapter(private var event: Event,
                                        private var votedForDate: Map<String, Boolean>) : RecyclerView.Adapter<BaseViewHolder>() {
    internal class DateItemHolder(view: View) : BaseViewHolder(view) {
        internal var date = view.findViewById<TextView>(R.id.eventDatesListlikeDate)
        internal var vote = view.findViewById<TextView>(R.id.eventDatesListlikeVote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_event_dates_listlike, parent,false)
        return DateItemHolder(itemView)
    }

    override fun getItemCount(): Int {
        if (event.status == EventStatus.Date_Finalized.value) {
            return 1
        }
        return event.date?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val castedHolder = holder as DateItemHolder

        if (event.status == EventStatus.Date_Finalized.value) {
            castedHolder.date.text = Event.convertDateToReadableFormat(event.finalizedDate ?: "ERROR")
            castedHolder.vote.text = ""
            castedHolder.itemView.isSelected = false
            castedHolder.itemView.setBackgroundColor(Color.GREEN)
            return
        }

        val receivedDate = event.date?.get(position) ?: return
        castedHolder.date.text = Event.convertDateToReadableFormat(receivedDate)
        castedHolder.vote.text = event.dateVote?.get(position)

        val isViewSelected = votedForDate.get(receivedDate) ?: false

        if (isViewSelected) {
            castedHolder.itemView.isSelected = true
            castedHolder.itemView.setBackgroundColor(Color.YELLOW)
        } else {
            castedHolder.itemView.isSelected = false
            castedHolder.itemView.setBackgroundColor(Color.WHITE)
        }
    }

    fun loadData(eventData: Event, votedForDateData: Map<String, Boolean>?) {
        event = eventData
        votedForDateData?.also { votedForDate = it }
        notifyDataSetChanged()
    }
}

