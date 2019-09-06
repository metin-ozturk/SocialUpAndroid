package com.jora.socialup.fragments.createEvent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.jora.socialup.R
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

    private var initialHour : Int? = null
    private var initialMinute : Int? = null

    private var finalHour : Int? = null
    private var finalMinute: Int? = null

    companion object {
        fun newInstance(listener: TimePickerFragmentInterface) : TimePickerDialogFragment {
            val dialogFragment = TimePickerDialogFragment()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_set_time, container, false)

        initialHour = 0
        initialMinute = 0
        finalHour = 0
        finalMinute = 0

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


            setTimeInitialHourPicker.setOnValueChangedListener { _, _, newVal ->
                initialHour = newVal
            }

            setTimeInitialMinutePicker.setOnValueChangedListener { _, _, newVal ->
                initialMinute = newVal * 5 // Convert 0..11 to 0, 5 .. 55
            }

            setTimeFinalHourPicker.setOnValueChangedListener { _, _, newVal ->
                finalHour = newVal
            }

            setTimeFinalMinutePicker.setOnValueChangedListener { _, _, newVal ->
                finalMinute = newVal * 5  // Convert 0..11 to 0, 5 .. 55
            }

            setTimeButtonDialogFragment.setOnClickListener {
                listener?.onFinishInitialTime("${twoDecimalFormat.format(initialHour)}${twoDecimalFormat.format(initialMinute)}")
                listener?.onFinishFinalTime("${twoDecimalFormat.format(finalHour)}${twoDecimalFormat.format(finalMinute)}")

            }

        }

        return viewToBeCreated
    }

}