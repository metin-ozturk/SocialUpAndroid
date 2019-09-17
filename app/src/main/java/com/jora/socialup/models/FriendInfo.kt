package com.jora.socialup.models

import android.graphics.Bitmap


class FriendInfo(internal var name : String? = null, internal var image : Bitmap? = null,
                 internal var isSelected : Boolean? = null) {
    override fun toString(): String {
        return mapOf("Name" to name as Any, "Image" to image as Any,
            "IsSelected" to isSelected as Any).toString()
    }
}