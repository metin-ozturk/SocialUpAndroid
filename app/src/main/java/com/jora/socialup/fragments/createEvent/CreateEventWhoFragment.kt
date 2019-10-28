package com.jora.socialup.fragments.createEvent

import android.app.SearchManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.jora.socialup.R
import com.jora.socialup.adapters.SearchFriendsRecyclerViewAdapter
import com.jora.socialup.helpers.OnGestureTouchListener
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.models.Event
import com.jora.socialup.models.FriendInfo
import com.jora.socialup.models.FriendInviteStatus
import com.jora.socialup.models.User
import com.jora.socialup.viewModels.CreateEventViewModel
import kotlinx.android.synthetic.main.fragment_create_event_who.view.*

// Check whether selected friends from past events works correctly - it has a part in what fragment

class CreateEventWhoFragment : Fragment() {

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }

//    private var friends = ArrayList<FriendInfo>()

    private var eventToBePassed : Event? = null
    private var viewToBeCreated : View? = null
//    private val customSearchAdapter : SearchFriendsRecyclerViewAdapter by lazy {
//        SearchFriendsRecyclerViewAdapter(friends)
//    }


    private val userID : String? by lazy {
        FirebaseAuth.getInstance().currentUser?.uid
    }

    private var searchFriendsFragment : SearchFriendsFragment? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_create_event_who, container, false)
        eventToBePassed = createEventViewModel.event.value ?: Event()


        setSearchFriendsFragment()

        // To persist data across configuration changes like orientation change

        if (savedInstanceState == null && createEventViewModel.friends.value.isNullOrEmpty()) {
            searchFriendsFragment?.downloadFriendsNamesAndImagesAndNotifyRecyclerView(userID, eventToBePassed)
        } else {
            searchFriendsFragment?.friends = createEventViewModel.friends.value ?: ArrayList()
            searchFriendsFragment?.retrieveFriendsData()
        }

        setSwipeGestures()


        return viewToBeCreated
    }


    override fun onPause() {
        super.onPause()
        createEventViewModel.apply {
            updateEventData(eventToBePassed)
            updateFriendsData(searchFriendsFragment?.friends)
        }

        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.remove(searchFriendsFragment ?: return)
        transaction?.commit()
    }

    private fun setSearchFriendsFragment() {
        searchFriendsFragment = SearchFriendsFragment.newInstance(object: SearchFriendsFragment.SearchFriendsFragmentInterface {
            override fun onPause(friends: ArrayList<FriendInfo>) {
                createEventViewModel.updateFriendsData(friends)
            }

            override fun onFragmentDestroyed() {
                searchFriendsFragment = null
            }
        })

        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.add(R.id.createEventWhoFrameLayout, searchFriendsFragment ?: return)
        transaction?.commit()
    }

    private fun setSwipeGestures() {
        viewToBeCreated?.createEventWhoRootConstraintLayout?.setOnTouchListener(
            OnGestureTouchListener(activity!!,
                object: OnGestureTouchListener.OnGestureInitiated {
                    override fun swipedLeft() {
                        super.swipedLeft()

                        getSelectedFriendsIDsAndUpdateViewModel()

                        val createEventWhatFragment = CreateEventWhatFragment()
                        val transaction = activity?.supportFragmentManager?.beginTransaction()
                        transaction?.replace(R.id.eventCreateFrameLayout, createEventWhatFragment)
                        transaction?.commit()
                    }

                    override fun swipedRight() {
                        super.swipedRight()

                        getSelectedFriendsIDsAndUpdateViewModel()

                        val createEventWhenFragment = CreateEventWhenFragment()
                        val transaction = activity?.supportFragmentManager?.beginTransaction()
                        transaction?.replace(R.id.eventCreateFrameLayout, createEventWhenFragment)
                        transaction?.commit()
                    }
                }))
    }

    private fun getSelectedFriendsIDsAndUpdateViewModel() {
        val selectedFriends = searchFriendsFragment?.friends?.filter { it.friendInviteStatus == FriendInviteStatus.Selected }
        val selectedFriendsIDs = selectedFriends?.map { it.iD }
        eventToBePassed?.eventWithWhomID = selectedFriendsIDs as? ArrayList<String>
        eventToBePassed?.eventWithWhomNames = selectedFriends?.map { it.name } as? ArrayList<String>

        createEventViewModel.updateEventData(eventToBePassed)

    }
}