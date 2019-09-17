package com.jora.socialup.models

import com.jora.socialup.adapters.CalendarAdapter

class DateTimeInfo(var dateTimeStatus: CalendarAdapter.TimeStatus? = CalendarAdapter.TimeStatus.DEFAULT,
                   var date : String? = null,
                   var initialHourAndMinute : String? = null,
                   var finalHourAndMinute : String? = null,
                   var positionInCalendar : Int? = null,
                   var month: Int? = null) {
    override fun toString(): String {
        return "$date$initialHourAndMinute$finalHourAndMinute"
    }
}