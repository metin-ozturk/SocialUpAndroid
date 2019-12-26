package com.jora.socialup.cloudMessaging

import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.storage.FirebaseStorage
import com.jora.socialup.R
import com.jora.socialup.activities.EventCreateActivity
import com.jora.socialup.activities.HomeFeedActivity
import com.jora.socialup.models.NotificationType


const val friendshipRequestApprovedNotificationActionConstant = "friendshipRequestApproved"
const val friendshipRequestRejectedNotificationActionConstant = "friendshipRequestRejected"

class CloudMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (message.data["notificationType"] == NotificationType.FriendshipRequest.value.toString())
            friendshipRequestReceived(message)

    }

    override fun onNewToken(token: String) {
        sendCloudMessagingRegistrationTokenToServer(token)
    }


    private fun sendCloudMessagingRegistrationTokenToServer(token: String) {
        // If user is logged in update the token
        FirebaseAuth.getInstance().currentUser?.uid?.also {
            FirebaseFirestore.getInstance().collection("users").document(it)
                .update("CloudMessagingToken", token)
        }
    }

    private fun friendshipRequestReceived(message: RemoteMessage) {
        FirebaseStorage.getInstance().reference.child("Images/Users/${message.data["senderID"]}/profilePhoto.jpeg")
            .getBytes(1024 * 1024).addOnSuccessListener {
                val notificationID = System.currentTimeMillis().toInt()

                val intent = Intent(this, HomeFeedActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

                val approveButtonIntent = Intent(this, ActionButtonBroadcastReceiver::class.java).apply {
                    putExtra("notificationID", notificationID)
                    putExtra("senderID", message.data["senderID"])
                    action = friendshipRequestApprovedNotificationActionConstant
                }
                val pendingApproveButtonIntent = PendingIntent.getBroadcast(this, 0, approveButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT)


                val rejectButtonIntent = Intent(this, ActionButtonBroadcastReceiver::class.java).apply {
                    putExtra("notificationID", notificationID)
                    putExtra("senderID", message.data["senderID"])
                    action = friendshipRequestRejectedNotificationActionConstant
                }
                val pendingRejectButtonIntent = PendingIntent.getBroadcast(this, 0, rejectButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                val builder =
                    NotificationCompat.Builder(this, getString(R.string.friendship_channel_id))
                        .setSmallIcon(R.drawable.friend)
                        .setLargeIcon(BitmapFactory.decodeByteArray(it, 0, it.size))
                        .setContentTitle("Friendship Request")
                        .setContentText("${message.data["senderName"]} sent you a friendship request.")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .addAction(R.drawable.cancel, "Reject", pendingRejectButtonIntent)
                        .addAction(R.drawable.tick, "Approve", pendingApproveButtonIntent)

                NotificationManagerCompat.from(this)
                    .notify(notificationID, builder.build())
            }
    }

}