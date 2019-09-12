package com.jora.socialup.helpers

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.Window
import android.view.WindowManager

fun Window.isUserInteractionEnabled(enabled: Boolean) {
    if (!enabled) this.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    else this.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
}
class HelperFunctions {
    companion object {
        fun showMessage(context: Context, title: String, message: String) {
            val alertDialog = AlertDialog.Builder(context).create()
            alertDialog.apply {
                setTitle(title)
                setMessage(message)
                setButton(DialogInterface.BUTTON_NEUTRAL, "OKAY") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }

                show()
            }
    }
}

}