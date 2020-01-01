package com.jora.socialup.fragments.eventFeedAndDetail

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.jora.socialup.R
import com.jora.socialup.adapters.EventDatesVoteRecyclerViewAdapter
import com.jora.socialup.helpers.OnGestureTouchListener
import com.jora.socialup.helpers.ProgressBarFragmentDialog
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.helpers.observeOnce
import com.jora.socialup.models.*
import com.jora.socialup.viewModels.EventViewModel
import kotlinx.android.synthetic.main.fragment_event_detail.view.*

class EventDetailFragment : Fragment() {
    private val eventDetailTag = "EventDetailTag"
    private var event : Event? = null
    private var customDatesAdapter : EventDatesVoteRecyclerViewAdapter? = null

    private val viewModel : EventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(EventViewModel::class.java)
    }

    private var votedForDates = mutableMapOf<String, Boolean>()

    private var eventResponseStatus : EventResponseStatus = EventResponseStatus.NotResponded

    private var progressBarFragmentDialog: ProgressBarFragmentDialog? = null

    private var finalizeEvenDateDialogFragment : FinalizeEventDateDialogFragment? = null
    private var inviteFriendsDialogFragment : InviteFriendsDialogFragment? = null
    private var getDirectionsToEventDialogFragment: GetDirectionsToEventDialogFragment? = null

    private var viewToBeCreated: View? = null
    private var isInviteFriendMenuAdded : Boolean? = null

    private val dateVotesChangedBy : ArrayList<Int> by lazy {
        val arraySize = event?.dateVote?.size ?: 0
        val arrayList = ArrayList<Int>()
        (0 until arraySize).forEach { _ ->  arrayList.add(0)  }
        arrayList
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getDataFromViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_event_detail, container,false)

        customDatesAdapter = EventDatesVoteRecyclerViewAdapter(event ?: return null, votedForDates)

        isInviteFriendMenuAdded = savedInstanceState?.getBoolean("isInviteFriendMenuAdded")

        setCustomDatesToVoteRecyclerView()
        setCustomDatesToVoteRecyclerViewListeners()
        setTextViewsButtonsAndImages()

        viewToBeCreated?.eventDetailRootLayout?.setOnTouchListener(
            OnGestureTouchListener(
                context!!,
                object : OnGestureTouchListener.OnGestureInitiated {
                    override fun swipedLeft() {
                        swipedToLeft()
                    }
                })
        )

        setFavoriteImageView()
        setDeleteEventImageView()
        setFinalizeEventDateImageView()
        setInviteFriendsImageView()
        setGetDirectionsToEventImageView()

        setEventResponseStatusToButtons()
        setProgressBar()

        return viewToBeCreated
    }


    override fun onPause() {
        super.onPause()
        if (progressBarFragmentDialog?.isLoadingInProgress == true) progressBarFragmentDialog?.dismiss()

        if (inviteFriendsDialogFragment?.isAdded == true) {
            isInviteFriendMenuAdded = true
            inviteFriendsDialogFragment?.dismiss()
        }


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isInviteFriendMenuAdded", isInviteFriendMenuAdded ?: false)
    }

    private fun getDataFromViewModel() {
        event = viewModel.event.value?.copy()
        eventResponseStatus = viewModel.eventResponseStatus.value ?: EventResponseStatus.NotResponded

        viewModel.votedForDates.observeOnce(activity!!) {
            votedForDates = it
        }

    }

    private fun setProgressBar() {
        progressBarFragmentDialog = ProgressBarFragmentDialog.newInstance(
            object: ProgressBarFragmentDialog.ProgressBarFragmentDialogInterface {
                override fun onCancel() {
                }

                override fun onDialogFragmentDestroyed() {
                    progressBarFragmentDialog = null
                }
            })
    }

    private fun setFinalizeEventDateDialogFragment() {
        finalizeEvenDateDialogFragment = FinalizeEventDateDialogFragment.newInstance(
            object: FinalizeEventDateDialogFragment.FinalizeEventDateDialogFragmentInterface {
                override fun onDateSelected(result: String) {
                    finalizeEventDate(result)
                }

                override fun onDialogFragmentDestroyed() {
                    finalizeEvenDateDialogFragment = null
                }
            }
        )
    }

    private fun swipedToLeft() {
        if (progressBarFragmentDialog?.isLoadingInProgress == true) return

        progressBarFragmentDialog?.show(fragmentManager ?: return, null)

        val eventID = event?.iD ?: return

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            var eventDocumentSnapshot : DocumentSnapshot? = null

            try {
                eventDocumentSnapshot = transaction.get(FirebaseFirestore.getInstance().collection("events").document(eventID))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val readEventDateData = eventDocumentSnapshot?.data?.get("When") as? ArrayList<String>
            val willComeList = eventDocumentSnapshot?.data?.get("WithWhomWillCome") as? ArrayList<String>
            val wontComeList = eventDocumentSnapshot?.data?.get("WithWhomWontCome") as? ArrayList<String>
            val mayComeList = eventDocumentSnapshot?.data?.get("WithWhomMayCome") as? ArrayList<String>
            val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTransaction

            val eventResponseStatusAsInt = when(eventResponseStatus) {
                EventResponseStatus.NotResponded -> {
                    arrayListOf(willComeList, wontComeList, mayComeList).forEach { it?.remove(userID) }
                    0
                }
                EventResponseStatus.NotGoing -> {
                    if (wontComeList?.contains(userID) == false) wontComeList.add(userID)
                    arrayListOf(willComeList, mayComeList).forEach { it?.remove(userID) }
                    1
                }
                EventResponseStatus.Maybe -> {
                    if (mayComeList?.contains(userID) == false) mayComeList.add(userID)
                    arrayListOf(willComeList, wontComeList).forEach { it?.remove(userID) }
                    2
                }
                EventResponseStatus.Going -> {
                    if (willComeList?.contains(userID) == false) willComeList.add(userID)
                    arrayListOf(mayComeList, wontComeList).forEach { it?.remove(userID) }
                    3
                }
            }

            val updatedReadEventDateData = readEventDateData?.zip(dateVotesChangedBy)?.map { zippedPair ->
                val eventDateData = zippedPair.first

                val date = eventDateData.substring(0, 16)
                val currentVote = eventDateData.substring(16)
                val voteChangedBy = zippedPair.second

                date + (currentVote.toInt() + voteChangedBy).toString()
            } as ArrayList<String>

            transaction.update(FirebaseFirestore.getInstance().collection("events").document(eventID),
                mapOf( "When" to updatedReadEventDateData, "FinalizedDate" to event?.finalizedDate, "EventStatus" to event?.status,
                    "WithWhomWillCome" to willComeList, "WithWhomWontCome" to wontComeList, "WithWhomMayCome" to mayComeList))


            val eventSpecificPath = FirebaseFirestore.getInstance().collection("users").document(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                .collection("events").document(eventID)

            val toBeUpdated = mapOf("EventResponseStatus" to eventResponseStatusAsInt, "EventIsFavorite" to viewModel.isFavorite) as MutableMap<String, Any>
            toBeUpdated.putAll(votedForDates)

            transaction.update(eventSpecificPath,toBeUpdated)

            //Nullify friends list so when a new event is viewed in detail, it won't show this event's friends data.
            viewModel.friends = null

            val eventFragment = EventFragment()
            val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
            fragmentTransaction?.replace(R.id.homeRootFrameLayout, eventFragment)
            fragmentTransaction?.commit()

            progressBarFragmentDialog?.dismiss()
        }


    }

    private fun setEventResponseStatusToButtons() {
        when (eventResponseStatus) {
            EventResponseStatus.Going -> {
                viewToBeCreated?.eventDetailGoingButton?.setBackgroundColor(Color.GREEN)
            }
            EventResponseStatus.Maybe -> {
                viewToBeCreated?.eventDetailMaybeButton?.setBackgroundColor(Color.YELLOW)
            }
            EventResponseStatus.NotGoing -> {
                viewToBeCreated?.eventDetailNotGoingButton?.setBackgroundColor(Color.RED)
            }
            else -> {
                viewToBeCreated?.eventDetailGoingButton?.setBackgroundColor(Color.LTGRAY)
                viewToBeCreated?.eventDetailMaybeButton?.setBackgroundColor(Color.LTGRAY)
                viewToBeCreated?.eventDetailNotGoingButton?.setBackgroundColor(Color.LTGRAY)
            }
        }

        viewToBeCreated?.eventDetailGoingButton?.setOnClickListener {
            if (eventResponseStatus == EventResponseStatus.Going) {
                eventResponseStatus = EventResponseStatus.NotResponded
                viewToBeCreated?.eventDetailGoingButton?.setBackgroundColor(Color.LTGRAY)
            } else {
                eventResponseStatus = EventResponseStatus.Going
                viewToBeCreated?.eventDetailGoingButton?.setBackgroundColor(Color.GREEN)
                viewToBeCreated?.eventDetailMaybeButton?.setBackgroundColor(Color.LTGRAY)
                viewToBeCreated?.eventDetailNotGoingButton?.setBackgroundColor(Color.LTGRAY)
            }
            viewModel.assertEventResponseStatus(eventResponseStatus)
        }

        viewToBeCreated?.eventDetailMaybeButton?.setOnClickListener {
            if (eventResponseStatus == EventResponseStatus.Maybe) {
                eventResponseStatus = EventResponseStatus.NotResponded
                viewToBeCreated?.eventDetailMaybeButton?.setBackgroundColor(Color.LTGRAY)
            } else {
                eventResponseStatus = EventResponseStatus.Maybe
                viewToBeCreated?.eventDetailMaybeButton?.setBackgroundColor(Color.YELLOW)
                viewToBeCreated?.eventDetailNotGoingButton?.setBackgroundColor(Color.LTGRAY)
                viewToBeCreated?.eventDetailGoingButton?.setBackgroundColor(Color.LTGRAY)
            }
            viewModel.assertEventResponseStatus(eventResponseStatus)

        }

        viewToBeCreated?.eventDetailNotGoingButton?.setOnClickListener {
            if (eventResponseStatus == EventResponseStatus.NotGoing) {
                eventResponseStatus = EventResponseStatus.NotResponded
                viewToBeCreated?.eventDetailNotGoingButton?.setBackgroundColor(Color.LTGRAY)
            } else {
                eventResponseStatus = EventResponseStatus.NotGoing
                viewToBeCreated?.eventDetailNotGoingButton?.setBackgroundColor(Color.RED)
                viewToBeCreated?.eventDetailGoingButton?.setBackgroundColor(Color.LTGRAY)
                viewToBeCreated?.eventDetailMaybeButton?.setBackgroundColor(Color.LTGRAY)
            }
            viewModel.assertEventResponseStatus(eventResponseStatus)
        }


    }

    private fun setFavoriteImageView() {
        if (viewModel.isFavorite) viewToBeCreated?.eventDetailFavoriteImageView?.setImageResource(R.drawable.favorite_selected) else viewToBeCreated?.eventDetailFavoriteImageView?.setImageResource(R.drawable.favorite)

        viewToBeCreated?.eventDetailFavoriteImageView?.setOnClickListener {
            viewModel.isFavorite = if (viewModel.isFavorite) {
                viewToBeCreated?.eventDetailFavoriteImageView?.setImageResource(R.drawable.favorite)
                // If favorite event is deselected then remove it from viewmodel
                viewModel.favoriteEvents = viewModel.favoriteEvents.filter { it.iD != event?.iD  } as ArrayList<Event>
                false
            } else {
                if (viewModel.favoriteEventsCount >= 3) {
                    // If user have more than three favorite events, don't allow to add a new favorite event
                    Toast.makeText(activity!!, "Cannot Have More Than 3 Favorite Events", Toast.LENGTH_SHORT).show()
                    false
                }
                else {
                    // If favorite event is select, add it to the viewmodel and show at favorite events menu
                    viewToBeCreated?.eventDetailFavoriteImageView?.setImageResource(R.drawable.favorite_selected)
                    event?.also { viewModel.favoriteEvents.add(it) }
                    true

                }
            }

        }
    }

    private fun setDeleteEventImageView() {
        viewToBeCreated?.eventDetailCancelImageView?.setOnClickListener {

            val alertDialog = AlertDialog.Builder(context)

            alertDialog.apply {
                setTitle("Delete Event")
                setMessage("Are you sure you want to delete the event?")
                setPositiveButton("YES") { dialog, _ ->
                    progressBarFragmentDialog?.show(fragmentManager ?: return@setPositiveButton, null)
                    deleteEvent()
                    dialog.dismiss()
                }
                setNegativeButton("NO") { dialog, _ ->
                    dialog.dismiss()
                    return@setNegativeButton
                }
                create()
                show()
            }

        }
    }

    private fun deleteEvent() {

        val data = hashMapOf("eventID" to (event?.iD ?: return),
            "founderID" to (event?.founderID ?: return))

        FirebaseFunctions.getInstance().getHttpsCallable("deleteEventByID").call(data)
            .addOnSuccessListener { _ ->

                val eventsArray = viewModel.eventsArray.value
                val updatedEventsArray = ArrayList<Event>()

                eventsArray?.map {
                    if (it.iD != event?.iD)
                        updatedEventsArray.add(it)
                }

                viewModel.assertEventsArray(updatedEventsArray)

                val eventFragment = EventFragment()
                val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
                fragmentTransaction?.replace(R.id.homeRootFrameLayout, eventFragment)
                fragmentTransaction?.commit()

                progressBarFragmentDialog?.dismiss()

            }
    }

    private fun setFinalizeEventDateImageView() {
        viewToBeCreated?.eventDetailFinalizeDateImageView?.setOnClickListener {
            val alertDialog = AlertDialog.Builder(context)

            alertDialog.apply {
                setTitle("Finalize Event Date")
                setMessage("Are you sure you want to finalize the event date? \n(Most voted option will be selected)")
                setPositiveButton("YES") { dialog, _ ->
                    finalizeEventDateCheckForDatesWithSameVote()
                    dialog.dismiss()
                }
                setNegativeButton("NO") { dialog, _ ->
                    dialog.dismiss()
                    return@setNegativeButton
                }
                create()
                show()
            }
        }
    }

    private fun finalizeEventDateCheckForDatesWithSameVote() {
        val eventDate = event?.date?.zip(event?.dateVote ?: return)
        var mostVoteCount = 0
        var mostVotedDay = arrayListOf<String>()

        eventDate?.map {
            val date = it.first
            val vote = it.second.toInt()

            if (vote > mostVoteCount) {
                mostVoteCount = vote
                mostVotedDay = arrayListOf(date)
            } else if( vote == mostVoteCount) {
                mostVotedDay.add(date)
            }
        }

        if (mostVotedDay.size > 1) {
            setFinalizeEventDateDialogFragment()
            finalizeEvenDateDialogFragment?.loadEventDates(mostVotedDay)
            finalizeEvenDateDialogFragment?.show(fragmentManager ?: return, null)
        } else {
            finalizeEventDate(mostVotedDay.first())
        }
    }

    private fun finalizeEventDate(finalizedDate: String) {
        event?.finalizedDate = finalizedDate
        event?.status = EventStatus.Date_Finalized.value
        customDatesAdapter?.loadData(event ?: return, null)
        finalizeEvenDateDialogFragment?.dismiss()

        updateEventAtEventsArray {
            it.finalizedDate = finalizedDate
            it.status = EventStatus.Date_Finalized.value
        }

    }

    private fun setInviteFriendsDialogFragment() {
        inviteFriendsDialogFragment = InviteFriendsDialogFragment.newInstance(
            object: InviteFriendsDialogFragment.InviteFriendsDialogFragmentInterface {
                override fun onDialogFragmentDestroyed() {
                    inviteFriendsDialogFragment = null
                }

                override fun onFinish(friends: ArrayList<FriendInfo>) {
                    progressBarFragmentDialog?.show(fragmentManager ?: return, null)
                    updateEventAtEventsArray {eventAtEventsArray ->
                        var invitedFriendCount = 0
                        val invitedFriendIDs = ArrayList<String>()
                        val invitedFriendNames = ArrayList<String>()

                        friends.forEach { friend ->
                            if (friend.friendInviteStatus != FriendInviteStatus.AboutToBeSelected) return@forEach
                            invitedFriendIDs.add(friend.iD ?: return@forEach)
                            invitedFriendNames.add(friend.name ?: return@forEach)

                            friend.friendInviteStatus = FriendInviteStatus.Selected
                            invitedFriendCount += 1
                        }


                        val userSpecificEventData = mutableMapOf("EventResponseStatus" to 0,
                            "EventIsFavorite" to false)

                        eventAtEventsArray.date?.forEach { userSpecificEventData[it] = false }

                        FirebaseFirestore.getInstance().runBatch {batch ->
                            invitedFriendIDs.forEach {friendID ->
                                batch.set(FirebaseFirestore.getInstance().collection("users")
                                    .document(friendID).collection("events").document(eventAtEventsArray.iD ?: return@forEach), userSpecificEventData)
                            }

                             val allInvitedFriends = friends.filter { it.friendInviteStatus == FriendInviteStatus.Selected }

                            batch.update(FirebaseFirestore.getInstance().collection("events")
                                .document(eventAtEventsArray.iD ?: return@runBatch), mapOf("WithWhomInvited" to
                                    allInvitedFriends.map { it.iD }, "WithWhomInvitedNames" to allInvitedFriends.map { it.name }))
                        }.addOnCompleteListener {
                            if (it.exception != null) {
                                Log.d(eventDetailTag, "ERROR While Inviting friends", it.exception)
                                return@addOnCompleteListener
                            }

                            eventAtEventsArray.eventWithWhomID?.addAll(invitedFriendIDs)
                            eventAtEventsArray.eventWithWhomNames?.addAll(invitedFriendNames)
                            progressBarFragmentDialog?.dismiss()
                        }
                    }
                }
            })
    }

    private fun setInviteFriendsImageView() {
        viewToBeCreated?.eventDetailInviteImageView?.setOnClickListener {
            if (inviteFriendsDialogFragment == null) setInviteFriendsDialogFragment()
            inviteFriendsDialogFragment?.show(fragmentManager ?: return@setOnClickListener, null)
        }

        if (isInviteFriendMenuAdded == true) {
            setInviteFriendsDialogFragment()
            inviteFriendsDialogFragment?.show(fragmentManager ?: return, null)
            isInviteFriendMenuAdded = false
        }
    }

    private fun setGetDirectionsToEventDialogFragment() {
        getDirectionsToEventDialogFragment = GetDirectionsToEventDialogFragment.newInstance(object: GetDirectionsToEventDialogFragment.GetDirectionsToEventDialogFragmentInterface {
            override fun onDialogFragmentDestroyed() {
                getDirectionsToEventDialogFragment = null
            }
        })
    }

    private fun setGetDirectionsToEventImageView() {
        viewToBeCreated?.eventDetailLandmarkImageView?.setOnClickListener {
            setGetDirectionsToEventDialogFragment()
            getDirectionsToEventDialogFragment?.show(fragmentManager ?: return@setOnClickListener, null)
        }
    }

    private fun updateEventAtEventsArray(updateTo: (Event) -> Unit) {
        val eventsArray = viewModel.eventsArray.value
        eventsArray?.first { event?.iD == it.iD }?.apply {
            updateTo(this)
        }
    }

    private fun setCustomDatesToVoteRecyclerView() {
        viewToBeCreated?.datesToVoteRecyclerView?.adapter = customDatesAdapter

        val eventReceived = event ?: return
        customDatesAdapter?.loadData(eventReceived, votedForDates)

        val layoutManager = LinearLayoutManager(activity)
        viewToBeCreated?.datesToVoteRecyclerView?.layoutManager = layoutManager
        viewToBeCreated?.datesToVoteRecyclerView?.itemAnimator = DefaultItemAnimator()
    }

    private fun setCustomDatesToVoteRecyclerViewListeners() {
        viewToBeCreated?.datesToVoteRecyclerView?.addOnItemTouchListener(
            RecyclerItemClickListener(
                context!!,
                viewToBeCreated?.datesToVoteRecyclerView ?: return,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int) {
                        val eventReceived = event ?: return

                        view.apply {
                            isSelected = !isSelected
                            if (isSelected) setBackgroundColor(Color.GREEN) else setBackgroundColor(Color.WHITE)
                        }.also {
                            var vote = event?.dateVote?.get(position)?.toInt() ?: return
                            if (it.isSelected) {
                                vote++
                                dateVotesChangedBy[position] += 1
                            } else {
                                vote--
                                dateVotesChangedBy[position] -= 1
                            }

                            event?.dateVote!![position] = vote.toString()

                            val date = event?.date?.get(position) ?: return
                            votedForDates[date] = votedForDates[date] != true

                            customDatesAdapter?.loadData(eventReceived, votedForDates)
                            customDatesAdapter?.notifyItemChanged(event?.dateVote!!.count() - 1)
                        }
                    }

                })
        )
    }

    private fun setTextViewsButtonsAndImages() {
        if (event?.hasImage == false) viewToBeCreated?.eventDetailImageView?.visibility = GONE // if event hasn't an image, hide the image view

        if (event?.founderID != FirebaseAuth.getInstance().currentUser?.uid) {
            viewToBeCreated?.apply {
                eventDetailCancelImageView?.visibility = GONE
                eventDetailFinalizeDateImageView?.visibility = GONE
            }

        }


        val point = Point()
        activity?.windowManager?.defaultDisplay?.getSize(point)

        val heightOfEventDetailImageView = point.x * 9 / 16

        viewToBeCreated?.apply {
            eventDetailImageView?.layoutParams?.height = heightOfEventDetailImageView

            eventDetailImageView?.setImageBitmap(event?.image)
            eventDetailImageView?.requestLayout()

            eventDetailFounder?.text = event?.founderName
            eventDetailFounderImageView?.setImageBitmap(event?.founderImage)
            eventDetailLocation?.text = event?.locationName
            eventDetailTitle?.text = event?.name
        }


    }
}