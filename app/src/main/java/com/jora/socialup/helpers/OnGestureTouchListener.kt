package com.jora.socialup.helpers

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.abs

class OnGestureTouchListener(context: Context, private val listener: OnGestureInitiated) : OnTouchListener {
    private var gestureDetector: GestureDetector? = null

    companion object {

        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    interface OnGestureInitiated {
        fun swipedRight() {}
        fun swipedLeft() {}
        fun swipedDown() {}
        fun swipedUp() {}
        fun singleTappedConfirmed() {}
        fun longPressed() {}
    }


    init {
        gestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                if (e == null) return false
                listener.singleTappedConfirmed()
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                super.onLongPress(e)
                if (e == null) return
                listener.longPressed()
            }

            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                var result = false
                try {
                    val diffY = (e2?.y ?: 0f) - (e1?.y ?: 0f)
                    val diffX = (e2?.x ?: 0f) - (e1?.x ?: 0f)
                    if (abs(diffX) > abs(diffY)) {
                        if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                listener.swipedRight()
                            } else {
                                listener.swipedLeft()
                            }

                            result = true
                        }
                    } else if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            listener.swipedDown()
                        } else {
                            listener.swipedUp()
                        }
                        result = true
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }

                return result
            }
        })
    }


    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector?.onTouchEvent(event) ?: false
    }
}