package com.jora.socialup.fragments.eventFeedAndDetail

import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.widget.Toast
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
import com.jora.socialup.helpers.observeOnce
import com.jora.socialup.models.Event
import com.jora.socialup.models.EventResponseStatus
import com.jora.socialup.viewModels.EventViewModel
import kotlinx.android.synthetic.main.fragment_event_detail.view.*
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

    private var progressBarFragmentDialog: ProgressBarFragmentDialog? = null

    private var viewToBeCreated: View? = null

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

        setEventResponseStatusToButtons()
        setFavoriteImageView()
        setProgressBar()

        return viewToBeCreated
    }


    override fun onPause() {
        super.onPause()
        if (progressBarFragmentDialog?.isLoadingInProgress == true) progressBarFragmentDialog?.dismiss()
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

    private fun swipedToLeft() {
        if (progressBarFragmentDialog?.isLoadingInProgress == true) return

        progressBarFragmentDialog?.show(fragmentManager ?: return, null)

        val eventID = event?.iD ?: return

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            var whenDocumentSnapshot : DocumentSnapshot? = null

            try {
                whenDocumentSnapshot = transaction.get(FirebaseFirestore.getInstance().collection("events").document(eventID))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val readEventDateData = whenDocumentSnapshot?.data?.get("When") as ArrayList<String>?

            val updatedReadEventDateData = readEventDateData?.zip(dateVotesChangedBy)?.map { zippedPair ->
                val eventDateData = zippedPair.first

                val date = eventDateData.substring(0, 16)
                val currentVote = eventDateData.substring(16)
                val voteChangedBy = zippedPair.second

                date + (currentVote.toInt() + voteChangedBy).toString()
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

            val toBeUpdated = mapOf("EventResponseStatus" to eventResponseStatusAsInt, "EventIsFavorite" to viewModel.isFavorite) as MutableMap<String, Any>
            toBeUpdated.putAll(votedForDates)

            transaction.update(eventSpecificPath,toBeUpdated)

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