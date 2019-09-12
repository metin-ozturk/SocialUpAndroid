package com.jora.socialup.fragments.createEvent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.jora.socialup.R
import kotlinx.android.synthetic.main.fragment_dialog_set_time.*
import kotlinx.android.synthetic.main.fragment_dialog_set_time.view.*
import java.text.DecimalFormat
import java.util.*


class TimePickerDialogFragment : DialogFragment() {

    interface TimePickerFragmentInterface {
        fun onFinishInitialTime(result: String)
        fun onFinishFinalTime(result: String)
    }

    private var viewToBeCreated : View? = null
    private var listener: TimePickerFragmentInterface? = null

    var initialFinalHoursMinutes : String? = null

    companion object {
        fun newInstance(listener: TimePickerFragmentInterface) : TimePickerDialogFragment {
            val dialogFragment = TimePickerDialogFragment()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_set_time, container, false)

        viewToBeCreated?.apply {

            setTimeTitleToDialogFragment.text = "To: "
            setTimeTitleFromDialogFragment.text = "From: "

            val twoDecimalFormat = DecimalFormat("00")

            (0..23).map { twoDecimalFormat.format(it) }.toTypedArray().also { hourValues ->
                arrayOf(setTimeInitialHourPicker, setTimeFinalHourPicker).forEach {
                    it.apply {
                        displayedValues = hourValues
                        minValue = 0
                        maxValue = 23
                    }
                }

            }


            (0..55 step 5).map { twoDecimalFormat.format(it) }.toTypedArray().also {minuteValues ->
                arrayOf(setTimeInitialMinutePicker, setTimeFinalMinutePicker).forEach {
                    it.apply {
                        displayedValues = minuteValues
                        minValue = 0
                        maxValue = 11
                    }
                }
            }

            // Update starting values of initial and final time values when orientation changes
            initialFinalHoursMinutes?.let { updateInitialAndFinalHoursMinutes(it) }


            setTimeButtonDialogFragment.setOnClickListener {
                // By multiplying with 5, we convert 0..11 to 0, 5..55
                listener?.onFinishInitialTime("${twoDecimalFormat.format(setTimeInitialHourPicker.value)}${twoDecimalFormat.format(setTimeInitialMinutePicker.value * 5)}")
                listener?.onFinishFinalTime("${twoDecimalFormat.format(setTimeFinalHourPicker.value)}${twoDecimalFormat.format(setTimeFinalMinutePicker.value * 5)}")

            }

        }

        return viewToBeCreated
    }

    private fun updateInitialAndFinalHoursMinutes(updateWith: String) {
        viewToBeCreated?.apply {
            setTimeInitialHourPicker.value = updateWith.substring(0, 2).toInt()
            setTimeInitialMinutePicker.value = updateWith.substring(2,4).toInt()
            setTimeFinalHourPicker.value = updateWith.substring(4,6).toInt()
            setTimeFinalMinutePicker.value = updateWith.substring(6,8).toInt()
        }
    }

    fun getInitialAndFinalHoursMinutes() : String {
        val twoDecimalFormat = DecimalFormat("00")
        return twoDecimalFormat.format(setTimeInitialHourPicker.value) +
                    twoDecimalFormat.format(setTimeInitialMinutePicker.value) +
                    twoDecimalFormat.format(setTimeFinalHourPicker.value) +
                    twoDecimalFormat.format(setTimeFinalMinutePicker.value)
    }

}