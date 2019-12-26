package com.jora.socialup.adapters

import android.opengl.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.functions.FirebaseFunctions
import com.jora.socialup.R
import com.jora.socialup.fragments.eventFeedAndDetail.InviteFriendsDialogFragment
import com.jora.socialup.helpers.ProgressBarFragmentTag
import com.jora.socialup.models.FriendshipRequestStatus
import com.jora.socialup.models.UserNotification

class UserNotificationAdapter(private var userNotifications : ArrayList<UserNotification>) : RecyclerView.Adapter<BaseViewHolder>() {

    interface UserNotificationAdapterInterface {
        fun onStartOfCloudFun()
        fun onFinishOfCloudFun()
    }

    var listener : UserNotificationAdapterInterface? = null


    internal class UserNotificationHolder(view: View) : BaseViewHolder(view) {
        internal var notificationDescription = view.findViewById<TextView>(R.id.adapterUserNotificationListDescription)
        internal var userImage = view.findViewById<ImageView>(R.id.adapterUserNotificationListUserImage)
        internal var confirmImage = view.findViewById<ImageView>(R.id.adapterUserNotificationListConfirmImageView)
        internal var cancelImage = view.findViewById<ImageView>(R.id.adapterUserNotificationListCancelImageView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.adapter_user_notification_list, parent,false)
        return UserNotificationHolder(itemView)
    }

    override fun getItemCount(): Int {
        return userNotifications.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val castedHolder = holder as UserNotificationHolder
        castedHolder.userImage.setImageBitmap(userNotifications[position].senderImage)

        castedHolder.confirmImage.setOnClickListener {
            callFriendshipCloudFunctionByName("addFriend",
                userNotifications[position].receiverID,
                userNotifications[position].senderID,
                position)
        }

        castedHolder.cancelImage.setOnClickListener {
            callFriendshipCloudFunctionByName("removeFriendshipRequest",
                userNotifications[position].receiverID,
                userNotifications[position].senderID,
                position)
        }

        if (userNotifications[position].friendshipRequestStatus == FriendshipRequestStatus.ReceivedFriendshipRequest) {
            castedHolder.notificationDescription.text = " ${userNotifications[position].senderName} sent you a friendship request."
            castedHolder.confirmImage.visibility = View.VISIBLE
        } else if  (userNotifications[position].friendshipRequestStatus == FriendshipRequestStatus.ReceivedFriendshipRequest) {
            castedHolder.notificationDescription.text = "You sent a friendship request to ${userNotifications[position].senderName}}"
            castedHolder.confirmImage.visibility = View.GONE
        }
    }

    private fun callFriendshipCloudFunctionByName(cloudFunctionName: String, userID: String?, senderID : String?, position: Int) {
        val data = hashMapOf(
            "userID" to (userID ?: return),
            "senderID" to (senderID ?: return)
        )

        listener?.onStartOfCloudFun()

        FirebaseFunctions.getInstance().getHttpsCallable(cloudFunctionName)
            .call(data)
            .addOnSuccessListener {
                userNotifications.removeAt(position)
                listener?.onFinishOfCloudFun()
                notifyDataSetChanged()

            }.addOnFailureListener {
                listener?.onFinishOfCloudFun()
                Log.d("UserNotifAdapter", "Error While Calling $cloudFunctionName Cloud Function", it)
            }
    }

}