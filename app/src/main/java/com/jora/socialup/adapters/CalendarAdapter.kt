package com.jora.socialup.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue

class CalendarAdapter : RecyclerView.Adapter<BaseViewHolder>() {

    var currentMonth : Int? = null //0,1,2..11
    var initialMonth : Int? = null
    var currentYear : Int? = null //2018, 2019, 2020
    var currentDay: Int? = null // 1,2,3...31

    private var numberOfDays : Int? = null // 28, 29, 30, 31
    private var firstDayOfMonth : Int? = null // 1 = Monday, 7 = Sunday
    var numberOfMonthsChanged : Int = 0 // shouldn't exceed 6

    private var checkedDays = mutableMapOf<Int, ArrayList<Int>>()
    private var selectedDays = mutableMapOf<Int, ArrayList<Int>>()


    val numberOfPastCellsToShow : Int
        get() = firstDayOfMonth ?: 0

    val numberOfCurrentCellsToShow : Int
        get() = numberOfDays ?: 0

    private val numberOfFutureCellsToShow : Int
        get()  {
            val futureCells = 7 - ((numberOfPastCellsToShow + numberOfCurrentCellsToShow) % 7)
            return if (futureCells == 7) 0 else futureCells
        }

    private val dayTitlesCount : Int = 7
    private val dayTitles = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    private val calendarActiveUnselectedColor = Color.parseColor("#9a0000")

    internal class CalendarItemHolder(view: View) : BaseViewHolder(view) {
        internal var calendarItem = view.findViewById<TextView>(R.id.createEventWhenCalendarItem)
    }

    init {
        val currentDayMonthYear = getCurrentDayMonthYear()

        currentDay =  currentDayMonthYear[0]
        currentMonth = currentDayMonthYear[1]
        initialMonth = currentMonth
        currentYear = currentDayMonthYear[2]

        numberOfDays = getNumberOfDaysInMonth(currentYear ?: 0, currentMonth ?: 0)

        convertFirstDayOfMonthToArrayFriendlyFormat()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView =  LayoutInflater.from(parent.context).inflate(R.layout.adapter_calendar_gridlike, parent,false)
        return CalendarItemHolder(itemView)
    }

    override fun getItemCount(): Int {

        return  dayTitlesCount + numberOfPastCellsToShow + numberOfCurrentCellsToShow + numberOfFutureCellsToShow
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {

        val castedHolder = holder as CalendarItemHolder
        if (position < dayTitlesCount) {
            castedHolder.calendarItem.setBackgroundColor(Color.DKGRAY)
            castedHolder.calendarItem.text = dayTitles[position]
            castedHolder.itemView.alpha = 1f
            return
        }
        else if (position < dayTitlesCount + numberOfPastCellsToShow
                    || position > (dayTitlesCount + numberOfPastCellsToShow + numberOfCurrentCellsToShow - 1 )){
            castedHolder.calendarItem.setBackgroundColor(Color.LTGRAY)
            castedHolder.calendarItem.text = ""
            castedHolder.itemView.alpha = 1f
            return
        } else if ( (position >= dayTitlesCount + numberOfPastCellsToShow) &&
            (position < dayTitlesCount + numberOfPastCellsToShow + (currentDay ?: 0) - 1 ) &&
                initialMonth == currentMonth) {

            castedHolder.calendarItem.setBackgroundColor(Color.TRANSPARENT)
            castedHolder.itemView.setBackgroundColor(calendarActiveUnselectedColor)
            castedHolder.itemView.alpha = 0.4f
            castedHolder.calendarItem.text = (position + 1 - numberOfPastCellsToShow - dayTitlesCount).toString()
            return
        } else {
            castedHolder.calendarItem.setBackgroundColor(Color.TRANSPARENT)
            castedHolder.itemView.setBackgroundColor(calendarActiveUnselectedColor)
            castedHolder.itemView.alpha = 0.8f
            castedHolder.calendarItem.text = (position + 1 - numberOfPastCellsToShow - dayTitlesCount).toString()
        }

        if (checkedDays[numberOfMonthsChanged]?.contains(position) == true) {
            castedHolder.itemView.setBackgroundColor(Color.YELLOW)
            castedHolder.itemView.alpha = 0.8f
        } else if(selectedDays[numberOfMonthsChanged]?.contains(position) == true) {
            castedHolder.itemView.setBackgroundColor(Color.GREEN)
            castedHolder.itemView.alpha = 0.8f
        }

    }

    fun updateCheckedDays(position: Int) {

        if (checkedDays[numberOfMonthsChanged] == null )
            checkedDays[numberOfMonthsChanged] = arrayListOf(position)
        else
            checkedDays[numberOfMonthsChanged]?.add(position)

        notifyItemChanged(position)
    }

    fun updateSelectedDays(position: Int, month: Int) {
        if (selectedDays[month] == null )
            selectedDays[month] = arrayListOf(position)
        else
            selectedDays[month]?.add(position)

        notifyItemChanged(position)
    }

    fun removeCheckedDays(position: Int, month: Int) {

        checkedDays[month]?.remove(position)

        Log.d("OSMAN",checkedDays[numberOfMonthsChanged]?.toString() )
        notifyItemChanged(position)
    }

    fun removeSelectedDays(position: Int) {

        selectedDays[numberOfMonthsChanged]?.remove(position)
        Log.d("OSMAN",checkedDays[numberOfMonthsChanged]?.toString() )

        notifyItemChanged(position)

    }

    fun updateMonthToBeShowed(goToFuture: Boolean) {
        if (goToFuture) {
            if (numberOfMonthsChanged >= 6) return // don't go far than six months later than now

            if ((currentMonth ?: 0) + 1 <= 11) {
                currentMonth = (currentMonth ?: 0 ) + 1

            } else {
                currentYear = (currentYear ?: 0)+ 1
                currentMonth = 0
            }
            numberOfMonthsChanged += 1
        } else {
            if (numberOfMonthsChanged <= 0) return // Don't go back further than the actual month

            if ((currentMonth ?: 0) - 1 >= 0) {
                currentMonth = (currentMonth ?: 0) - 1
            } else {
                currentYear = (currentYear ?: 0) - 1
                currentMonth = 11
            }
            numberOfMonthsChanged -=1
        }


        numberOfDays = getNumberOfDaysInMonth(currentYear ?: 0, currentMonth ?: 0)

        convertFirstDayOfMonthToArrayFriendlyFormat()

        notifyDataSetChanged()
    }

    private fun convertFirstDayOfMonthToArrayFriendlyFormat() {
        firstDayOfMonth = getDayOfSpecificDate(1, currentMonth ?: 0, currentYear ?: 0)
        firstDayOfMonth = if (firstDayOfMonth == 1) 6 else (firstDayOfMonth ?: 0) - 2
    }

    companion object {

        fun getNumberOfDaysInMonth(currentYear: Int, currentMonth: Int): Int {
            val calendar = GregorianCalendar(currentYear, currentMonth, 1)
            return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        fun getCurrentDayMonthYear(): Array<Int> {
            val calendar = Calendar.getInstance()
            return arrayOf(
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.YEAR)
            )
        }
        fun getDayOfSpecificDate(day: Int, month: Int, year: Int): Int {
            return GregorianCalendar(year, month, day).get(Calendar.DAY_OF_WEEK)
        }
    }
}