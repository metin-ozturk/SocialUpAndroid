package com.jora.socialup.fragments.createEvent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.jora.socialup.R
import com.jora.socialup.models.Event
import com.jora.socialup.models.FriendInviteStatus
import com.jora.socialup.viewModels.CreateEventViewModel

// Check whether selected friends from past events works correctly - it has a part in what fragment

class CreateEventWhoFragment : Fragment() {

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }

    private var eventToBePassed : Event? = null
    private var viewToBeCreated : View? = null

    private var searchFriendsFragment : SearchFriendsFragment? = null
    private var searchFriendsFragmentListener = object: SearchFriendsFragment.SearchFriendsFragmentInterface {
        override fun onFragmentDestroyed() {
            searchFriendsFragment = null
        }

        override fun updateFriendInfoIfPastEventIsLoaded() {
            super.updateFriendInfoIfPastEventIsLoaded()
            searchFriendsFragment?.updateSelectedFriends(eventToBePassed?.eventWithWhomID ?: return)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_create_event_who, container, false)
        eventToBePassed = createEventViewModel.event.value

        createEventViewModel.event.observe(viewLifecycleOwner, Observer {
            eventToBePassed = it
        })

        if (childFragmentManager.fragments.size > 0 ){
            searchFriendsFragment = (childFragmentManager.fragments.first() as SearchFriendsFragment).apply {
                listener = searchFriendsFragmentListener
            }
        } else {
            // Should be called when fragment is created for the first time
            setSearchFriendFragmentAndDownloadFriendInfo()
        }

        return viewToBeCreated
    }


    override fun onPause() {
        super.onPause()
        getSelectedFriendsIDsAndUpdateViewModel()
    }

    private fun setSearchFriendFragmentAndDownloadFriendInfo() {
        searchFriendsFragment = SearchFriendsFragment.newInstance(searchFriendsFragmentListener,
            false, eventToBePassed?.eventWithWhomID)

        val transaction = childFragmentManager.beginTransaction()
        transaction.add(R.id.createEventWhoFrameLayout, searchFriendsFragment ?: return)
        transaction.commit()

    }


    private fun getSelectedFriendsIDsAndUpdateViewModel() {
        val selectedFriends = searchFriendsFragment?.downloadedFriends?.filter { it.friendInviteStatus == FriendInviteStatus.Selected }
        selectedFriends?.also { friends ->
            eventToBePassed?.eventWithWhomID = friends.map { it.iD ?: "Error"} as ArrayList<String>
            eventToBePassed?.eventWithWhomNames = friends.map { it.name ?: "Error"} as ArrayList<String>
        }

        createEventViewModel.updateEventData(eventToBePassed)
    }
}