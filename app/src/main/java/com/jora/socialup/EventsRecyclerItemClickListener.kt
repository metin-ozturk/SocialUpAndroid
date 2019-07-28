package com.jora.socialup

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView

class RecyclerItemClickListener(context: Context, private val recyclerView: RecyclerView,
                                      private val listener: OnItemClickListener) : RecyclerView.OnItemTouchListener {

    private var gestureDetector : GestureDetector? = null

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
        fun onLongItemClick(view: View, position: Int)
    }

    init {
        gestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                super.onSingleTapUp(e)
                if (e == null) return false
                val child = recyclerView.findChildViewUnder(e.x , e.y)

                if (child != null) listener.onItemClick(child, recyclerView.getChildAdapterPosition(child))
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                super.onLongPress(e)
                if (e == null) return
                val child = recyclerView.findChildViewUnder(e.x, e.y)
                if (child != null) listener.onLongItemClick(child, recyclerView.getChildAdapterPosition(child))

            }
        })
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {

    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        return gestureDetector?.onTouchEvent(e) ?: false

    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    }
}