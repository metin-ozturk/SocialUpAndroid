package com.jora.socialup.models

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jora.socialup.R
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.asDeferred
import java.lang.Exception

private const val userTag = "User"

class User(var name: String? = null, var email : String? = null, var gender : String? = null,
           var birthday: String? = null, var friends: ArrayList<String>? = null) {

    override fun toString(): String {
        return "Name: $name \n Email: $email \n Gender: $gender \n Birthday: $birthday \n FriendList: $friends"
    }

    fun returnUserInformation(): Map<String, Any> {
        return mapOf("Name" to name!!, "Email" to email!!, "Gender" to gender!!, "Birthday" to birthday!!, "FriendList" to friends!!)
    }

    companion object {
        fun downloadUserInfo(userID: String, returnUserInfo: (User, Bitmap) -> Unit) {
            var downloadedUser : User? = null
            var downloadedUserImage: Bitmap? = null

            val bgScope = CoroutineScope(Dispatchers.IO)

            bgScope.launch {
                val getUserInfo = FirebaseFirestore.getInstance().collection("users")
                    .document(userID).get().asDeferred()
                val getUserImage = FirebaseStorage.getInstance().reference.child("Images/Users/$userID/profilePhoto.jpeg")
                    .getBytes(1024 * 1024).asDeferred()

                try {
                    val userInfoData = getUserInfo.await() as DocumentSnapshot
                    downloadedUser = User().apply {
                        userInfoData.data?.also {
                            birthday = it["Birthday"] as String
                            name = it["Name"] as String
                            email = it["Email"] as String
                            gender = it["Gender"] as String
                            friends = it["FriendList"] as ArrayList<String>
                        }
                    }
                    val userImageAsByteArray = getUserImage.await() as ByteArray

                    downloadedUserImage = BitmapFactory.decodeByteArray(userImageAsByteArray, 0, userImageAsByteArray.size)

                } catch (e: Exception) {
                    Log.d(userTag, "USER INFO DOWNLOAD FAILED WITH: ", e)
                }

                withContext(Dispatchers.Main) {
                    returnUserInfo(downloadedUser ?: return@withContext, downloadedUserImage ?: return@withContext)
                    bgScope.cancel()
                }

            }
        }

        fun downloadFriendsIDs(userID: String, returnFriendsIDs: (ArrayList<String>) -> Unit) {
            FirebaseFirestore.getInstance().collection("users").document(userID)
                .get().addOnSuccessListener {
                    returnFriendsIDs( it.data?.get("FriendList") as ArrayList<String> )
                }
        }

        fun downloadFriendsNamesAndImages(friendID: String, returnUserNameAndImage: (String?, ByteArray?) -> Unit) {
            var friendName : String? = null
            var friendImage : ByteArray? = null

            val bgScope = CoroutineScope(Dispatchers.IO)

            bgScope.launch {
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
                    Log.d("USER", "FRIEND DATA DOWNLOAD FAILED WITH: ", e)
                }

                withContext(Dispatchers.Main) {
                    returnUserNameAndImage(friendName, friendImage)
                    bgScope.cancel()
                }
            }

        }

    }
}