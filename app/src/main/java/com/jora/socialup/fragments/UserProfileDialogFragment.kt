package com.jora.socialup.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.functions.FirebaseFunctions
import com.jora.socialup.R
import com.jora.socialup.helpers.DichotomousAlertDialogFragment
import com.jora.socialup.helpers.DichotomousAlertDialogFragmentTag
import com.jora.socialup.helpers.ProgressBarFragmentDialog
import com.jora.socialup.helpers.ProgressBarFragmentTag
import com.jora.socialup.models.FriendshipRequestStatus
import com.jora.socialup.models.User
import com.jora.socialup.viewModels.UserProfileViewModel
import kotlinx.android.synthetic.main.fragment_dialog_user_profile.view.*

const val UserProfileDialogFragmentTag = "UserProfileDialogFragTag"

class UserProfileDialogFragment : DialogFragment() {

    interface UserProfileDialogFragmentInterface {
        fun onFriendshipRequestSent()
        fun onDialogFragmentDestroyed()
    }

    private val viewModel: UserProfileViewModel by lazy {
        ViewModelProviders.of(this).get(UserProfileViewModel::class.java)
    }

    private val profileTag = "UserProfileDialogFrag"

    private var viewToBeCreated: View? = null
    var listener: UserProfileDialogFragmentInterface? = null

    private var user: User? = null
    private var userImage: Bitmap? = null
    private var signedInUser: User? = null
    private var friendshipRequestStatus: FriendshipRequestStatus? = null

    private var progressBarFragmentDialog: ProgressBarFragmentDialog? = null
    private val progressBarListener = object: ProgressBarFragmentDialog.ProgressBarFragmentDialogInterface {
        override fun onCancel() {
            isCancelable = true
        }

        override fun onDialogFragmentDestroyed() {
            progressBarFragmentDialog = null
        }
    }

    private var friendshipRequestAlertDialogFragment : DichotomousAlertDialogFragment? = null
    private val friendshipRequestAlertDialogFragmentListener = object :
        DichotomousAlertDialogFragment.DichotomousAlertDialogFragmentInterface {
        override fun onYesButtonTapped() {
            when (friendshipRequestStatus ) {
                FriendshipRequestStatus.ReceivedFriendshipRequest -> callFriendshipCloudFunctionByName("addFriend")
                FriendshipRequestStatus.SentFriendshipRequest -> callFriendshipCloudFunctionByName("removeFriendshipRequest")
                FriendshipRequestStatus.AlreadyFriends -> callFriendshipCloudFunctionByName("removeFriend")
                else -> return
            }
        }

        override fun onNoButtonTapped() {
            super.onNoButtonTapped()
            when (friendshipRequestStatus) {
                FriendshipRequestStatus.ReceivedFriendshipRequest -> callFriendshipCloudFunctionByName("removeFriendshipRequest")
                else -> return
            }
        }

        override fun onDialogFragmentDestroyed() {
            super.onDialogFragmentDestroyed()
            friendshipRequestAlertDialogFragment = null
        }

    }


    companion object {
        suspend fun newInstance(listener: UserProfileDialogFragmentInterface, userID: String, signedInUserID: String): UserProfileDialogFragment {
            val dialogFragment = UserProfileDialogFragment()
            dialogFragment.listener = listener

            User.downloadUserInfoForProfileViewing(userID, signedInUserID) {
                    user, userImage, signedInUser, friendshipRequestStatus ->
                dialogFragment.user = user
                dialogFragment.userImage = userImage
                dialogFragment.signedInUser = signedInUser
                dialogFragment.friendshipRequestStatus = FriendshipRequestStatus.getFriendshipRequestStatusByValue(friendshipRequestStatus)
            }

            return dialogFragment
        }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_user_profile, container, false)
        getOrSetDataViewModel()
        setUserProfileImageAndTextViews()

        if (signedInUser?.friends?.contains(user?.ID ?: return null) == true) friendshipRequestStatus = FriendshipRequestStatus.AlreadyFriends

        when {
            friendshipRequestStatus == FriendshipRequestStatus.AlreadyFriends -> {
                setAddFriendImageAndTextView(Color.GREEN, "Already Friends") {
                    setAndShowDichotomousAlertDialog("Friendship Request",
                        "Do you want to remove ${user?.name} from your friends?")
                }
            }

            signedInUser?.ID == user?.ID -> {
                viewToBeCreated?.userProfileDialogFragmentAddFriendImageView?.visibility = View.GONE
            }

            friendshipRequestStatus == FriendshipRequestStatus.ReceivedFriendshipRequest -> {
                setAddFriendImageAndTextView(Color.BLUE, "Received Friendship Request") {
                    setAndShowDichotomousAlertDialog("Friendship Request",
                        "Will you accept ${user?.name}'s friendship request?")
                }
            }

            friendshipRequestStatus == FriendshipRequestStatus.SentFriendshipRequest -> {
                setAddFriendImageAndTextView(Color.CYAN, "Sent Friendship Request") {
                    setAndShowDichotomousAlertDialog("Friendship Request",
                        "Do you want to withdraw your friendship request to ${user?.name}?")
                }
            }

            friendshipRequestStatus == FriendshipRequestStatus.NoFriendshipRequest -> {
                setAddFriendImageViewListener()
            }
        }

