package com.jora.socialup.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import com.jora.socialup.models.Event

class EventHistoryRecyclerViewAdapter : RecyclerView.Adapter<BaseViewHolder>() {

    private var pastEvents = ArrayList<Event>()

    internal class HistoryItemHolder(view: View) : BaseViewHolder(view) {
        internal var pastEvent = view.findViewById<TextView>(R.id.eventHistoryListlikePastEvent)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_event_history_listlike, parent,false)
        return HistoryItemHolder(itemView)
    }

    override fun getItemCount(): Int {
        return if (pastEvents.size > 0) pastEvents.size else 1
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val castedHolder = holder as HistoryItemHolder

        castedHolder.pastEvent.text = if (pastEvents.size > 0) pastEvents[position].name else "Loading..."
    }

    fun loadData(eventsData: ArrayList<Event>) {
        pastEvents = eventsData
        notifyDataSetChanged()
    }
}