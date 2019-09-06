package com.jora.socialup.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.jora.socialup.R
import com.jora.socialup.viewModels.CreateEventViewModel
import java.text.DecimalFormat
import java.util.*

class CalendarAdapter : RecyclerView.Adapter<BaseViewHolder>() {

    enum class TimeStatus {
        CHECKED,
        SELECTED,
        DEFAULT
    }

    class DateTimeInfo(var dateTimeStatus: TimeStatus? = TimeStatus.DEFAULT,
                                var date : String? = null,
                                var initialHourAndMinute : String? = null,
                                var finalHourAndMinute : String? = null,
                                var positionInCalendar : Int? = null,
                                var month: Int? = null) {
        override fun toString(): String {
            return "$date$initialHourAndMinute$finalHourAndMinute"
        }
    }

    var currentMonth : Int? = null //0,1,2..11
    var initialMonth : Int? = null
    var currentYear : Int? = null //2018, 2019, 2020
    var currentDay: Int? = null // 1,2,3...31

    private var numberOfDays : Int? = null // 28, 29, 30, 31
    private var firstDayOfMonth : Int? = null // 1 = Monday, 7 = Sunday
    var numberOfMonthsChanged : Int = 0 // shouldn't exceed 6

    var dateTime = arrayListOf<DateTimeInfo>()

    private val numberOfPastCellsToShow : Int
        get() = firstDayOfMonth ?: 0

    private val numberOfCurrentCellsToShow : Int
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
            castedHolder.itemView.alpha = 1f
            castedHolder.calendarItem.text = (position + 1 - numberOfPastCellsToShow - dayTitlesCount).toString()
        }

        if (dateTime.firstOrNull { it.month == numberOfMonthsChanged && it.positionInCalendar == position}?.dateTimeStatus == TimeStatus.CHECKED ){
            castedHolder.itemView.setBackgroundColor(Color.YELLOW)
            castedHolder.itemView.alpha = 0.8f
        } else if(dateTime.firstOrNull { it.month == numberOfMonthsChanged && it.positionInCalendar == position}?.dateTimeStatus == TimeStatus.SELECTED ) {
            castedHolder.itemView.setBackgroundColor(Color.GREEN)
            castedHolder.itemView.alpha = 0.8f
        }

    }

    fun onClick(position: Int) {
        val dateAsString = getDateAsString(position) ?: return

        when(dateTime.firstOrNull { it.date == dateAsString }?.dateTimeStatus) {
            null -> {
                dateTime.add(
                    DateTimeInfo(
                        TimeStatus.CHECKED,
                        dateAsString,
                        null,
                        null,
                        position,
                        numberOfMonthsChanged
                    )
                )

                notifyItemChanged(position)

            }

            TimeStatus.DEFAULT -> {
                dateTime.first { it.date == dateAsString }.dateTimeStatus = TimeStatus.CHECKED
                notifyItemChanged(position)
            }

            else -> {
                dateTime.first { it.date == dateAsString }.dateTimeStatus = TimeStatus.DEFAULT
                notifyItemChanged(position)

            }
        }
    }

    fun isLongClickedDateChecked(position: Int) : Boolean {
        val dateAsString = getDateAsString(position) ?: return false

        return when(dateTime.firstOrNull { it.date == dateAsString }?.dateTimeStatus ) {
            TimeStatus.CHECKED -> true
            else -> false
        }

    }

    private fun getDateAsString(position: Int) : String? {
        val daysPassedInInitialMonthBeforeCurrentDay = if (initialMonth == currentMonth) (currentDay ?: 0) - 1 else 0

        val cellCountBeforeDateCellsStart = 7 + numberOfPastCellsToShow + daysPassedInInitialMonthBeforeCurrentDay

        if (position < cellCountBeforeDateCellsStart ||
            (position >= (cellCountBeforeDateCellsStart + numberOfCurrentCellsToShow))
        ) return null

        val day = position - cellCountBeforeDateCellsStart + 1  + daysPassedInInitialMonthBeforeCurrentDay // Position starts at 0, add 1.
        val month = (currentMonth ?: 0) + 1 // Since January is 0, not 1.
        val year = currentYear ?: 0
        val twoDecimalFormat = DecimalFormat("00")

        return "${twoDecimalFormat.format(day)}${twoDecimalFormat.format(month)}$year"
    }

    fun onFinishInitialTimePicker(result: String) {
        dateTime.forEach {
            if (it.dateTimeStatus == TimeStatus.CHECKED)
                it.initialHourAndMinute = result
        }
    }

    fun onFinishFinalTimePicker(result: String) {
        dateTime.forEach {
            if (it.dateTimeStatus == TimeStatus.CHECKED) {
                it.finalHourAndMinute = result
                it.dateTimeStatus = TimeStatus.SELECTED

                notifyItemChanged(it.positionInCalendar ?: 0)
            }
        }
    }

    fun showResults() : List<String> {
        return dateTime
            .filter { it.dateTimeStatus == TimeStatus.SELECTED } // Get all selected dates
            .map { "${it.date}${it.initialHourAndMinute}${it.finalHourAndMinute}" } // convert DateTimeInfo to string format - date/hourMinute/hourMinute
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