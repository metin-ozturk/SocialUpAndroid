package com.jora.socialup.cloudMessaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

class ActionButtonBroadcastReceiver : BroadcastReceiver() {
    private val broadcastReceiverTag = "ActionBroadReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {

        val actionContent = intent?.action
        val notificationID = intent?.extras?.get("notificationID") as? Int

        val notificationReceiverID = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificationSenderID = intent?.getStringExtra("senderID") ?: return

        val data = hashMapOf("userID" to (notificationReceiverID), "senderID" to (notificationSenderID))

        if (actionContent == friendshipRequestApprovedNotificationActionConstant) {
            callFriendshipCloudFunctionByName("addFriend",
                "Added Friend Successfully",
                "Couldn't Add Friend",
                data, context)


        } else if (actionContent == friendshipRequestRejectedNotificationActionConstant) {

            callFriendshipCloudFunctionByName("removeFriendshipRequest",
                "Rejected Friendship Request",
                "Couldn't Reject Friendship Request",
                data, context)
        }

        // Remove Notification
        NotificationManagerCompat.from(context ?: return).cancel(notificationID ?: return)

        // Close Notification Tray
        val intentToCloseNotificationTray = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(intentToCloseNotificationTray)


    }

    private fun callFriendshipCloudFunctionByName(cloudFunctionName: String, toastMessageSuccess: String, toastMessageFailure : String,
                                                  data: HashMap<String, String> ,context: Context?) {
        FirebaseFunctions.getInstance().getHttpsCallable(cloudFunctionName)
            .call(data)
            .addOnSuccessListener {
                Toast.makeText(context, toastMessageSuccess, Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(context, toastMessageFailure, Toast.LENGTH_SHORT).show()
                Log.d(broadcastReceiverTag, "Error While Calling $cloudFunctionName Cloud Function", it)
            }
    }
}