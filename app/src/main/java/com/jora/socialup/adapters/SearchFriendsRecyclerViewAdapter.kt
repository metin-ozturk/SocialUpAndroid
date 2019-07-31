package com.jora.socialup.adapters

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import com.jora.socialup.fragments.FriendInfo

class SearchFriendsRecyclerViewAdapter(private var friends: ArrayList<FriendInfo>)
                                        : RecyclerView.Adapter<BaseViewHolder>() {


    internal class FriendsItemHolder(view: View) : BaseViewHolder(view) {
        internal var friendName = view.findViewById<TextView>(R.id.friendsSearchTextViewListlike)
        internal var friendImage = view.findViewById<ImageView>(R.id.friendsSearchImageViewListlike)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_friends_search_listlike, parent,false)
        return FriendsItemHolder(itemView)
    }

    override fun getItemCount(): Int {
        return if (friends.size > 0) friends.size else 1
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val castedHolder = holder as FriendsItemHolder

        if (friends.size > 0) {
            castedHolder.friendName.text = friends[position].name
            castedHolder.friendImage.setImageBitmap(friends[position].image)

            if (friends[position].isSelected == true) {
                castedHolder.itemView.isSelected = true
                castedHolder.itemView.setBackgroundColor(Color.GREEN)
            } else {
                castedHolder.itemView.isSelected = false
                castedHolder.itemView.setBackgroundColor(Color.WHITE)
            }
        } else {
            castedHolder.friendName.text = "Loading..."
            castedHolder.friendImage.setImageResource(R.drawable.imageplaceholder)
        }
    }

    fun dataUpdated(friendsData: ArrayList<FriendInfo>) {
        friends = friendsData
        notifyDataSetChanged()
    }
}