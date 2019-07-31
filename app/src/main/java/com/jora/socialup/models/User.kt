package com.jora.socialup.models

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jora.socialup.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.asDeferred
import java.lang.Exception


class User() {
    var name : String? = null
    var email : String? = null
    var gender : String? = null
    var birthday: String? = null
    var friends: ArrayList<String>? = null

    companion object {
        fun downloadFriendsIDs(returnFriendsIDs: (ArrayList<String>) -> Unit) {
            FirebaseFirestore.getInstance().collection("users").document("MKbCN5M1gnZ9Yi427rPf2SzyvqM2")
                .get().addOnSuccessListener {
                    returnFriendsIDs( it.data?.get("FriendList") as ArrayList<String> )
                }
        }

        fun downloadFriendsNamesAndImages(friendID: String, returnUserNameAndImage: (String?, ByteArray?) -> Unit) {
            var friendName : String? = null
            var friendImage : ByteArray? = null

            GlobalScope.launch(Dispatchers.Main) {
                val downloadFriendName =
                    FirebaseFirestore.getInstance().collection("users").document(friendID).get().asDeferred()
                val downloadFriendImage =
                    FirebaseStorage.getInstance().reference.child("Images/Users/$friendID/profilePhoto.jpeg")
                        .getBytes(1024 * 1024).asDeferred()


                try {
                    val downloadedData = mutableListOf<Any>(downloadFriendName.await(), downloadFriendImage.await())
                    val snap = downloadedData[0] as DocumentSnapshot
                    friendName = snap.data?.get("Name") as String
                    friendImage = downloadedData[1] as ByteArray
                } catch (e: Exception) {
                    friendName = null
                    friendImage = null
                    Log.d("EVENT", "FRIEND DATA DOWNLOAD FAILED WITH: ", e)
                }

                returnUserNameAndImage(friendName, friendImage)
            }

        }

    }
}