        return viewToBeCreated
    }

    override fun onStart() {
        super.onStart()

        val params = dialog?.window?.attributes
        val widthOfDialog = 400f

        params?.width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            widthOfDialog,
            resources.displayMetrics
        ).toInt()
        dialog?.window?.attributes = params as android.view.WindowManager.LayoutParams


        val widthOfProfileImage =
            params.width - (viewToBeCreated?.userProfileDialogFragmentProfilePhotoImageView?.marginEnd
                ?: 0) - (viewToBeCreated?.userProfileDialogFragmentProfilePhotoImageView?.marginStart
                ?: 0)
        val heightOfProfileImage = widthOfProfileImage * 9 / 16

        viewToBeCreated?.userProfileDialogFragmentProfilePhotoImageView?.layoutParams?.height =
            heightOfProfileImage


    }

    override fun onResume() {
        super.onResume()

        // Assign Listener to the previously nullified reference to Progress Bar
        childFragmentManager.findFragmentByTag(ProgressBarFragmentTag)?.also {
            progressBarFragmentDialog = it as ProgressBarFragmentDialog
            progressBarFragmentDialog?.listener = progressBarListener
        }

        childFragmentManager.findFragmentByTag(DichotomousAlertDialogFragmentTag)?.also {
            friendshipRequestAlertDialogFragment = it as DichotomousAlertDialogFragment
            friendshipRequestAlertDialogFragment?.listener = friendshipRequestAlertDialogFragmentListener
        }


    }


    override fun onDestroy() {
        super.onDestroy()
        listener?.onDialogFragmentDestroyed()
    }

    private fun getOrSetDataViewModel() {
        if (user != null) {
            viewModel.user = user
            viewModel.userImage = userImage
            viewModel.signedInUser = signedInUser
            viewModel.friendshipRequestStatus = friendshipRequestStatus
        } else {
            user = viewModel.user
            userImage = viewModel.userImage
            signedInUser = viewModel.signedInUser
            friendshipRequestStatus = viewModel.friendshipRequestStatus
        }
    }

    private fun setProgressBar() {
        progressBarFragmentDialog = ProgressBarFragmentDialog.newInstance(progressBarListener)
    }

    private fun setUserProfileImageAndTextViews() {
        viewToBeCreated?.userProfileDialogFragmentProfilePhotoImageView?.setImageBitmap(userImage)
        viewToBeCreated?.userProfileDialogFragmentNameTextField?.text = user?.name

        val genderImageId = if (user?.gender == "Male") R.drawable.male else R.drawable.female
        viewToBeCreated?.userProfileDialogFragmentGenderImageView?.setImageDrawable(
            ContextCompat.getDrawable(
                context!!,
                genderImageId
            )
        )
    }

    private fun setAddFriendImageAndTextView(imageColor: Int, text: String, imageViewClicked: () -> Unit) {
        viewToBeCreated?.userProfileDialogFragmentAddFriendImageView?.apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_person_24dp))
            setColorFilter(imageColor)
            visibility = View.VISIBLE
            setOnClickListener { imageViewClicked() }
        }
        viewToBeCreated?.userProfileDialogFragmentFriendshipRequestTextView?.apply {
            this.text = text
            visibility = View.VISIBLE
        }
    }


    private fun setAddFriendImageViewListener() {
        viewToBeCreated?.userProfileDialogFragmentAddFriendImageView?.visibility = View.VISIBLE
        viewToBeCreated?.userProfileDialogFragmentAddFriendImageView?.setOnClickListener {
            it.isClickable = false

            val data = hashMapOf(
                "userID" to (user?.ID ?: return@setOnClickListener),
                "senderName" to (signedInUser?.name ?: return@setOnClickListener),
                "senderID" to (signedInUser?.ID ?: return@setOnClickListener)
            )

            if (progressBarFragmentDialog == null) setProgressBar()
            else if (progressBarFragmentDialog?.isLoadingInProgress == true) return@setOnClickListener

            this@UserProfileDialogFragment.isCancelable = false

            progressBarFragmentDialog?.show(childFragmentManager, ProgressBarFragmentTag)


            FirebaseFunctions.getInstance().getHttpsCallable("sendFriendshipRequestByToken").call(data)
                .addOnSuccessListener {
                    listener?.onFriendshipRequestSent()
                    progressBarFragmentDialog?.dismiss()
                    this@UserProfileDialogFragment.dismiss()
                }.addOnFailureListener { exception ->
                    Log.d(
                        profileTag,
                        "Error while sending friendship request",
                        exception
                    )
                    this@UserProfileDialogFragment.isCancelable = true
                    it.isClickable = true
                }
        }
    }

    private fun setAndShowDichotomousAlertDialog(dialogTitle: String, dialogText : String) {
        friendshipRequestAlertDialogFragment = DichotomousAlertDialogFragment
            .newInstance(dialogTitle,
                dialogText,
                friendshipRequestAlertDialogFragmentListener)

        friendshipRequestAlertDialogFragment?.show(childFragmentManager, DichotomousAlertDialogFragmentTag)
    }


    private fun callFriendshipCloudFunctionByName(cloudFunctionName: String) {
        val data = hashMapOf(
            "userID" to (user?.ID ?: return),
            "senderID" to (signedInUser?.ID ?: return)
        )

        if (progressBarFragmentDialog == null) setProgressBar()
        else if (progressBarFragmentDialog?.isLoadingInProgress == true) return

        this@UserProfileDialogFragment.isCancelable = false

        progressBarFragmentDialog?.show(childFragmentManager, ProgressBarFragmentTag)

        FirebaseFunctions.getInstance().getHttpsCallable(cloudFunctionName)
            .call(data)
            .addOnSuccessListener {
                progressBarFragmentDialog?.dismiss()
                this@UserProfileDialogFragment.dismiss()
            }.addOnFailureListener {
                Log.d(profileTag, "Error While Calling $cloudFunctionName Cloud Function", it)
                this@UserProfileDialogFragment.isCancelable = true
            }
    }



}