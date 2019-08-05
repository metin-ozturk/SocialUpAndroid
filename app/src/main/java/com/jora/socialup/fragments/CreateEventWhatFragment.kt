package com.jora.socialup.fragments

import android.Manifest.permission.CAMERA
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.jora.socialup.R
import com.jora.socialup.activities.EventCreateActivity
import com.jora.socialup.activities.MainActivity
import com.jora.socialup.adapters.EventHistoryRecyclerViewAdapter
import com.jora.socialup.helpers.OnSwipeTouchListener
import com.jora.socialup.helpers.RecyclerItemClickListener
import com.jora.socialup.models.Event
import com.jora.socialup.viewModels.CreateEventViewModel
import com.squareup.haha.perflib.Main
import kotlinx.android.synthetic.main.fragment_create_event_what.*
import kotlinx.android.synthetic.main.fragment_create_event_what.view.*


private const val MY_PERMISSION_REQUEST_CAMERA = 2

class CreateEventWhatFragment : Fragment() {

    private var historyEventsIDs = ArrayList<String>()
    private var historyEvents = ArrayList<Event>()

    private var customHistoryAdapter = EventHistoryRecyclerViewAdapter()
    private val createEventViewModel : CreateEventViewModel by lazy {
        ViewModelProviders.of(activity!!).get(CreateEventViewModel::class.java)
    }
    private var viewToBeCreated : View? = null
    private var eventToBePassed : Event? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        viewToBeCreated = inflater.inflate(R.layout.fragment_create_event_what, container, false)
        reloadViewModelDataToViews()

        // Download events to show in the history. If they were downloaded before, reload them.
        if (createEventViewModel.eventHistory.value == null) downloadLastFiveEventsCreatedByUser()
        else customHistoryAdapter.loadData(historyEvents)


        setCreateEventWhatImageView()
        setIsPublicOrPrivate()
        setGestureRecognizerToCreateEventImageView() // Also initialize pick and take photo here
        setHistoryRecyclerView()
        setHistoryImageViewListener()
        setHistoryRecyclerViewListener()
        setSwipeRightGesture()


