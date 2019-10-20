package com.jora.socialup.cloudMessaging

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class CloudMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("OSMAN",message.data["text"] ?: "No Data Received")
        Log.d("OSMAN", message.notification?.toString() ?: "No Notification Received")
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

}