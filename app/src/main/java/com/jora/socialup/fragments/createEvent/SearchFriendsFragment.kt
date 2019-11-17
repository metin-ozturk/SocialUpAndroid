package com.jora.socialup.fragments.createEvent

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
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
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.models.FriendInfo
import com.jora.socialup.models.FriendInviteStatus
import com.jora.socialup.viewModels.FriendInfoViewModel
import kotlinx.android.synthetic.main.fragment_search_friends.view.*
import java.util.*
import kotlin.collections.ArrayList

class SearchFriendsFragment : Fragment() {

    interface SearchFriendsFragmentInterface {
        fun onPause(friends: ArrayList<FriendInfo>) {}
        fun updateFriendInfoIfPastEventIsLoaded() {}
        fun onFragmentDestroyed()
    }

    private val friendInfoViewModel : FriendInfoViewModel by lazy {
        ViewModelProviders.of(this).get(FriendInfoViewModel::class.java)
    }

    private var invitedPersonsIDs = arrayListOf<String>()
    private var searchAfterEventCreated = false
    private var viewToBeCreated : View? = null

    var downloadedFriends = ArrayList<FriendInfo>()

    var listener : SearchFriendsFragmentInterface? = null

    private val userID : String? by lazy {
        FirebaseAuth.getInstance().currentUser?.uid
    }


    val customSearchAdapter : SearchFriendsRecyclerViewAdapter by lazy {
        SearchFriendsRecyclerViewAdapter(downloadedFriends)
    }

    companion object {
        fun newInstance(listener: SearchFriendsFragmentInterface, searchAfterEventCreated: Boolean = false,
                        invitedPersonsIDs : ArrayList<String>?) : SearchFriendsFragment {
            val fragment = SearchFriendsFragment()
            fragment.listener = listener
            fragment.searchAfterEventCreated = searchAfterEventCreated
            fragment.invitedPersonsIDs = invitedPersonsIDs ?: ArrayList()
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_search_friends, container, false)

        friendInfoViewModel.friends.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            downloadedFriends = it
            customSearchAdapter.dataUpdated(downloadedFriends)
        })

        friendInfoViewModel.downloadFriendInfo(userID, invitedPersonsIDs)

        setSearchView()
        setRecyclerView()
        setSearchViewListeners()
        setRecyclerViewListeners()

        return viewToBeCreated
    }

    override fun onResume() {
        super.onResume()

        if (friendInfoViewModel.isFriendInfoDownloadCompleted.value == true) {
            listener?.updateFriendInfoIfPastEventIsLoaded()
        }
    }

    override fun onPause() {
        super.onPause()
        listener?.onPause(downloadedFriends)
    }


    override fun onDestroy() {
        super.onDestroy()
        listener?.onFragmentDestroyed()
    }

    fun updateSelectedFriends(selectedFriendsIDs: ArrayList<String>) {
        val updatedFriendInfo = ArrayList<FriendInfo>()

        friendInfoViewModel.friends.value?.forEach {
            if (selectedFriendsIDs.contains(it.iD)) it.friendInviteStatus = FriendInviteStatus.Selected
            else it.friendInviteStatus = FriendInviteStatus.NotSelected
            updatedFriendInfo.add(it)
        }

        friendInfoViewModel.updateFriendInfo(updatedFriendInfo)

        customSearchAdapter.dataUpdated(updatedFriendInfo)
    }

    private fun setSearchView() {
        viewToBeCreated?.searchFriendsSearchView?.apply {
            val searchManager = context?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
            val searchableInfo = searchManager.getSearchableInfo(activity?.componentName)
            queryHint = "Search Friends..."
            setSearchableInfo(searchableInfo)

            isIconified = false
            isIconifiedByDefault = false
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            clearFocus()
        }
    }

    private fun setSearchViewListeners() {
        viewToBeCreated?.apply {
            searchFriendsSearchView.setOnClickListener {
                it.requestFocus()
            }

            val searchCloseButtonID =
                searchFriendsSearchView.context.resources.getIdentifier("android:id/search_close_btn", null, null)
            val searchCloseButton = searchFriendsSearchView.findViewById<ImageView>(searchCloseButtonID)

            searchCloseButton.setOnClickListener {
                searchFriendsSearchView.setQuery("", true)
                searchFriendsSearchView.clearFocus()
            }

            searchFriendsSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchFriendsSearchView.clearFocus()
                    searchFriendsSearchView.setQuery("", true)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText?.isEmpty() == true) {
                        searchFriendsSearchView.clearFocus()
                        customSearchAdapter.dataUpdated(downloadedFriends)
                        return false
                    }

                    val searchedFriends = downloadedFriends.filter { retrievedFriend ->
                        val retrievedFriendName = retrievedFriend.name ?: return false
                        retrievedFriendName.toLowerCase(Locale("tr", "TR")).contains(newText?.toLowerCase(Locale("tr", "TR")).toString())
                    } as ArrayList<FriendInfo>

                    customSearchAdapter.dataUpdated(searchedFriends)

                    return false
                }
            })
        }
    }

    private fun setRecyclerView() {
        viewToBeCreated?.searchFriendsWhoRecyclerView?.apply {
            adapter = customSearchAdapter
            layoutManager = LinearLayoutManager(activity!!)
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setRecyclerViewListeners() {
        viewToBeCreated?.searchFriendsWhoRecyclerView?.addOnItemTouchListener(
            RecyclerItemClickListener(
                activity!!,
                viewToBeCreated!!.searchFriendsWhoRecyclerView,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int) {
                        super.onItemClick(view, position)

                        if (searchAfterEventCreated) {
                            when (downloadedFriends[position].friendInviteStatus) {
                                FriendInviteStatus.NotSelected -> downloadedFriends[position].friendInviteStatus = FriendInviteStatus.AboutToBeSelected
                                FriendInviteStatus.AboutToBeSelected -> downloadedFriends[position].friendInviteStatus = FriendInviteStatus.NotSelected
                                else -> return
                            }
                        } else {
                            when (downloadedFriends[position].friendInviteStatus) {
                                FriendInviteStatus.NotSelected -> downloadedFriends[position].friendInviteStatus = FriendInviteStatus.Selected
                                FriendInviteStatus.Selected -> downloadedFriends[position].friendInviteStatus = FriendInviteStatus.NotSelected
                                else -> return
                            }
                        }

                        customSearchAdapter.dataUpdated(downloadedFriends)
                    }
                }
            )
        )
    }


}