        return viewToBeCreated
    }


    override fun onPause() {
        super.onPause()

        createEventToBeTransferredAndUpdateViewModel()
        if (historyEvents.isNotEmpty()) {
            createEventViewModel.updateEventHistory(historyEvents)
        }
    }

    private fun setSwipeRightGesture() {
        viewToBeCreated?.createEventWhatRootConstraintLayout?.setOnTouchListener(OnSwipeTouchListener(activity!!,
            object: OnSwipeTouchListener.OnGestureInitiated {
                override fun swipedRight() {
                    super.swipedRight()
                    goToWhoFragment()
                }

                override fun swipedLeft() {
                    super.swipedLeft()
                    startActivity(Intent(activity!!, MainActivity::class.java))
                }
            }))
    }


    private fun reloadViewModelDataToViews() {
        if (createEventViewModel.event.value != null) {
            val retrievedEvent = createEventViewModel.event.value ?: return
            viewToBeCreated?.apply {
                createEventWhatName.text.insert(0, retrievedEvent.name)
                createEventWhatDescription.text.insert(0, retrievedEvent.description)
                createEventWhatImageView.setImageBitmap(retrievedEvent.image)
                createEventWhatIsPublic.isChecked = retrievedEvent.isPrivate ?: false
                createEventWhatIsPublic.text = if (createEventWhatIsPublic.isChecked) "Private" else "Public"
            }
        }

        if (createEventViewModel.eventHistory.value != null) {
            historyEvents = createEventViewModel.eventHistory.value ?: return
        }

    }

    private fun createEventToBeTransferredAndUpdateViewModel() {
        val eventToBeTransferred = eventToBePassed ?: Event()

        eventToBeTransferred.name = createEventWhatName.text.toString()
        eventToBeTransferred.description = createEventWhatDescription.text.toString()
        eventToBeTransferred.image = createEventWhatImageView.drawable.toBitmap()
        eventToBeTransferred.isPrivate = createEventWhatIsPublic.isChecked

        createEventViewModel.updateEventToBeCreated(eventToBeTransferred)

    }

    private fun goToWhoFragment() {
        val createEventWhoFragment = CreateEventWhoFragment()
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.eventCreateFrameLayout, createEventWhoFragment)
        transaction?.commit()
    }



    private fun downloadLastFiveEventsCreatedByUser() {
        FirebaseFirestore.getInstance().collection("users").document("MKbCN5M1gnZ9Yi427rPf2SzyvqM2")
            .collection("events").get().addOnSuccessListener { snap ->
                historyEventsIDs = ArrayList((snap.documents.map { it.id }).subList(0,5))
                historyEventsIDs.forEach { docsID ->
                    Event.downloadEventInformation(docsID) {event ->
                        historyEvents.add(event)
                        customHistoryAdapter.loadData(historyEvents)
                    }
                }
            }

    }

    private fun setHistoryImageViewListener() {

        viewToBeCreated?.apply {
            createEventWhatHistoryImageView.setOnClickListener {
                if (createEventWhatHistoryRecyclerView.visibility == GONE) {
                    createEventWhatHistoryRecyclerView.visibility = VISIBLE
                    createEventWhatHistoryRecyclerView.animate().alpha(1f).setDuration(500L).setListener(null)
                } else {
                    createEventWhatHistoryRecyclerView.animate().alpha(0f).setDuration(500L).setListener(null)
                    Handler().postDelayed({
                        createEventWhatHistoryRecyclerView.visibility = GONE
                    }, 500)
                }
            }
        }
    }

    private fun setHistoryRecyclerView() {
        viewToBeCreated?.apply {
            createEventWhatHistoryRecyclerView.adapter = customHistoryAdapter
            val layoutManager = LinearLayoutManager(activity)
            createEventWhatHistoryRecyclerView.layoutManager = layoutManager
            createEventWhatHistoryRecyclerView.itemAnimator = DefaultItemAnimator()
            createEventWhatHistoryRecyclerView.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setHistoryRecyclerViewListener() {
        viewToBeCreated?.apply {
            createEventWhatHistoryRecyclerView.addOnItemTouchListener(
                RecyclerItemClickListener(activity!!,
                    createEventWhatHistoryRecyclerView,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int) {
                            if (historyEvents.size == 0) return

                            val clickedEvent = historyEvents[position]
                            eventToBePassed = clickedEvent

                            if (clickedEvent.image == null) createEventWhatImageView.setImageResource(R.drawable.imageplaceholder) else createEventWhatImageView.setImageBitmap(
                                clickedEvent.image
                            )

                            createEventWhatIsPublic.isChecked = clickedEvent.isPrivate ?: false
                            createEventWhatName.text.clear()
                            createEventWhatName.text.insert(0, clickedEvent.name)
                            createEventWhatDescription.text.clear()
                            createEventWhatDescription.text.insert(0, clickedEvent.description)
                        }

                    })
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            0 -> { if (resultCode == RESULT_OK) {
                    val selectedImage : Bitmap = data?.extras?.get("data") as Bitmap
                    createEventWhatImageView.setImageBitmap(selectedImage)
                }
            }
            1 -> { if (resultCode == RESULT_OK) {
                    val selectedImage = data?.data
                    createEventWhatImageView.setImageURI(selectedImage)
                }
            }
        }
    }

    private fun setGestureRecognizerToCreateEventImageView() {
        viewToBeCreated?.createEventWhatImageView?.setOnTouchListener(
            OnSwipeTouchListener(context!!, object : OnSwipeTouchListener.OnGestureInitiated {
                override fun singleTappedConfirmed() {
                    super.singleTappedConfirmed()

                    val pickPhotoIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(pickPhotoIntent, 1)

                }

                override fun longPressed() {
                    super.longPressed()
                    checkCameraPermission {
                        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(takePhotoIntent, 0)
                    }
                }

                override fun swipedRight() {
                    super.swipedRight()
                    goToWhoFragment()
                }
            }))
    }

    private fun checkCameraPermission(completion: () -> Unit) {
        if (checkSelfPermission(activity!!, CAMERA) != PackageManager.PERMISSION_GRANTED) {
            completion()
        } else {
            ActivityCompat.requestPermissions(activity!!, arrayOf(CAMERA), MY_PERMISSION_REQUEST_CAMERA)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(takePhotoIntent, 0)
        } else {
            createEventWhatImageView.setImageResource(R.drawable.imageplaceholder)
        }
    }

    private fun setIsPublicOrPrivate() {
        viewToBeCreated?.createEventWhatIsPublic?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewToBeCreated?.createEventWhatIsPublic?.text = "Private" else viewToBeCreated?.createEventWhatIsPublic?.text = "Public"
        }
    }

    private fun setCreateEventWhatImageView() {

        val point = Point()
        activity?.windowManager?.defaultDisplay?.getSize(point)

        val heightOfEventDetailImageView = (point.x - 16)* 9 / 16 // THIS IS HARDCODED - NOT NICE
        viewToBeCreated?.createEventWhatImageView?.layoutParams?.height = heightOfEventDetailImageView
        viewToBeCreated?.createEventWhatImageView?.requestLayout()

    }
}