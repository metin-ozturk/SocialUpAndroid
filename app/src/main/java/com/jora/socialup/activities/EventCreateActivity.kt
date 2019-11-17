package com.jora.socialup.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.jora.socialup.R
import com.jora.socialup.fragments.createEvent.*
import com.jora.socialup.viewModels.CreateEventViewModel
import kotlinx.android.synthetic.main.activity_create_event.*


class EventCreateActivity : AppCompatActivity() {

    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(this).get(CreateEventViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)


        val viewPager2 = findViewById<ViewPager2>(R.id.viewPager2)
        val pagerAdapter = CreateEventFragmentAdapter(supportFragmentManager, lifecycle)
        viewPager2.adapter = pagerAdapter

        val pageNames = arrayOf("WHAT", "WHO", "WHEN", "WHERE", "SUMMARY")
        TabLayoutMediator(createEventActivityTabLayout, viewPager2, true) { tab, position ->
            tab.text = pageNames[position]
        }.attach()

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position == 3) {
                    createEventViewModel.updateIsPagingEnabled(false)
                } else {
                    createEventViewModel.updateIsPagingEnabled(true)
                }
            }
        })

        createEventViewModel.isPagingEnabled.observe(this, Observer {
            viewPager2.isUserInputEnabled = it
        })

    }

}

class CreateEventFragmentAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle)  {
    private val fragments = arrayOf(CreateEventWhatFragment(), CreateEventWhoFragment(),
        CreateEventWhenFragment(), CreateEventWhereFragment(), CreateEventSummaryFragment() )

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

}

