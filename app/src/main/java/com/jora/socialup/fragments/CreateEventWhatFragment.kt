package com.jora.socialup.fragments

import android.Manifest.permission.CAMERA
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import com.jora.socialup.helpers.OnSwipeTouchListener
import kotlinx.android.synthetic.main.fragment_create_event_what.*


private const val MY_PERMISSION_REQUEST_CAMERA = 2

class CreateEventWhatFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(com.jora.socialup.R.layout.fragment_create_event_what, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setCreateEventWhatImageView()
        setIsPublicOrPrivate()
        setGestureRecognizerToCreateEventImageView() // Also initialize pick and take photo here


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
        createEventWhatImageView.setOnTouchListener(
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
        }
    }

    private fun setIsPublicOrPrivate() {
        createEventWhatIsPublic.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) createEventWhatIsPublic.text = "Private" else createEventWhatIsPublic.text = "Public"
        }
    }

    private fun setCreateEventWhatImageView() {

        val point = Point()
        activity?.windowManager?.defaultDisplay?.getSize(point)

        val heightOfEventDetailImageView = (point.x - 16)* 9 / 16 // THIS IS HARDCODED - NOT NICE
        createEventWhatImageView.layoutParams.height = heightOfEventDetailImageView
        createEventWhatImageView.requestLayout()

    }
}