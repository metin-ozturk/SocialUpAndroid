package com.jora.socialup.fragments.eventFeedAndDetail

import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jora.socialup.helpers.OnGestureTouchListener
import com.jora.socialup.R
import com.jora.socialup.adapters.EventDatesVoteRecyclerViewAdapter
import com.jora.socialup.helpers.ProgressBarFragmentDialog
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.models.Event
import com.jora.socialup.models.EventResponseStatus
import com.jora.socialup.viewModels.EventViewModel
import kotlinx.android.synthetic.main.fragment_event_detail.*
import java.lang.Exception

class EventDetailFragment : Fragment() {
    private val eventDetailTag = "EventDetailTag"
    private var event : Event? = null
    private var customDatesAdapter : EventDatesVoteRecyclerViewAdapter? = null

    private val viewModel : EventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(EventViewModel::class.java)
    }

    private var votedForDates = mutableMapOf<String, Boolean>()

    private var eventResponseStatus : EventResponseStatus = EventResponseStatus.NotResponded

    private val dateVotesChangedBy : ArrayList<Int> by lazy {
        val arraySize = event?.dateVote?.size ?: 0
        val arrayList = ArrayList<Int>()
        (0..arraySize).forEach { _ ->  arrayList.add(0)  }
        arrayList
    }

    private var isFavorite = false

    private val progressBarFragmentDialog: ProgressBarFragmentDialog by lazy {
        ProgressBarFragmentDialog.newInstance(object: ProgressBarFragmentDialog.ProgressBarFragmentDialogInterface {
            override fun onCancel() {
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (activity == null || context == null)  {
            Log.d(eventDetailTag, "Activity is Null")
            return null
        }

        event = ViewModelProviders.of(activity!!).get(EventViewModel::class.java).event.value

        eventResponseStatus = viewModel.eventResponseStatus.value ?: EventResponseStatus.NotResponded

        isFavorite = viewModel.isFavorite.value ?: false
        votedForDates = viewModel.votedForDates.value ?: mutableMapOf()

        val eventReceived = event ?: return null
        customDatesAdapter = EventDatesVoteRecyclerViewAdapter(eventReceived, votedForDates)

        return inflater.inflate(R.layout.fragment_event_detail, container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setCustomDatesToVoteRecyclerView()
        setCustomDatesToVoteRecyclerViewListeners()
        setTextViewsButtonsAndImages()

        view.setOnTouchListener(
            OnGestureTouchListener(
                context!!,
                object : OnGestureTouchListener.OnGestureInitiated {
                    override fun swipedLeft() {
                        swipedToLeft()
                    }
                })
        )

        setEventResponseStatusToButtons()
        setFavoriteImageView()
    }

    override fun onPause() {
        super.onPause()
        if (progressBarFragmentDialog.isLoadingInProgress) progressBarFragmentDialog.dismiss()
    }

    private fun swipedToLeft() {
        if (progressBarFragmentDialog.isLoadingInProgress) return

        progressBarFragmentDialog.show(fragmentManager ?: return, null)

        val eventID = event?.iD ?: return
        val updateEventTo = event ?: return

        viewModel.assertWhichViewToBeShowed(updateEventTo)
        viewModel.updateEventsArrayWithViewedEvent(updateEventTo)


        FirebaseFirestore.getInstance().runTransaction { transaction ->
            var whenDocumentSnapshot : DocumentSnapshot? = null

            try {
                whenDocumentSnapshot = transaction.get(FirebaseFirestore.getInstance().collection("events").document(eventID))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val readEventDateData = whenDocumentSnapshot?.data?.get("When") as ArrayList<String>?

            val readEventDate = readEventDateData?.map { eventDate ->
                eventDate.substring(0, 16)
            } as ArrayList<String>

            var readEventDateVote = readEventDateData.map {eventDateVote ->
                eventDateVote.substring(16)
            } as ArrayList<String>

            readEventDateVote = readEventDateVote.zip(dateVotesChangedBy).map { pair ->
                (pair.second + pair.first.toInt()).toString()
            } as ArrayList<String>


            val updatedReadEventDateData = readEventDate.zip(readEventDateVote).map { pair ->
                pair.first + pair.second
            } as ArrayList<String>

            transaction.update(FirebaseFirestore.getInstance().collection("events").document(eventID),
                mapOf( "When" to updatedReadEventDateData))

            val eventResponseStatusAsInt = when(eventResponseStatus) {
                EventResponseStatus.NotResponded -> 0
                EventResponseStatus.NotGoing -> 1
                EventResponseStatus.Maybe -> 2
                EventResponseStatus.Going -> 3
            }

            val eventSpecificPath = FirebaseFirestore.getInstance().collection("users").document(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                .collection("events").document(eventID)

            val toBeUpdated = mapOf("EventResponseStatus" to eventResponseStatusAsInt, "EventIsFavorite" to isFavorite) as MutableMap<String, Any>
            toBeUpdated.putAll(votedForDates)

            transaction.update(eventSpecificPath,toBeUpdated)

            val eventFragment = EventFragment()
            val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
            fragmentTransaction?.replace(R.id.homeRootFrameLayout, eventFragment)
            fragmentTransaction?.commit()

            progressBarFragmentDialog.dismiss()
        }


    }

    private fun setEventResponseStatusToButtons() {
        when (eventResponseStatus) {
            EventResponseStatus.Going -> {
                eventDetailGoingButton.setBackgroundColor(Color.GREEN)
            }
            EventResponseStatus.Maybe -> {
                eventDetailMaybeButton.setBackgroundColor(Color.YELLOW)
            }
            EventResponseStatus.NotGoing -> {
                eventDetailNotGoingButton.setBackgroundColor(Color.RED)
            }
            else -> {
                eventDetailGoingButton.setBackgroundColor(Color.LTGRAY)
                eventDetailMaybeButton.setBackgroundColor(Color.LTGRAY)
                eventDetailNotGoingButton.setBackgroundColor(Color.LTGRAY)
            }
        }

        eventDetailGoingButton.setOnClickListener {
            if (eventResponseStatus == EventResponseStatus.Going) {
                eventResponseStatus = EventResponseStatus.NotResponded
                eventDetailGoingButton.setBackgroundColor(Color.LTGRAY)
            } else {
                eventResponseStatus = EventResponseStatus.Going
                eventDetailGoingButton.setBackgroundColor(Color.GREEN)
                eventDetailMaybeButton.setBackgroundColor(Color.LTGRAY)
                eventDetailNotGoingButton.setBackgroundColor(Color.LTGRAY)
            }
            viewModel.assertEventResponseStatus(eventResponseStatus)
        }

        eventDetailMaybeButton.setOnClickListener {
            if (eventResponseStatus == EventResponseStatus.Maybe) {
                eventResponseStatus = EventResponseStatus.NotResponded
                eventDetailMaybeButton.setBackgroundColor(Color.LTGRAY)
            } else {
                eventResponseStatus = EventResponseStatus.Maybe
                eventDetailMaybeButton.setBackgroundColor(Color.YELLOW)
                eventDetailNotGoingButton.setBackgroundColor(Color.LTGRAY)
                eventDetailGoingButton.setBackgroundColor(Color.LTGRAY)
            }
            viewModel.assertEventResponseStatus(eventResponseStatus)

        }

        eventDetailNotGoingButton.setOnClickListener {
            if (eventResponseStatus == EventResponseStatus.NotGoing) {
                eventResponseStatus = EventResponseStatus.NotResponded
                eventDetailNotGoingButton.setBackgroundColor(Color.LTGRAY)
            } else {
                eventResponseStatus = EventResponseStatus.NotGoing
                eventDetailNotGoingButton.setBackgroundColor(Color.RED)
                eventDetailGoingButton.setBackgroundColor(Color.LTGRAY)
                eventDetailMaybeButton.setBackgroundColor(Color.LTGRAY)
            }
            viewModel.assertEventResponseStatus(eventResponseStatus)
        }


    }

    private fun setFavoriteImageView() {
        if (isFavorite) eventDetailFavoriteImageView.setImageResource(R.drawable.favorite_selected) else eventDetailFavoriteImageView.setImageResource(R.drawable.favorite)

        eventDetailFavoriteImageView.setOnClickListener {
            isFavorite = if (isFavorite) {
                eventDetailFavoriteImageView.setImageResource(R.drawable.favorite)
                false
            } else {
                eventDetailFavoriteImageView.setImageResource(R.drawable.favorite_selected)
                true
            }

            viewModel.assertIsFavorite(isFavorite)
        }
    }

    private fun setCustomDatesToVoteRecyclerView() {
        datesToVoteRecyclerView.adapter = customDatesAdapter

        val eventReceived = event ?: return
        customDatesAdapter?.loadData(eventReceived, votedForDates)

        val layoutManager = LinearLayoutManager(activity)
        datesToVoteRecyclerView.layoutManager = layoutManager
        datesToVoteRecyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun setCustomDatesToVoteRecyclerViewListeners() {
        datesToVoteRecyclerView.addOnItemTouchListener(
            RecyclerItemClickListener(
                context!!,
                datesToVoteRecyclerView,
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
                                dateVotesChangedBy[position] = 1
                            } else {
                                vote--
                                dateVotesChangedBy[position] = -1
                            }
                            event?.dateVote!![position] = vote.toString()

                            val eventDate = event?.date!![position]
                            votedForDates[eventDate] = it.isSelected

                            customDatesAdapter?.loadData(eventReceived, votedForDates)
                            customDatesAdapter?.notifyItemChanged(event?.dateVote!!.count() - 1)
                        }
                    }

                })
        )
    }

    private fun setTextViewsButtonsAndImages() {
        if (event?.hasImage == false) eventDetailImageView.visibility = GONE // if event hasn't an image, hide the image view

        val point = Point()
        activity?.windowManager?.defaultDisplay?.getSize(point)

        val heightOfEventDetailImageView = point.x * 9 / 16

        eventDetailImageView.layoutParams.height = heightOfEventDetailImageView

        eventDetailImageView.setImageBitmap(event?.image)
        eventDetailImageView.requestLayout()

        eventDetailDate.text = when (event?.date?.size) {
            0 -> "ERROR"
            1 -> Event.convertDateToReadableFormat(event?.date?.first()!!)
            else -> "Multiple Dates Are Proposed"
        }

        eventDetailFounder.text = event?.founderName
        eventDetailFounderImageView.setImageBitmap(event?.founderImage)
        eventDetailLocation.text = event?.locationName
        eventDetailTitle.text = event?.name
    }
}