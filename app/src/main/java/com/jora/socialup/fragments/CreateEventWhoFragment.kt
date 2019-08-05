package com.jora.socialup.fragments

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
import android.widget.ImageView
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jora.socialup.R
import com.jora.socialup.adapters.SearchFriendsRecyclerViewAdapter
import com.jora.socialup.helpers.OnSwipeTouchListener
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.models.Event
import com.jora.socialup.models.User
import com.jora.socialup.viewModels.CreateEventViewModel
import kotlinx.android.synthetic.main.fragment_create_event_who.view.*
import kotlinx.android.synthetic.main.fragment_create_event_who.view.createEventWhoRecyclerView

class FriendInfo(internal var name : String? = null, internal var image : Bitmap? = null,
                 internal var isSelected : Boolean? = null) {
    override fun toString(): String {
        return mapOf<String, Any>("Name" to name as Any, "Image" to image as Any,
                            "IsSelected" to isSelected as Any).toString()
    }
}

class CreateEventWhoFragment : Fragment() {

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }

    private var friends = ArrayList<FriendInfo>()
    private val friendIDsArrayList = ArrayList<String>()

    private var eventToBePassed : Event? = null
    private var viewToBeCreated : View? = null
    private val customSearchAdapter : SearchFriendsRecyclerViewAdapter by lazy {
        SearchFriendsRecyclerViewAdapter(friends)
    }

    private var friendsMap : MutableMap<String, FriendInfo> = mutableMapOf()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_create_event_who, container, false)
        eventToBePassed = createEventViewModel.event.value

        setSwipeGestures()
        setSearchView()
        setRecyclerView()
        downloadFriendsNamesAndImagesAndNotifyRecyclerView()
        setSearchViewListeners()
        setRecyclerViewListeners()


        return viewToBeCreated
    }


    private fun setSearchView() {
        viewToBeCreated?.apply {
            val searchManager = context?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
            val searchableInfo = searchManager.getSearchableInfo(activity?.componentName)
            createEventWhoSearchView?.setSearchableInfo(searchableInfo)

            createEventWhoSearchView?.isIconified = false
            createEventWhoSearchView?.setIconifiedByDefault(false)
            createEventWhoSearchView?.clearFocus()
        }
    }

    private fun setSearchViewListeners() {
        viewToBeCreated?.apply {
            createEventWhoSearchView.setOnClickListener {
                createEventWhoSearchView.requestFocus()
            }

            val searchCloseButtonID =
                createEventWhoSearchView.context.resources.getIdentifier("android:id/search_close_btn", null, null)
            val searchCloseButton = createEventWhoSearchView.findViewById<ImageView>(searchCloseButtonID)
            searchCloseButton.setOnClickListener {
                createEventWhoSearchView.setQuery("", true)
                createEventWhoSearchView.clearFocus()
            }

            createEventWhoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    createEventWhoSearchView.clearFocus()
                    createEventWhoSearchView.setQuery("", true)
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText?.isEmpty() == true) {
                        createEventWhoSearchView.clearFocus()
                        friends = friendsMap.map { it.value } as ArrayList<FriendInfo>
                        customSearchAdapter.dataUpdated(friends)
                        return false
                    }

                    friends = (friendsMap.map { it.value } as ArrayList<FriendInfo>).filter { retrievedFriend ->
                        val retrievedFriendName = retrievedFriend.name ?: return false
                        retrievedFriendName.contains(newText.toString())
                    } as ArrayList<FriendInfo>

                    customSearchAdapter.dataUpdated(friends)


                    return false
                }
            })
        }
    }

    private fun setRecyclerView() {
        viewToBeCreated?.apply {
            createEventWhoRecyclerView?.adapter = customSearchAdapter
            val layoutManager = LinearLayoutManager(activity!!)
            createEventWhoRecyclerView?.layoutManager = layoutManager
            createEventWhoRecyclerView?.itemAnimator = DefaultItemAnimator()
            createEventWhoRecyclerView?.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setRecyclerViewListeners() {
        viewToBeCreated?.createEventWhoRecyclerView?.addOnItemTouchListener(
            RecyclerItemClickListener(
                activity!!,
                viewToBeCreated!!.createEventWhoRecyclerView,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int) {
                        super.onItemClick(view, position)

                        view.apply {
                            isSelected = !isSelected
                            if (isSelected) setBackgroundColor(Color.GREEN) else setBackgroundColor(Color.WHITE)
                        }.also {
                            friendsMap[friendIDsArrayList[position]]?.isSelected = view.isSelected

                        }
                    }
                }
            )
        )
    }

    private fun downloadFriendsNamesAndImagesAndNotifyRecyclerView() {
        User.downloadFriendsIDs { friendIDs ->
            friendIDs.forEach {friendID ->
                User.downloadFriendsNamesAndImages(friendID) { friendName, friendImage ->
                    if (friendName == null || friendImage == null) {

                        val friend = FriendInfo("ERROR",
                            BitmapFactory.decodeResource(activity!!.resources, R.drawable.imageplaceholder),
                            false)
                        friends.add(friend)
                        friendsMap[friendID] = friend
                        friendIDsArrayList.add(friendID)

                        customSearchAdapter.notifyItemChanged(friends.size - 1)
                    } else {

                        val friend = if (eventToBePassed?.eventWithWhomID?.contains(friendID) == true) {
                            FriendInfo(friendName,
                                BitmapFactory.decodeByteArray(friendImage, 0, friendImage.size),
                                true)
                        } else {
                            FriendInfo(friendName,
                                BitmapFactory.decodeByteArray(friendImage, 0, friendImage.size),
                                false)
                        }

                        friends.add(friend)
                        friendsMap[friendID] = friend
                        friendIDsArrayList.add(friendID)

                        customSearchAdapter.notifyItemChanged(friends.size - 1)
                    }

                }
            }
        }
    }

    private fun setSwipeGestures() {
        viewToBeCreated?.createEventWhoRootConstraintLayout?.setOnTouchListener(
            OnSwipeTouchListener(activity!!,
                object: OnSwipeTouchListener.OnGestureInitiated {
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
        val eventToBePassed = eventToBePassed ?: return
        val selectedFriends = friendsMap.filter { it.value.isSelected == true }
        val selectedFriendsIDs = ArrayList(selectedFriends.keys)
        eventToBePassed.eventWithWhomID = selectedFriendsIDs
        createEventViewModel.updateEventToBeCreated(eventToBePassed)
    }
}