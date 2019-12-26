package com.jora.socialup.models

import android.graphics.Bitmap

enum class NotificationType(val value: Int) {
    FriendshipRequest(1)
}

data class UserNotification(internal var notificationType: NotificationType,
                            internal var friendshipRequestStatus: FriendshipRequestStatus,
                            internal var senderID : String,
                            internal var receiverID : String,
                            internal var senderImage : Bitmap,
                            internal var senderName : String)