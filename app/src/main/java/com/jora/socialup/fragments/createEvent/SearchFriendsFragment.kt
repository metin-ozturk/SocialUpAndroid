package com.jora.socialup.fragments.createEvent

import android.app.SearchManager
import android.content.Context
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
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jora.socialup.R
import com.jora.socialup.adapters.SearchFriendsRecyclerViewAdapter
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.models.Event
import com.jora.socialup.models.FriendInfo
import com.jora.socialup.models.FriendInviteStatus
import com.jora.socialup.models.User
import kotlinx.android.synthetic.main.fragment_search_friends.view.*
import java.util.*
import kotlin.collections.ArrayList

class SearchFriendsFragment : Fragment() {

    interface SearchFriendsFragmentInterface {
        fun onPause(friends: ArrayList<FriendInfo>)
        fun onFragmentDestroyed()
    }

    private var searchAfterEventCreated = false
    private var viewToBeCreated : View? = null
    var friends = ArrayList<FriendInfo>()

    var listener : SearchFriendsFragmentInterface? = null

    val customSearchAdapter : SearchFriendsRecyclerViewAdapter by lazy {
        SearchFriendsRecyclerViewAdapter(friends)
    }

    companion object {
        fun newInstance(listener: SearchFriendsFragmentInterface, searchAfterEventCreated: Boolean = false) : SearchFriendsFragment {
            val fragment = SearchFriendsFragment()
            fragment.listener = listener
            fragment.searchAfterEventCreated = searchAfterEventCreated
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_search_friends, container, false)

        setSearchView()
        setRecyclerView()
        setSearchViewListeners()
        setRecyclerViewListeners()

        return viewToBeCreated
    }


    override fun onPause() {
        super.onPause()
        listener?.onPause(friends)
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.onFragmentDestroyed()
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
                        customSearchAdapter.dataUpdated(friends)
                        return false
                    }

                    val searchedFriends = friends.filter { retrievedFriend ->
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
                            when (friends[position].friendInviteStatus) {
                                FriendInviteStatus.NotSelected -> friends[position].friendInviteStatus = FriendInviteStatus.AboutToBeSelected
                                FriendInviteStatus.AboutToBeSelected -> friends[position].friendInviteStatus = FriendInviteStatus.NotSelected
                                else -> return
                            }
                        } else {
                            when (friends[position].friendInviteStatus) {
                                FriendInviteStatus.NotSelected -> friends[position].friendInviteStatus = FriendInviteStatus.Selected
                                FriendInviteStatus.Selected -> friends[position].friendInviteStatus = FriendInviteStatus.NotSelected
                                else -> return
                            }
                        }

                        customSearchAdapter.notifyItemChanged(position)
                    }
                }
            )
        )
    }

    fun downloadFriendsNamesAndImagesAndNotifyRecyclerView(userID: String?, eventToBePassed: Event?) {
        var friend : FriendInfo

        User.downloadFriendsIDs(userID ?: return) { friendIDs ->
            if (friendIDs.isEmpty()) customSearchAdapter.updateDefaultHolderText("You don't any have friends")

            friendIDs.forEach {friendID ->
                User.downloadFriendsNamesAndImages(friendID) { friendName, friendImage ->


                    if (friendName == null || friendImage == null) {
                        friend = FriendInfo(
                            friendID,
                            "ERROR",
                            BitmapFactory.decodeResource(activity!!.resources, R.drawable.imageplaceholder),
                            FriendInviteStatus.NotSelected
                        )

                    } else {
                        friend = if (eventToBePassed?.eventWithWhomID?.contains(friendID) == true) {
                            FriendInfo(
                                friendID,
                                friendName,
                                BitmapFactory.decodeByteArray(friendImage, 0, friendImage.size),
                                FriendInviteStatus.Selected
                            )
                        } else {
                            FriendInfo(
                                friendID,
                                friendName,
                                BitmapFactory.decodeByteArray(friendImage, 0, friendImage.size),
                                FriendInviteStatus.NotSelected
                            )
                        }

                    }

                    friends.add(friend)
                    friends.sortWith(compareBy( { it.friendInviteStatus?.value} , {it.name}))
                    customSearchAdapter.notifyDataSetChanged()

                }

            }
        }
    }

    fun retrieveFriendsData() {
        if (friends.size == 0) customSearchAdapter.updateDefaultHolderText("You don't any have friends")
        customSearchAdapter.notifyDataSetChanged()
    }
}