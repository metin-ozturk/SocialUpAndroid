package com.jora.socialup.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jora.socialup.R
import com.jora.socialup.adapters.SearchFriendsRecyclerViewAdapter
import com.jora.socialup.adapters.UserNotificationAdapter
import com.jora.socialup.helpers.ProgressBarFragmentDialog
import com.jora.socialup.helpers.ProgressBarFragmentTag
import com.jora.socialup.models.FriendshipRequestStatus
import com.jora.socialup.models.User
import com.jora.socialup.models.UserNotification
import kotlinx.android.synthetic.main.fragment_dialog_user_notification_list.view.*

const val UserNotificationListFragTag = "UserNotifListFragTag"

class UserNotificationList : DialogFragment() {
    interface UserNotificationListInterface {
        fun onDialogFragmentDestroyed()
    }

    private var viewToBeCreated: View? = null
    private var userNotifications = ArrayList<UserNotification>()
    var listener : UserNotificationListInterface? = null

    private val userNotificationAdapter : UserNotificationAdapter by lazy {
        UserNotificationAdapter(userNotifications)
    }

    private var progressBarFragmentDialog: ProgressBarFragmentDialog? = null
    private var progressBarFragmentListener : ProgressBarFragmentDialog.ProgressBarFragmentDialogInterface
            = object: ProgressBarFragmentDialog.ProgressBarFragmentDialogInterface {
        override fun onCancel() {

        }

        override fun onDialogFragmentDestroyed() {
            progressBarFragmentDialog = null
        }
    }

    companion object {
        suspend fun newInstance(listener: UserNotificationListInterface, signedInUserID: String): UserNotificationList {
            val dialogFragment = UserNotificationList()
            dialogFragment.listener = listener
            User.downloadUserNotificationInfo(signedInUserID) {
                dialogFragment.userNotifications = it
                dialogFragment.userNotificationAdapter.listener = object : UserNotificationAdapter.UserNotificationAdapterInterface {
                    override fun onStartOfCloudFun() {
                        if (dialogFragment.progressBarFragmentDialog == null) dialogFragment.setProgressBar()
                        dialogFragment.progressBarFragmentDialog?.show(dialogFragment.childFragmentManager, ProgressBarFragmentTag)
                    }

                    override fun onFinishOfCloudFun() {
                        dialogFragment.progressBarFragmentDialog?.dismiss()
                    }
                }
            }

            return dialogFragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_user_notification_list, container, false)

        setRecyclerView()

        return viewToBeCreated
    }

    override fun onStart() {
        super.onStart()

        val params = dialog?.window?.attributes
        val widthOfDialog = 400f

        params?.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthOfDialog, resources.displayMetrics).toInt()
        dialog?.window?.attributes = params as android.view.WindowManager.LayoutParams
    }

    override fun onResume() {
        super.onResume()

        // Assign Listener to the previously nullified reference to Progress Bar
        childFragmentManager.findFragmentByTag(ProgressBarFragmentTag)?.also {
            progressBarFragmentDialog = it as ProgressBarFragmentDialog
            progressBarFragmentDialog?.listener = progressBarFragmentListener
        }
    }

    override fun onPause() {
        super.onPause()

        if (progressBarFragmentDialog?.isLoadingInProgress == true) progressBarFragmentDialog?.dismiss()
    }

    private fun setRecyclerView() {
        viewToBeCreated?.userNotificationListRecyclerView?.apply {
            adapter = userNotificationAdapter
            layoutManager = LinearLayoutManager(activity!!)
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))

        }
    }

    private fun setProgressBar() {
        progressBarFragmentDialog = ProgressBarFragmentDialog.newInstance(progressBarFragmentListener)

    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.onDialogFragmentDestroyed()
    }





}