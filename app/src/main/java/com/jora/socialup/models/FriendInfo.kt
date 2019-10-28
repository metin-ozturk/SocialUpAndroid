package com.jora.socialup.models

import android.graphics.Bitmap


data class FriendInfo(internal var iD: String? = null, internal var name : String? = null, internal var image : Bitmap? = null,
                 internal var friendInviteStatus : FriendInviteStatus? = null) {
    override fun toString(): String {
        return mapOf("Id" to iD, "Name" to name as Any, "Image" to image as Any,
            "FriendInviteStatus" to friendInviteStatus as Any).toString()
    }
}

enum class FriendInviteStatus(val value: Int) {
    NotSelected(0),
    AboutToBeSelected(1),
    Selected(2);
}