package com.jora.socialup.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import com.jora.socialup.models.FriendInfo
import com.jora.socialup.models.FriendInviteStatus

class SearchFriendsRecyclerViewAdapter(private var friends: ArrayList<FriendInfo>)
                                        : RecyclerView.Adapter<BaseViewHolder>() {

    private var defaultHolderText = "Loading..."
    private var friendPhotoVisible = true

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

            when(friends[position].friendInviteStatus) {
                FriendInviteStatus.Selected -> castedHolder.itemView.setBackgroundColor(Color.GREEN)
                FriendInviteStatus.AboutToBeSelected -> castedHolder.itemView.setBackgroundColor(Color.YELLOW)
                else -> castedHolder.itemView.setBackgroundColor(Color.WHITE)
            }
        } else {
            castedHolder.friendName.text = defaultHolderText
            castedHolder.friendName.textSize = 24f

            if (friendPhotoVisible) castedHolder.friendImage.setImageResource(R.drawable.imageplaceholder)
            else castedHolder.friendImage.visibility = GONE
        }
    }

    fun dataUpdated(friendsData: ArrayList<FriendInfo>) {
        friends = friendsData
        notifyDataSetChanged()
    }

    fun updateDefaultHolderText(updateTo: String) {
        defaultHolderText = updateTo
        friendPhotoVisible = false
        notifyDataSetChanged()
    }
}