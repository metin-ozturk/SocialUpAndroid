package com.jora.socialup.helpers

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.jora.socialup.R
import kotlinx.android.synthetic.main.fragment_dialog_progress_bar.view.*

const val ProgressBarFragmentTag = "ProgressBarFragmentTag"

class ProgressBarFragmentDialog : DialogFragment() {

    interface ProgressBarFragmentDialogInterface {
        fun onCancel()
        fun onDialogFragmentDestroyed()
    }

    private var viewToBeCreated : View? = null

    var listener: ProgressBarFragmentDialogInterface? = null

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
        return super.onCreateDialog(savedInstanceState).also {
            (parentFragment as? DialogFragment)?.dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_progress_bar, container, false)
        viewToBeCreated?.fragmentDialogProgressBarTextView?.text = "Waiting..."

        dialog?.apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }

        return viewToBeCreated

    }


    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onCancel()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
        isLoadingInProgressData = true
    }

    override fun dismiss() {
        super.dismiss()
        isLoadingInProgressData = false
    }

    override fun onDestroy() {
        super.onDestroy()
        (parentFragment as? DialogFragment)?.dialog?.window?.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        listener?.onDialogFragmentDestroyed()
    }



}