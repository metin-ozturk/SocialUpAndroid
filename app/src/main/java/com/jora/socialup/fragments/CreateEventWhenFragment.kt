package com.jora.socialup.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.jora.socialup.adapters.CalendarAdapter
import com.jora.socialup.helpers.OnSwipeTouchListener
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.models.Event
import com.jora.socialup.viewModels.CreateEventViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.android.synthetic.main.fragment_create_event_when.view.*
import java.text.DecimalFormat
import kotlin.collections.ArrayList


class CreateEventWhenFragment : Fragment() {

    private var viewToBeCreated : View? = null

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }

    private var eventToBePassed : Event? = null

    private var customCalendarAdapter : CalendarAdapter? = null

    private var monthMap = mapOf(0 to "January", 1 to "February", 2 to "March", 3 to "April", 4 to "May", 5 to "June",
                                            6 to "July", 7 to "August", 8 to "September", 9 to "October", 10 to "November", 11 to "December" )

    private var hourAndMinuteToBeSet = mutableMapOf<String, ArrayList<String>>()
    private var datesWhichAreChecked = ArrayList<String>()
    private var adapterPositions = mutableMapOf<Int, ArrayList<Int>>()


    private val initialTimePicker : TimePickerFragment by lazy {
        TimePickerFragment(true, object: TimePickerFragment.TimePickerFragmentInterface {
            override fun onFinish(result: String) {

                datesWhichAreChecked = datesWhichAreChecked.map {
                    "$it$result"
                } as ArrayList<String>

                finalTimePicker.show(activity?.supportFragmentManager, "timePicker")
                initialTimePicker.dismiss()
            }
        })
    }

    private val finalTimePicker : TimePickerFragment by lazy {
        TimePickerFragment(false, object: TimePickerFragment.TimePickerFragmentInterface {
            override fun onFinish(result: String) {

                datesWhichAreChecked = datesWhichAreChecked.map {
                    "$it$result"
                } as ArrayList<String>


                datesWhichAreChecked.forEach {
                    hourAndMinuteToBeSet[it.substring(0,8)] = arrayListOf(it.substring(8, 16))
                }

                datesWhichAreChecked = ArrayList()


                adapterPositions.forEach { positions ->
                    positions.value.forEach {position ->
                        customCalendarAdapter?.removeCheckedDays(position, positions.key)
                        customCalendarAdapter?.updateSelectedDays(position, positions.key)
                    }
                }

                adapterPositions = mutableMapOf()

                finalTimePicker.dismiss()
            }
        })
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(com.jora.socialup.R.layout.fragment_create_event_when, container, false)
        eventToBePassed = createEventViewModel.event.value

        setCalendarRecyclerView()
        updateMonthTitle()
        goToNextOrPreviousMonth()
        setCalendarRecyclerViewListeners()
        setSwipeGestures()

        return viewToBeCreated
    }

    private fun setSwipeGestures() {
        viewToBeCreated?.createEventWhenRootConstraintLayout?.setOnTouchListener(
            OnSwipeTouchListener(activity!!,
                object: OnSwipeTouchListener.OnGestureInitiated {
                    override fun swipedRight() {
                        super.swipedRight()
                        Log.d("OSMAN", hourAndMinuteToBeSet.toString())
                    }
                })
        )
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

                    val cellCountBeforeDateCellsStart = 7 + (customCalendarAdapter?.numberOfPastCellsToShow ?: 0) +
                            if (customCalendarAdapter?.initialMonth == customCalendarAdapter?.currentMonth) ((customCalendarAdapter?.currentDay ?: 0) - 1) else 0
                    val numberOfCurrentCells = customCalendarAdapter?.numberOfCurrentCellsToShow ?: 0

                    if (position < cellCountBeforeDateCellsStart ||
                        (position >= (cellCountBeforeDateCellsStart + numberOfCurrentCells))
                    ) return

                    val day = position - cellCountBeforeDateCellsStart + 1 // Position starts at 0, add 1.
                    val month = (customCalendarAdapter?.currentMonth ?: 0) + 1 // Since January is 0, not 1.
                    val year = (customCalendarAdapter?.currentYear ?: 0)
                    val twoDecimalFormat = DecimalFormat("00")

                    val dateAsString = "${twoDecimalFormat.format(day)}${twoDecimalFormat.format(month)}$year"

                    val numberOfMonthsChanged = customCalendarAdapter?.numberOfMonthsChanged ?: 0

                    if (adapterPositions[numberOfMonthsChanged] == null ) adapterPositions[numberOfMonthsChanged] = ArrayList()

                    if (adapterPositions[numberOfMonthsChanged]?.contains(position) == false) {
                        customCalendarAdapter?.updateCheckedDays(position)
                        adapterPositions[numberOfMonthsChanged]?.add(position)
                        datesWhichAreChecked.add(dateAsString)
                    } else {
                        customCalendarAdapter?.removeCheckedDays(position, numberOfMonthsChanged)
                        customCalendarAdapter?.removeSelectedDays(position)
                        datesWhichAreChecked.remove(dateAsString)
                        adapterPositions[numberOfMonthsChanged]?.remove(position)
                        hourAndMinuteToBeSet.remove(dateAsString)

                    }
                }

                override fun onLongItemClick(view: View, position: Int) {
                    super.onLongItemClick(view, position)
                    initialTimePicker.show(activity?.supportFragmentManager, "timePicker")
                }
            }
        ))
    }


    private fun updateMonthTitle() {
        val currentMonthName = monthMap[customCalendarAdapter?.currentMonth]
        viewToBeCreated?.createEventWhenMonthTitle?.text = currentMonthName ?: ""
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


    private fun getCurrentDateAndSixMonthsLater() {
        val currentDateToBeConverted = Calendar.getInstance().timeInMillis
        val defaultLocale = Locale(Locale.getDefault().language, Locale.getDefault().country)
        val currentDate = SimpleDateFormat("ddMMyyyy", defaultLocale ).format(currentDateToBeConverted)


        // Set maximum date to be six months from now on
        val getSixMonthsLaterInCalendar = Calendar.getInstance()
        getSixMonthsLaterInCalendar.add(Calendar.MONTH, 6)
        val sixMonthsLaterDate = SimpleDateFormat("ddMMyyyy", defaultLocale ).format(getSixMonthsLaterInCalendar.timeInMillis)



    }
}