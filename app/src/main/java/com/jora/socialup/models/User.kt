package com.jora.socialup.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.grpc.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.tasks.await

private const val userTag = "User"

class User(var ID: String? = null, var name: String? = null, var email : String? = null, var gender : String? = null,
           var birthday: String? = null, var friends: ArrayList<String>? = null, var hasActiveNotification: Boolean? = null) {

    override fun toString(): String {
        return "ID: $ID \n Name: $name \n Email: $email \n Gender: $gender \n Birthday: $birthday \n FriendList: $friends \n HasActiveNotification: $hasActiveNotification"
    }

    fun returnUserInformation(): Map<String, Any> {
        return mapOf("ID" to ID!!, "Name" to name!!, "Email" to email!!, "Gender" to gender!!, "Birthday" to birthday!!, "FriendList" to friends!!, "HasActiveNotification" to hasActiveNotification!!)
    }

    companion object {
        suspend fun downloadUserNotificationInfo(receiverID: String, completion: (ArrayList<UserNotification>) -> Unit) {
            val userNotifications = ArrayList<UserNotification>()

           val friendshipRequestDocuments =  FirebaseFirestore.getInstance().collection("users")
                .document(receiverID).collection("friendshipRequests").get().await()

            friendshipRequestDocuments.forEach {
                val downloadUserImage = FirebaseStorage.getInstance().reference
                    .child("Images/Users/${it.id}/profilePhoto.jpeg")
                    .getBytes( 2048 * 2048).asDeferred()
                val downloadUserName = FirebaseFirestore.getInstance().collection("users")
                    .document(it.id).get().asDeferred()

                val userImageAsByteArray = downloadUserImage.await() as ByteArray
                val userImageAsBitmap = BitmapFactory.decodeByteArray(userImageAsByteArray, 0, userImageAsByteArray.size)

                val downloadedUserName = (downloadUserName.await() as DocumentSnapshot).data?.get("Name") as String

                val userNotification = UserNotification(NotificationType.FriendshipRequest,
                    FriendshipRequestStatus.getFriendshipRequestStatusByValue((it.data.get("FriendshipRequestStatus") as? Long)?.toInt() ?: 0),
                    it.id,
                    receiverID,
                    userImageAsBitmap,
                    downloadedUserName)

                userNotifications.add(userNotification)

                if (userNotifications.count() == friendshipRequestDocuments.count()) {
                    completion(userNotifications)
                }
            }



        }


        suspend fun downloadUserInfoForProfileViewing(
            userID: String,
            signedInUserID: String,
            returnUserInfo: (User, Bitmap, User, Int) -> Unit
        ) {
            var downloadedUser: User? = null
            var downloadedUserImage: Bitmap? = null
            var signedInUser: User? = null
            var friendshipRequestStatusRetrieved: Int? = null

            val getUserInfo = FirebaseFirestore.getInstance().collection("users")
                .document(userID).get().asDeferred()
            val getUserImage = FirebaseStorage.getInstance()
                .reference.child("Images/Users/$userID/profilePhoto.jpeg")
                .getBytes(2048 * 2048).asDeferred()
            val getSignedInUserInfo =
                FirebaseFirestore.getInstance().collection("users").document(signedInUserID)
                    .get().asDeferred()
            val friendshipRequestStatusPromise = FirebaseFirestore.getInstance().collection("users")
                .document(signedInUserID).collection("friendshipRequests").document(userID).get()
                .asDeferred()

            try {
                val userInfoData = getUserInfo.await() as DocumentSnapshot

                downloadedUser = User().apply {
                    userInfoData.data?.also {
                        birthday = it["Birthday"] as String
                        name = it["Name"] as String
                        email = it["Email"] as String
                        gender = it["Gender"] as String
                        friends = it["FriendList"] as ArrayList<String>
                        ID = it["ID"] as String
                        hasActiveNotification = it["HasActiveNotification"] as Boolean
                    }
                }

                val userImageAsByteArray = getUserImage.await() as ByteArray
                downloadedUserImage = BitmapFactory.decodeByteArray(
                    userImageAsByteArray,
                    0,
                    userImageAsByteArray.size
                )


                friendshipRequestStatusRetrieved = (friendshipRequestStatusPromise.await().data?.get("FriendshipRequestStatus") as? Long)?.toInt()


                val signedInUserInfoData = getSignedInUserInfo.await() as DocumentSnapshot
                signedInUser = User().apply {
                    signedInUserInfoData.data?.also {
                        birthday = it["Birthday"] as String
                        name = it["Name"] as String
                        email = it["Email"] as String
                        gender = it["Gender"] as String
                        friends = it["FriendList"] as ArrayList<String>
                        ID = it["ID"] as String
                        hasActiveNotification = it["HasActiveNotification"] as Boolean
                    }
                }

            } catch (e: Exception) {
                Log.d(userTag, "USER INFO DOWNLOAD FAILED WITH: ", e)
            }

            returnUserInfo(downloadedUser ?: return, downloadedUserImage ?: return,
                signedInUser ?: return, friendshipRequestStatusRetrieved ?: 0)



        }

        fun downloadUserInfo(userID: String, returnUserInfo: (User, Bitmap) -> Unit) {
            var downloadedUser : User? = null
            var downloadedUserImage: Bitmap? = null

            val getUserInfo = FirebaseFirestore.getInstance().collection("users")
                .document(userID).get().asDeferred()
            val getUserImage = FirebaseStorage.getInstance().reference.child("Images/Users/$userID/profilePhoto.jpeg")
                .getBytes(2048 * 2048).asDeferred()

            val bgScope = CoroutineScope(Dispatchers.IO)
            bgScope.launch {
                try {
                    val userInfoData = getUserInfo.await() as DocumentSnapshot
                    downloadedUser = User().apply {
                        userInfoData.data?.also {
                            birthday = it["Birthday"] as String
                            name = it["Name"] as String
                            email = it["Email"] as String
                            gender = it["Gender"] as String
                            friends = it["FriendList"] as ArrayList<String>
                            ID = it["ID"] as String
                            hasActiveNotification = it["HasActiveNotification"] as Boolean
                        }
                    }
                    val userImageAsByteArray = getUserImage.await() as ByteArray

                    downloadedUserImage = BitmapFactory.decodeByteArray(
                        userImageAsByteArray,
                        0,
                        userImageAsByteArray.size
                    )
                } catch (e: Exception) {
                    Log.d(userTag, "USER INFO DOWNLOAD FAILED WITH: ", e)
                }

                withContext(Dispatchers.Main) {
                    returnUserInfo(downloadedUser ?: return@withContext, downloadedUserImage ?: return@withContext )
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
                        .getBytes(2048 * 2048).asDeferred()


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