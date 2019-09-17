package com.jora.socialup.helpers

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.jora.socialup.R
import kotlinx.android.synthetic.main.fragment_dialog_progress_bar.view.*

class ProgressBarFragmentDialog : DialogFragment() {

    interface ProgressBarFragmentDialogInterface {
        fun onCancel()
    }
    private var viewToBeCreated : View? = null

    private var listener: ProgressBarFragmentDialogInterface? = null

    private var isLoadingInProgressData = false
    val isLoadingInProgress : Boolean
            get() = isLoadingInProgressData

    companion object {
        fun newInstance(listener: ProgressBarFragmentDialogInterface) : ProgressBarFragmentDialog {
            val dialogFragment = ProgressBarFragmentDialog()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return object : Dialog(activity!!, theme) {
            override fun onTouchEvent(event: MotionEvent): Boolean {

                if (MotionEvent.ACTION_OUTSIDE == event.action) {
                    setVoluntaryCancelAlertDialog()
                    return true
                }

                return super.onTouchEvent(event)
            }
        }.apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)

            window?.apply {
                setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            }
        }
        
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_progress_bar, container, false)
        viewToBeCreated?.fragmentDialogProgressBarTextView?.text = "Waiting..."

        viewToBeCreated?.fragmentDialogProgressBarRootConstraintLayout?.alpha = 0.6f

        return viewToBeCreated
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
        isLoadingInProgressData = true
    }

    override fun dismiss() {
        super.dismiss()
        isLoadingInProgressData = false

    }

    private fun setVoluntaryCancelAlertDialog() {
        val alertDialog = AlertDialog.Builder(activity!!).create()
        alertDialog.apply {
            setTitle("Warning")
            setMessage("Are you sure you want to stop waiting and cancel the process?")
            setButton(DialogInterface.BUTTON_POSITIVE, "YES") { dialogInterface, _ ->
                listener?.onCancel()
                dialogInterface.dismiss()
                this@ProgressBarFragmentDialog.dismiss()
            }

            setButton(DialogInterface.BUTTON_NEGATIVE, "NO") { dialogInterface, _ ->
                dialogInterface.dismiss()

            }

            show()
        }

    }

    fun setCancelAlertDialog() {
        val alertDialog = AlertDialog.Builder(activity!!).create()
        alertDialog.apply {
            setTitle("Warning")
            setMessage("An error occurred. Process is halted.")
            setButton(DialogInterface.BUTTON_NEUTRAL, "OKAY") { dialogInterface, _ ->
                listener?.onCancel()
                dialogInterface.dismiss()
                this@ProgressBarFragmentDialog.dismiss()
            }

            show()
        }
    }



}