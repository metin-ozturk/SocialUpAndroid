package com.jora.socialup.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.jora.socialup.models.User

class UserProfileViewModel : ViewModel() {
    var user : User? = null
    var userImage : Bitmap? = null
}