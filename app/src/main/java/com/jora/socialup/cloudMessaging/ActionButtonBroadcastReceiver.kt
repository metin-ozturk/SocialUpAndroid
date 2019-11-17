package com.jora.socialup.cloudMessaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ActionButtonBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val actionContent = intent?.action
        val notificationID = intent?.extras?.get("notificationID") as? Int

        if (actionContent == friendshipRequestApprovedNotificationActionConstant) {
            val firestoreInstance = FirebaseFirestore.getInstance()

            val notificationReceiverID = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val notificationSenderID = intent.getStringExtra("senderID") ?: return

            firestoreInstance.runTransaction {
                val receiverReference = firestoreInstance.collection("users").document(notificationReceiverID)
                val senderReference = firestoreInstance.collection("users").document(notificationSenderID)

                val receiverFriendsListSnap = it.get(receiverReference)
                val senderFriendsListSnap = it.get(senderReference)

                val newReceiverFriendsList = (receiverFriendsListSnap["FriendList"] as ArrayList<String>).apply { add(notificationSenderID) }
                val newSenderFriendsList = (senderFriendsListSnap["FriendList"] as ArrayList<String>).apply { add(notificationReceiverID) }

                it.update(receiverReference, "FriendList", newReceiverFriendsList)
                it.update(senderReference, "FriendList", newSenderFriendsList)
            }


        } else if (actionContent == friendshipRequestRejectedNotificationActionConstant) {
            Log.d("OSMAN", "REJECTED")
        }

        // Remove Notification
        NotificationManagerCompat.from(context ?: return).cancel(notificationID ?: return)

        // Close Notification Tray
        val intentToCloseNotificationTray = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(intentToCloseNotificationTray)
    }
}