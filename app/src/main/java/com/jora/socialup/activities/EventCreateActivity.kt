package com.jora.socialup.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jora.socialup.R
import com.jora.socialup.fragments.CreateEventWhatFragment

class EventCreateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        val createEventWhoFragment = CreateEventWhatFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.eventCreateFrameLayout, createEventWhoFragment)
        transaction.commit()
    }
}