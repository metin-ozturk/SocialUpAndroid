package com.jora.socialup.fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import androidx.core.view.marginLeft
import androidx.core.view.marginStart
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.auth.FirebaseAuth
import com.jora.socialup.R
import com.jora.socialup.models.User
import com.jora.socialup.viewModels.EventViewModel
import com.jora.socialup.viewModels.UserProfileViewModel
import kotlinx.android.synthetic.main.fragment_dialog_user_profile.view.*


class UserProfileDialogFragment : DialogFragment() {

    interface UserProfileDialogFragmentInterface {
        fun onDialogFragmentDestroyed()
    }

    private val viewModel : UserProfileViewModel by lazy {
        ViewModelProviders.of(this).get(UserProfileViewModel::class.java)
    }

    private val signedInUserEmail : String? by lazy {
        FirebaseAuth.getInstance().currentUser?.email
    }

    private var viewToBeCreated : View? = null
    private var listener : UserProfileDialogFragmentInterface? = null

    private var user : User? = null
    private var userImage : Bitmap? = null


    companion object {
        fun newInstance(listener: UserProfileDialogFragmentInterface, user: User?, userImage: Bitmap?) : UserProfileDialogFragment {
            val dialogFragment = UserProfileDialogFragment()
            dialogFragment.listener = listener
            dialogFragment.user = user
            dialogFragment.userImage = userImage
            return dialogFragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_user_profile, container, false)

        if (user != null) {
            viewModel.user = user
            viewModel.userImage = userImage
        } else if (viewModel.user != null) {
            user = viewModel.user
            userImage = viewModel.userImage
        }

        viewToBeCreated?.userProfileDialogFragmentProfilePhotoImageView?.setImageBitmap(userImage)
        viewToBeCreated?.userProfileDialogFragmentNameTextField?.text = user?.name

        val genderImageId = if (user?.gender == "Male") R.drawable.male else R.drawable.female
        viewToBeCreated?.userProfileDialogFragmentGenderImageView?.setImageDrawable(ContextCompat.getDrawable(activity!!, genderImageId))


        viewToBeCreated?.userProfileDialogFragmentAddFriendImageView?.setOnClickListener {

        }

        if (signedInUserEmail == user?.email) viewToBeCreated?.userProfileDialogFragmentAddFriendImageView?.visibility = View.GONE

        return viewToBeCreated
    }

    override fun onResume() {
        super.onResume()

        val params = dialog?.window?.attributes
        val widthOfDialog = 400f

        params?.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthOfDialog, resources.displayMetrics).toInt()
        dialog?.window?.attributes = params as android.view.WindowManager.LayoutParams


        val widthOfProfileImage = params.width - (viewToBeCreated?.userProfileDialogFragmentProfilePhotoImageView?.marginEnd ?: 0)- (viewToBeCreated?.userProfileDialogFragmentProfilePhotoImageView?.marginStart ?: 0)
        val heightOfProfileImage = widthOfProfileImage * 9 / 16

        viewToBeCreated?.userProfileDialogFragmentProfilePhotoImageView?.layoutParams?.height = heightOfProfileImage
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.onDialogFragmentDestroyed()
    }
}