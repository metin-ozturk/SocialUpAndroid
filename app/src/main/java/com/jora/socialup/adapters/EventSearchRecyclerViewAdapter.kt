package com.jora.socialup.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import kotlin.collections.ArrayList

class EventSearchRecyclerViewAdapter(private var searchResult: ArrayList<String>) : RecyclerView.Adapter<BaseViewHolder>() {

    inner class SearchItemHolder(view: View) : BaseViewHolder(view)  {
        internal var title = view.findViewById<TextView>(R.id.eventSearchListlikeTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_event_search_listlike, parent,false)
        itemView.setBackgroundColor(Color.WHITE)
        return SearchItemHolder(itemView)
    }

    override fun getItemCount(): Int {
        return searchResult.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val castedHolder = holder as SearchItemHolder
        castedHolder.title.text = searchResult[position]

    }

    fun showResults(titleDataToSet: ArrayList<String>) {
        searchResult = titleDataToSet
        notifyDataSetChanged()
    }
}