package com.jora.socialup.helpers

import android.view.Window
import android.view.WindowManager

fun Window.isUserInteractionEnabled(enabled: Boolean) {
    if (!enabled) this.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    else this.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
}
