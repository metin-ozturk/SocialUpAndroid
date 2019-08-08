package com.jora.socialup.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import com.jora.socialup.fragments.eventFeedAndDetail.EventFragment
import com.jora.socialup.R
import kotlin.collections.ArrayList

// When activities are changes, it is downloading events again - FIX

class ObservableArrayList<Element>(private val observer : () -> Unit):ArrayList<Element>() {

    //    override fun add(element: Element): Boolean = super.add(element).also { if (it) observer() }
    override fun add(element: Element): Boolean {
        return super.add(element).also { if (it) observer() }
    }
    // etc, override the other method similarly

}

class ObservableList<Element>(private val base: MutableList<Element>, private val observer : () -> Unit): MutableList<Element> by base {
    override fun add(element: Element): Boolean = base.add(element).also {
        if (it) observer()
    }
}

class MainActivity : AppCompatActivity() {
    private val tag = "MainActivityLog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (savedInstanceState == null) {
            val eventFragment = EventFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.homeRootFrameLayout, eventFragment)
            transaction.commit()
        }

    }



}