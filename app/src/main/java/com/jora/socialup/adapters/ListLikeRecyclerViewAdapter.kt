package com.jora.socialup.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import kotlin.collections.ArrayList

class ListLikeRecyclerViewAdapter(private var titleData: ArrayList<String>, private var subtitleData : ArrayList<String>? = null,
                                  private var viewsToBeSelected: Map<String, Boolean>? = null,
                                  private val dates: ArrayList<String>? = null) : RecyclerView.Adapter<BaseViewHolder>() {

    inner class SearchItemHolder(view: View) : BaseViewHolder(view)  {
        internal var title = view.findViewById<TextView>(R.id.ListLikeRecyclerViewTitle)
        internal var subtitle = view.findViewById<TextView>(R.id.ListLikeRecyclerViewSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.event_search_list_row, parent,false)
        return SearchItemHolder(itemView)
    }

    override fun getItemCount(): Int {
        return titleData.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val castedHolder = holder as SearchItemHolder
        castedHolder.title.text = titleData[position]
        castedHolder.subtitle.text = subtitleData?.get(position) ?: ""

        if (viewsToBeSelected == null) return

        val date = dates!!.get(position)
        val isViewSelected = viewsToBeSelected!![date] as Boolean

        if (isViewSelected) {
            castedHolder.itemView.isSelected = true
            castedHolder.itemView.setBackgroundColor(Color.GREEN)
        } else {
            castedHolder.itemView.isSelected = false
            castedHolder.itemView.setBackgroundColor(Color.WHITE)
        }

    }

    fun showResults(titleDataToSet: ArrayList<String>, subtitleDataToSet: ArrayList<String>? = null) {
        titleData = titleDataToSet
        subtitleData = subtitleDataToSet
        notifyDataSetChanged()
    }
}