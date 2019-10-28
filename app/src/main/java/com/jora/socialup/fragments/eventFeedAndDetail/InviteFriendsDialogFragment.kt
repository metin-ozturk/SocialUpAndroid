package com.jora.socialup.fragments.eventFeedAndDetail

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.auth.FirebaseAuth
import com.jora.socialup.fragments.createEvent.SearchFriendsFragment
import com.jora.socialup.models.Event
import com.jora.socialup.models.FriendInfo
import com.jora.socialup.viewModels.EventViewModel
import kotlinx.android.synthetic.main.fragment_dialog_invite_friends.view.*



class InviteFriendsDialogFragment : DialogFragment() {

    interface InviteFriendsDialogFragmentInterface {
        fun onFinish(friends: ArrayList<FriendInfo>)
        fun onDialogFragmentDestroyed()
    }

    private var viewToBeCreated : View? = null
    private var listener : InviteFriendsDialogFragmentInterface? = null

    private val viewModel : EventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(EventViewModel::class.java)
    }
    private val userID : String? by lazy { FirebaseAuth.getInstance().currentUser?.uid }
    private var searchFriendsFragment : SearchFriendsFragment? = null


    companion object {
        fun newInstance(listener: InviteFriendsDialogFragmentInterface) : InviteFriendsDialogFragment {
            val dialogFragment = InviteFriendsDialogFragment()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(com.jora.socialup.R.layout.fragment_dialog_invite_friends, container, false)
        setSearchFriendsFragment()

        if (viewModel.friends == null && savedInstanceState == null) {
            searchFriendsFragment?.downloadFriendsNamesAndImagesAndNotifyRecyclerView(userID, viewModel.event.value)
        } else {
            searchFriendsFragment?.friends = viewModel.friends ?: ArrayList()
            searchFriendsFragment?.friends?.sortWith(compareBy( { it.friendInviteStatus?.value} , {it.name}))
            searchFriendsFragment?.retrieveFriendsData()
        }

        viewToBeCreated?.inviteFriendsDialogFragmentConfirmButton?.setOnClickListener {
            listener?.onFinish(searchFriendsFragment?.friends ?: return@setOnClickListener)
            dismiss()
        }

        return viewToBeCreated
    }


    override fun onResume() {
        super.onResume()

        val params = dialog?.window?.attributes
        params?.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, resources.displayMetrics).toInt()
        params?.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400f, resources.displayMetrics).toInt()
        dialog?.window?.attributes = params as android.view.WindowManager.LayoutParams

    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.onDialogFragmentDestroyed()
    }

    private fun setSearchFriendsFragment() {
        searchFriendsFragment = SearchFriendsFragment.newInstance(object: SearchFriendsFragment.SearchFriendsFragmentInterface {
            override fun onPause(friends: ArrayList<FriendInfo>) {
                viewModel.friends = friends
            }

            override fun onFragmentDestroyed() {
                searchFriendsFragment = null
            }
        }, true)

        val transaction = this.childFragmentManager.beginTransaction()
        transaction.add(com.jora.socialup.R.id.inviteFriendsDialogFragmentFrameLayout, searchFriendsFragment ?: return)
        transaction.commit()
    }

}