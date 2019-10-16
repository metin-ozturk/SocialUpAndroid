package com.jora.socialup.helpers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.jora.socialup.R
import com.jora.socialup.viewModels.EventViewModel
import kotlinx.android.synthetic.main.fragment_favorite_events_menu.view.*


class FavoriteEventsMenu: Fragment(){

    interface FavoriteEventsMenuInterface {
        fun favoriteEventClicked()
    }

    var viewToBeCreated : View? = null

    private val viewModel : EventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(EventViewModel::class.java)
    }

    private val fadeInOutDuration = 300L
    private var listener : FavoriteEventsMenuInterface? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_favorite_events_menu, container, false)
        viewToBeCreated?.alpha = 0f
        viewToBeCreated?.startFavoriteEvent?.alpha = 0f
        viewToBeCreated?.middleFavoriteEvent?.alpha = 0f
        viewToBeCreated?.endFavoriteEvent?.alpha = 0f

        fillFavoriteEventsInformation()
        setFavoriteEventsListeners()

        return viewToBeCreated
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ObjectAnimator.ofFloat(viewToBeCreated,"alpha", 1f).apply {
            duration = fadeInOutDuration
            start()
        }
    }

    private fun fillFavoriteEventsInformation() {
        when (viewModel.favoriteEventsCount) {
            1 -> {
                viewToBeCreated?.startFavoriteEvent?.setImageBitmap(viewModel.favoriteEvents[0].image)
                viewToBeCreated?.startFavoriteEvent?.alpha = 1f
            }

            2 -> {
                viewToBeCreated?.startFavoriteEvent?.setImageBitmap(viewModel.favoriteEvents[0].image)
                viewToBeCreated?.middleFavoriteEvent?.setImageBitmap(viewModel.favoriteEvents[1].image)

                viewToBeCreated?.startFavoriteEvent?.alpha = 1f
                viewToBeCreated?.middleFavoriteEvent?.alpha = 1f
            }

            3 -> {
                viewToBeCreated?.startFavoriteEvent?.setImageBitmap(viewModel.favoriteEvents[0].image)
                viewToBeCreated?.middleFavoriteEvent?.setImageBitmap(viewModel.favoriteEvents[1].image)
                viewToBeCreated?.endFavoriteEvent?.setImageBitmap(viewModel.favoriteEvents[2].image)

                viewToBeCreated?.startFavoriteEvent?.alpha = 1f
                viewToBeCreated?.middleFavoriteEvent?.alpha = 1f
                viewToBeCreated?.endFavoriteEvent?.alpha = 1f
            }

            else -> Log.d("FavoriteEvents", "No Favorite Event Found.")

        }

    }

    private fun setFavoriteEventsListeners() {
        viewToBeCreated?.startFavoriteEvent?.setOnClickListener {
            viewModel.assertWhichViewToBeShowed(viewModel.favoriteEvents[0])
            listener?.favoriteEventClicked()
        }
        viewToBeCreated?.middleFavoriteEvent?.setOnClickListener {
            viewModel.assertWhichViewToBeShowed(viewModel.favoriteEvents[1])
            listener?.favoriteEventClicked()
        }
        viewToBeCreated?.endFavoriteEvent?.setOnClickListener {
            viewModel.assertWhichViewToBeShowed(viewModel.favoriteEvents[2])
            listener?.favoriteEventClicked()
        }
    }

    fun makeFavoriteEventsDisappear(completion: () -> Unit) {

        ObjectAnimator.ofFloat(viewToBeCreated,"alpha", 0f).apply {
            duration = fadeInOutDuration
            addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    completion()
                }
            })

            start()
        }
    }

    companion object {
        fun newInstance(listener: FavoriteEventsMenuInterface) : FavoriteEventsMenu {
            val dialogFragment = FavoriteEventsMenu()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }

}