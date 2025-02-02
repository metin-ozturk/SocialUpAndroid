package com.jora.socialup.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import com.jora.socialup.models.Event

abstract class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view)


class EventsRecyclerViewAdapter(private val height: Int) : RecyclerView.Adapter<BaseViewHolder>() {

    companion object {
        private const val TYPE_WITHIMAGE = 0
        private const val TYPE_WITHOUTIMAGE = 1
    }

    private var eventList : ArrayList<Event> = ArrayList()


    inner class EventsItemWithImageHolder(view: View) : BaseViewHolder(view) {
        internal var title = view.findViewById<TextView>(R.id.titleWithImage)
        internal var founderName = view.findViewById<TextView>(R.id.founderWithImage)
        internal var imageView = view.findViewById<ImageView>(R.id.eventImageView)
        internal var date = view.findViewById<TextView>(R.id.dateWithImage)
        internal var location = view.findViewById<TextView>(R.id.locationWithImage)
        internal var founderImageView = view.findViewById<ImageView>(R.id.founderImageWithImageView)

        init {
            imageView.layoutParams.height = height
            imageView.requestLayout()
        }

    }

     inner class EventsItemWithOutImageHolder(view: View) : BaseViewHolder(view) {
         internal var title = view.findViewById<TextView>(R.id.titleWithoutImage)
         internal var founderName = view.findViewById<TextView>(R.id.founderWithoutImage)
         internal var date = view.findViewById<TextView>(R.id.dateWithoutImage)
         internal var location = view.findViewById<TextView>(R.id.locationWithoutImage)
         internal var founderImageView = view.findViewById<ImageView>(R.id.founderImageWithoutImageView)

    }

    override fun getItemViewType(position: Int): Int {
        val event = eventList[position]

        return if (event.image == null) TYPE_WITHOUTIMAGE else TYPE_WITHIMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when(viewType) {
                TYPE_WITHIMAGE -> {
                    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.event_list_item, parent, false)
                    EventsItemWithImageHolder(itemView)
                }
                TYPE_WITHOUTIMAGE -> {
                    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.event_list_item_without_image, parent, false)
                    EventsItemWithOutImageHolder(itemView)
                } else -> {
                    throw IllegalArgumentException("Invalid view type")
            }

        }

    }


    override fun getItemCount(): Int {
        return eventList.size
    }

    override fun onBindViewHolder(holderRetrieved: BaseViewHolder, position: Int) {
        when (holderRetrieved.itemViewType) {
            TYPE_WITHIMAGE -> {
                val holder = holderRetrieved as EventsItemWithImageHolder

                val nonNullEventList = eventList ?: return
                val event = nonNullEventList[position]
                holder.title.text = event.name
                holder.founderName.text = event.founderName
                holder.imageView.setImageBitmap(event.image)
                holder.imageView.visibility = View.VISIBLE
                holder.location.text = event.locationName

                holder.date.text = when (event.date?.size) {
                    0 -> "ERROR"
                    1 -> Event.convertDateToReadableFormat(event.date?.first()!!)
                    else -> "Multiple Dates Are Proposed"
                }

                if (event.founderImage != null ) {
                    holder.founderImageView.setImageBitmap(event.founderImage)
                } else { holder.founderImageView.setImageResource(R.drawable.imageplaceholder) }
            }
            TYPE_WITHOUTIMAGE -> {
                val holder = holderRetrieved as EventsItemWithOutImageHolder

                val nonNullEventList = eventList ?: return
                val event = nonNullEventList[position]
                holder.title.text = event.name
                holder.founderName.text = event.founderName
                holder.location.text = event.locationName

                holder.date.text = when (event.date?.size) {
                    0 -> "ERROR"
                    1 -> Event.convertDateToReadableFormat(event.date?.first()!!)
                    else -> "Multiple Dates Are Proposed"
                }

                if (event.founderImage != null ) {
                    holder.founderImageView.setImageBitmap(event.founderImage)
                } else { holder.founderImageView.setImageResource(R.drawable.imageplaceholder) }
            }
        }

    }

    fun addSingleEventData(updateWith: Event) {
        eventList.add(updateWith)
        notifyItemChanged(eventList.size - 1)
    }

    fun addMultipleEventData(updateTo: ArrayList<Event>) {
        eventList = updateTo
        notifyDataSetChanged()
    }

    fun emptyEventData() {
        eventList = ArrayList()
        notifyDataSetChanged()
    }
}

