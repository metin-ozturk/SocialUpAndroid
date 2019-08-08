package com.jora.socialup.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jora.socialup.R
import com.jora.socialup.fragments.CreateEventWhatFragment

class EventCreateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        if (savedInstanceState == null) {
            val createEventWhatFragment = CreateEventWhatFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.eventCreateFrameLayout, createEventWhatFragment)
            transaction.commit()
        }


    }
}