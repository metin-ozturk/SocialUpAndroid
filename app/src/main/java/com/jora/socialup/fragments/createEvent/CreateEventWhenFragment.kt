package com.jora.socialup.fragments.createEvent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.jora.socialup.R
import com.jora.socialup.adapters.CalendarAdapter
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.models.Event
import com.jora.socialup.viewModels.CreateEventViewModel
import kotlinx.android.synthetic.main.fragment_create_event_when.view.*


class CreateEventWhenFragment : Fragment() {

    private var viewToBeCreated : View? = null

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }

    private var eventToBePassed : Event? = null

    private var customCalendarAdapter : CalendarAdapter? = null

    private var monthMap = mapOf(0 to "January", 1 to "February", 2 to "March", 3 to "April", 4 to "May", 5 to "June",
                                            6 to "July", 7 to "August", 8 to "September", 9 to "October", 10 to "November", 11 to "December" )

    private var timePickerDialogFragment : TimePickerDialogFragment? = null

    private var initialFinalHoursMinutes : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_create_event_when, container, false)

        createEventViewModel.event.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            eventToBePassed = it
        })

        setCalendarRecyclerView()
        updateMonthTitle()
        goToNextOrPreviousMonth()
        setCalendarRecyclerViewListeners()

        // Persist Checked and Selected Dates and Times
        if (createEventViewModel.eventDateTime.value != null) {
            customCalendarAdapter?.dateTime = createEventViewModel.eventDateTime.value ?: arrayListOf()
            if (savedInstanceState != null) {
                savedInstanceState.getString("initialFinalHoursMinutes")?.also {
                    setTimePickerDialogFragment()
                    timePickerDialogFragment?.initialFinalHoursMinutes = it
                    timePickerDialogFragment?.show(fragmentManager ?: return@also, null)
                }
            }
        }


        return viewToBeCreated
    }

    override fun onPause() {
        super.onPause()

        if (timePickerDialogFragment?.isAdded == true){
            initialFinalHoursMinutes = timePickerDialogFragment?.getInitialAndFinalHoursMinutes()
            timePickerDialogFragment?.dismiss()
        } else initialFinalHoursMinutes = null


        // Persist Checked and Selected Dates and Times
        createEventViewModel.updateEventDateTime(customCalendarAdapter?.dateTime ?: arrayListOf())

        val dateToBePassed = customCalendarAdapter?.showResults() as ArrayList<String>
        eventToBePassed?.date = dateToBePassed
        eventToBePassed?.dateVote = dateToBePassed.map { "0" } as ArrayList<String> // Initialize vote as 0

        createEventViewModel.updateEventData(eventToBePassed)

    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("initialFinalHoursMinutes", initialFinalHoursMinutes)
    }

    private fun setTimePickerDialogFragment() {
        timePickerDialogFragment = TimePickerDialogFragment.newInstance(object: TimePickerDialogFragment.TimePickerFragmentInterface {
            override fun onFinishInitialTime(result: String) {
                customCalendarAdapter?.onFinishInitialTimePicker(result)
            }

            override fun onFinishFinalTime(result: String) {
                customCalendarAdapter?.onFinishFinalTimePicker(result)
                timePickerDialogFragment?.dismiss()
            }

            override fun onDialogFragmentDestroyed() {
                timePickerDialogFragment = null
            }
        })
    }


    private fun setCalendarRecyclerView() {
        val layoutManager = GridLayoutManager(activity!!, 7)
        viewToBeCreated?.createEventWhenCalendarRecyclerView?.layoutManager = layoutManager

        customCalendarAdapter = CalendarAdapter()
        viewToBeCreated?.createEventWhenCalendarRecyclerView?.adapter = customCalendarAdapter

    }

    private fun setCalendarRecyclerViewListeners() {
        viewToBeCreated?.createEventWhenCalendarRecyclerView?.addOnItemTouchListener(
            RecyclerItemClickListener(
                activity!!,
                viewToBeCreated!!.createEventWhenCalendarRecyclerView,
                object: RecyclerItemClickListener.OnItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    super.onItemClick(view, position)
                    customCalendarAdapter?.onClick(position)
                }

                override fun onLongItemClick(view: View, position: Int) {
                    super.onLongItemClick(view, position)
                    if (customCalendarAdapter?.isLongClickedDateChecked(position) == true)  {
                        setTimePickerDialogFragment()
                        timePickerDialogFragment?.show(fragmentManager ?: return, null)
                    }
                }
            }
        ))
    }


    private fun updateMonthTitle() {
        val currentMonthName = monthMap[customCalendarAdapter?.currentMonth]
        val currentYear = customCalendarAdapter?.currentYear
        viewToBeCreated?.createEventWhenMonthTitle?.text = ((currentMonthName ?: "") + ", " + currentYear)

    }

    private fun goToNextOrPreviousMonth() {
        viewToBeCreated?.apply {
            createEventWhenFutureArrow?.setOnClickListener {
                customCalendarAdapter?.updateMonthToBeShowed(true)
                updateMonthTitle()
            }
            createEventWhenPastArrow?.setOnClickListener {
                customCalendarAdapter?.updateMonthToBeShowed(false)
                updateMonthTitle()
            }
        }

    }
}