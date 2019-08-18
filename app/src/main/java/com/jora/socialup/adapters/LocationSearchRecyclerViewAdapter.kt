package com.jora.socialup.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import com.jora.socialup.fragments.createEvent.LocationInfo

class LocationSearchRecyclerViewAdapter : RecyclerView.Adapter<BaseViewHolder>() {

    private var searchedLocations = ArrayList<LocationInfo>()

    internal class LocationsItemHolder(view: View) : BaseViewHolder(view) {
        internal var locationName = view.findViewById<TextView>(R.id.locationSearchTitleListlike)
        internal var locationDescription = view.findViewById<TextView>(R.id.locationSearchSubtitleListLike)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_location_search_listlike, parent,false)
        itemView.setBackgroundColor(Color.WHITE)
        itemView.alpha = 1f
        return LocationsItemHolder(itemView)
    }

    override fun getItemCount(): Int {
        return searchedLocations.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val castedHolder = holder as LocationsItemHolder
        castedHolder.locationName.text = searchedLocations[position].name
        castedHolder.locationDescription.text = searchedLocations[position].description
    }

    fun updateSearchedLocations(updateTo: ArrayList<LocationInfo>) {
        searchedLocations = updateTo
        notifyDataSetChanged()
    }


}