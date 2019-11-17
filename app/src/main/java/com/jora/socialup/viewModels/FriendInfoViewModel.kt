package com.jora.socialup.viewModels

import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jora.socialup.models.FriendInfo
import com.jora.socialup.models.FriendInviteStatus
import com.jora.socialup.models.User

class FriendInfoViewModel : ViewModel() {
    private var friendsData = MutableLiveData<ArrayList<FriendInfo>>()
    val friends: LiveData<ArrayList<FriendInfo>>
        get() = friendsData

    private var isFriendInfoDownloadCompletedData = MutableLiveData<Boolean>(false)
    val isFriendInfoDownloadCompleted : LiveData<Boolean>
        get() = isFriendInfoDownloadCompletedData


    fun downloadFriendInfo(userID: String?, invitedPersonsIDs: ArrayList<String>?) {
        var friend: FriendInfo

        User.downloadFriendsIDs(userID ?: return) { friendIDs ->
            friendIDs.forEach { friendID ->
                if (friendsData.value?.firstOrNull { it.iD == friendID } != null) return@forEach

                User.downloadFriendsNamesAndImages(friendID) { friendName, friendImage ->

                    if (friendName == null || friendImage == null) {
                        friend = FriendInfo(
                            friendID,
                            "ERROR",
                            null,
                            FriendInviteStatus.NotSelected
                        )

                    } else {
                        friend = if (invitedPersonsIDs?.contains(friendID) == true) {
                            FriendInfo(
                                friendID,
                                friendName,
                                BitmapFactory.decodeByteArray(friendImage, 0, friendImage.size),
                                FriendInviteStatus.Selected
                            )
                        } else {
                            FriendInfo(
                                friendID,
                                friendName,
                                BitmapFactory.decodeByteArray(friendImage, 0, friendImage.size),
                                FriendInviteStatus.NotSelected
                            )
                        }

                    }

                    var arrayToBeUpdated = friendsData.value
                    if (arrayToBeUpdated == null) arrayToBeUpdated = arrayListOf(friend)
                    else arrayToBeUpdated.add(friend)

                    arrayToBeUpdated.sortWith(compareBy({ it.friendInviteStatus?.value }, { it.name }))

                    friendsData.value = arrayToBeUpdated
                    if (friendsData.value?.size == friendIDs.size) isFriendInfoDownloadCompletedData.value = true
                 }
            }
        }
    }

    fun updateFriendInfo(updateTo: ArrayList<FriendInfo>) {
        friendsData.value = updateTo
    }

}