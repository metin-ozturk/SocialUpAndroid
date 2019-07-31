package com.jora.socialup.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import com.jora.socialup.models.Event
import java.lang.IllegalArgumentException

open class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
}


class EventsRecyclerViewAdapter(private val eventList : List<Event>?, private val height: Int) : RecyclerView.Adapter<BaseViewHolder>() {

    companion object {
        private const val TYPE_WITHIMAGE = 0
        private const val TYPE_WITHOUTIMAGE = 1
    }


    inner class EventsItemWithImageHolder(view: View) : BaseViewHolder(view), View.OnClickListener {
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
        override fun onClick(v: View?) {
        }
    }

     inner class EventsItemWithOutImageHolder(view: View) : BaseViewHolder(view), View.OnClickListener {
         internal var title = view.findViewById<TextView>(R.id.titleWithoutImage)
         internal var founderName = view.findViewById<TextView>(R.id.founderWithoutImage)
         internal var date = view.findViewById<TextView>(R.id.dateWithoutImage)
         internal var location = view.findViewById<TextView>(R.id.locationWithoutImage)
         internal var founderImageView = view.findViewById<ImageView>(R.id.founderImageWithoutImageView)

         init {

         }
         override fun onClick(v: View?) {
         }

    }

    override fun getItemViewType(position: Int): Int {
        val nonNullEventList = eventList ?: return TYPE_WITHOUTIMAGE
        val event = nonNullEventList[position]

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
        return eventList?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_WITHIMAGE -> {
                val holder = holder as EventsItemWithImageHolder

                val nonNullEventList = eventList ?: return
                val event = nonNullEventList[position]
                holder.title.text = event.name
                holder.founderName.text = event.founderName
                holder.imageView.setImageBitmap(event.image)
                holder.imageView.visibility = View.VISIBLE
                holder.location.text = event.locationName

                holder.date.text = when (event.date?.size) {
                    0 -> "ERROR"
                    1 -> Event.convertDateToReadableFormat(Event.convertDateToReadableFormat(event.date?.first()!!))
                    else -> "Multiple Dates Are Proposed"
                }

                if (event.founderImage != null ) {
                    holder.founderImageView.setImageBitmap(event.founderImage)
                } else { holder.founderImageView.setImageResource(R.drawable.imageplaceholder) }
            }
            TYPE_WITHOUTIMAGE -> {
                val holder = holder as EventsItemWithOutImageHolder

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
}

