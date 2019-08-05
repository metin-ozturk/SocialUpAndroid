package com.jora.socialup.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.jora.socialup.R
import kotlinx.android.synthetic.main.fragment_dialog_set_time.view.*
import java.text.DecimalFormat
import java.util.*


class TimePickerFragment(private val isInitial: Boolean, private val listener: TimePickerFragmentInterface) : DialogFragment() {

    interface TimePickerFragmentInterface {
        fun onFinish(result: String)
    }

    private var hourOfDay : Int? = null
    private var minute : Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dialog_set_time, container, false)

        val calendar = Calendar.getInstance()
        hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        minute = calendar.get(Calendar.MINUTE)

        view.apply {
            setTimeTimePickerDialogFragment.setIs24HourView(true)

            setTimeButtonDialogFragment.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimaryDark))
            setTimeTitleDialogFragment.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimaryDark))
            setTimeTitleDialogFragment.text = if (isInitial) "FROM:" else "TO:"

            setTimeTimePickerDialogFragment.setOnTimeChangedListener { _, hourOfDayRetrieved, minuteRetrieved ->
                hourOfDay = hourOfDayRetrieved
                minute = minuteRetrieved
            }

            setTimeButtonDialogFragment.setOnClickListener {
                val twoDecimalFormat = DecimalFormat("00")
                listener.onFinish("${twoDecimalFormat.format(hourOfDay)}${twoDecimalFormat.format(minute)}")

            }

        }

        return view
    }

}