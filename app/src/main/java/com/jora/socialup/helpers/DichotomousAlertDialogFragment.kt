package com.jora.socialup.helpers

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.DialogFragment

const val DichotomousAlertDialogFragmentTag = "DichotomousAlertDialogFragmentTag"

class DichotomousAlertDialogFragment : DialogFragment() {

    interface DichotomousAlertDialogFragmentInterface {
        fun onDialogFragmentDestroyed() {}
        fun onYesButtonTapped() {}
        fun onNoButtonTapped() {}
    }

    var listener: DichotomousAlertDialogFragmentInterface? = null


    companion object {
        fun newInstance(dialogTitle: String, dialogText: String,
                        listener: DichotomousAlertDialogFragmentInterface) : DichotomousAlertDialogFragment {
            val dialogFragment = DichotomousAlertDialogFragment()

            dialogFragment.arguments = Bundle().apply {
                putString("dialogTitle", dialogTitle)
                putString("dialogText", dialogText)
            }

            dialogFragment.listener = listener

            return dialogFragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogTitle = arguments?.getString("dialogTitle")
        val dialogText = arguments?.getString("dialogText")
        return object: AlertDialog.Builder(context, theme) {
            override fun create(): AlertDialog {
                val alertDialog = super.create()

                (parentFragment as? DialogFragment)?.dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                return alertDialog
            }
        }.setTitle(dialogTitle)
            .setMessage(dialogText)
            .setPositiveButton("Yes") { dialog, _ ->
                listener?.onYesButtonTapped()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                listener?.onNoButtonTapped()
                dialog.dismiss()
            }
            .create()


    }


    override fun onDestroy() {
        super.onDestroy()
        (parentFragment as? DialogFragment)?.dialog?.window?.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        listener?.onDialogFragmentDestroyed()
    }
}