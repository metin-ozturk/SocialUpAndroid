package com.jora.socialup.fragments.authentication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jora.socialup.R
import com.jora.socialup.models.User
import kotlinx.android.synthetic.main.fragment_dialog_sign_up_complete_information.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.text.DecimalFormat


class SignUpCompleteInformationDialogFragment : DialogFragment() {

    interface SignUpCompleteInformationDialogFragmentInterface {
        fun onFinish()
        fun onDialogFragmentDestroyed()
    }

    private var viewToBeCreated : View? = null

    private var listener: SignUpCompleteInformationDialogFragmentInterface? = null

    private var firebaseAuthentication : FirebaseAuth? = null

    private var userToBeCreated : User? = null

    private var currentDay = 0
    private var currentMonth = 0
    private var currentYear = 0
    private val years = (1900..2019).map { it.toString() }.toTypedArray()

    private var emailToBeRetrieved = ""
    private var nameToBeRetrieved = ""
    private var birthdayToBeRetrieved = ""
    private var urlToBeRetrieved = ""

    private var uploadedProfilePhoto : Bitmap? = null

    companion object {
        fun newInstance(listener: SignUpCompleteInformationDialogFragmentInterface) : SignUpCompleteInformationDialogFragment {
            val dialogFragment =
                SignUpCompleteInformationDialogFragment()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_sign_up_complete_information, container, false)
        firebaseAuthentication = FirebaseAuth.getInstance()


        setGenderPicker()
        setYears()
        setMonths()
        setDays()
        setConfirmButtonListener()
        setUploadPhotoButton()

        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)

        return viewToBeCreated
        }

    override fun onResume() {
        super.onResume()
        fillInitialInfo()

    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.onDialogFragmentDestroyed()
    }

    fun completeSignUpInfo(email: String = "", name: String = "", imageUrl: String = "", birthday: String = "") {
        emailToBeRetrieved = email
        nameToBeRetrieved = name
        urlToBeRetrieved = imageUrl
        birthdayToBeRetrieved = birthday
    }

    override fun onPause() {
        super.onPause()

        viewToBeCreated?.apply {
            fragmentDialogSignUpCompleteInformationNameInput?.text?.clear()
            fragmentDialogSignUpCompleteInformationEmailInput.text?.clear()
            fragmentDialogSignUpCompleteInformationPhotoImageView?.setImageResource(R.drawable.imageplaceholder)
        }
    }

    private fun fillInitialInfo() {
        viewToBeCreated?.apply {

            fragmentDialogSignUpCompleteInformationEmailInput?.text?.insert(0, emailToBeRetrieved )

            if (nameToBeRetrieved.isNotEmpty()) fragmentDialogSignUpCompleteInformationNameInput.text?.insert(0, nameToBeRetrieved)

            if (urlToBeRetrieved.isNotEmpty()) {
                val bgScope = CoroutineScope(Dispatchers.IO)

                bgScope.launch {
                    val downloadedImage = BitmapFactory.decodeStream(URL(urlToBeRetrieved).content as InputStream)
                    withContext(Dispatchers.Main) {
                        viewToBeCreated?.fragmentDialogSignUpCompleteInformationPhotoImageView?.setImageBitmap(downloadedImage)
                        bgScope.cancel()
                    }

                }
            }

            if (birthdayToBeRetrieved.isNotEmpty()) {
                birthdayToBeRetrieved.substring(0, 2).toInt().also {
                    currentDay = it
                    fragmentDialogSignUpCompleteInformationDayPicker.value = it - 1
                }

                birthdayToBeRetrieved.substring(2, 4).toInt().also {
                    currentMonth = it
                    fragmentDialogSignUpCompleteInformationMonthPicker.value = it - 1
                }

                val yearsBetweenCurrentYearAndBirthday = currentYear - birthdayToBeRetrieved.substring(4, 8).toInt()
                val yearIndex = years.size - yearsBetweenCurrentYearAndBirthday
                fragmentDialogSignUpCompleteInformationYearPicker.value = yearIndex - 1
                currentYear = birthdayToBeRetrieved.substring(4,8).toInt()
            }
        }
    }


    private fun setConfirmButtonListener() {
        val userID = firebaseAuthentication?.currentUser?.uid ?: return

        viewToBeCreated?.apply {
            fragmentDialogSignUpCompleteInformationConfirmButton?.setOnClickListener {
                val gender = when (fragmentDialogSignUpCompleteInformationGenderPicker.value) {
                    0 -> "Male"
                    1 -> "Female"
                    else -> "Error"
                }

                val twoDecimalFormat = DecimalFormat("00")
                val birthday = twoDecimalFormat.format(currentDay) + twoDecimalFormat.format(currentMonth) + currentYear

                userToBeCreated = User(userID, fragmentDialogSignUpCompleteInformationNameInput.text.toString(),
                    fragmentDialogSignUpCompleteInformationEmailInput.text.toString(),
                    gender,
                    birthday,
                    ArrayList(),
                    false)

                uploadUserInformationToFirestore()


            }
        }
    }

    private fun uploadUserInformationToFirestore() {
        val userID = firebaseAuthentication?.currentUser?.uid ?: return
        val userReference = FirebaseFirestore.getInstance().collection("users").document(userID)
        val userProfilePhotoReference = FirebaseStorage.getInstance().reference.child("Images/Users/$userID/profilePhoto.jpeg")


        val bgScope = CoroutineScope(Dispatchers.IO)
        bgScope.launch {
            var userImageToBeUploadedAsBitmap = uploadedProfilePhoto

            if (userImageToBeUploadedAsBitmap == null && urlToBeRetrieved.isNotEmpty())
                userImageToBeUploadedAsBitmap = BitmapFactory.decodeStream(URL(urlToBeRetrieved).content as InputStream)

            val outputStream = ByteArrayOutputStream()
            userImageToBeUploadedAsBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val userImageToBeUploadedAsJPEG = outputStream.toByteArray()

            userProfilePhotoReference.putBytes(userImageToBeUploadedAsJPEG).await()
            userReference.set(userToBeCreated?.returnUserInformation() as Map<String, Any>).await()
            listener?.onFinish()

            bgScope.cancel()
        }

    }

    private fun setUploadPhotoButton() {
        viewToBeCreated?.fragmenDialogSignUpCompleteInformationPhotoButton?.setOnClickListener {

            val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickPhotoIntent, 1)

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            val selectedImage = data?.data ?: return

            uploadedProfilePhoto = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(activity!!.contentResolver, selectedImage)
            } else {
                val imageSource = ImageDecoder.createSource(activity!!.contentResolver, selectedImage)
                ImageDecoder.decodeBitmap(imageSource)
            }

            viewToBeCreated?.fragmentDialogSignUpCompleteInformationPhotoImageView?.setImageURI(selectedImage)
        }


    }

    private fun setGenderPicker() {
        viewToBeCreated?.fragmentDialogSignUpCompleteInformationGenderPicker?.apply {
            displayedValues = arrayOf("Male", "Female")
            minValue = 0
            maxValue = 1
            value = 0
        }
    }
    private fun setYears() {

        viewToBeCreated?.fragmentDialogSignUpCompleteInformationYearPicker?.apply {
            displayedValues = years
            minValue = 0
            maxValue = years.size - 1
            value = years.size - 1
            currentYear = 2019

            setOnValueChangedListener { _, _, newVal ->
                currentYear = years[newVal].toInt()
                setDays()
            }
        }
    }
    private fun setMonths() {
        val months = (1..12).map { it.toString() }.toTypedArray()

        viewToBeCreated?.fragmentDialogSignUpCompleteInformationMonthPicker?.apply {
            displayedValues = months
            minValue = 0
            maxValue = months.size - 1
            value = 0
            currentMonth = 1

            setOnValueChangedListener { _, _, newVal ->
                currentMonth = months[newVal].toInt()
                setDays()
            }
        }
    }
    private fun setDays() {
        val days = (1..31).map { it.toString() }.toTypedArray()

        viewToBeCreated?.fragmentDialogSignUpCompleteInformationDayPicker?.apply {


            val numberOfDaysInTheMonth = when (currentMonth) {
                1 -> 31
                2 -> if (currentYear % 4 == 0) 29 else 28
                3 -> 31
                4 -> 30
                5 -> 31
                6 -> 30
                7 -> 31
                8 -> 31
                9 ->  30
                10 ->  31
                11 ->  30
                else -> 31
            }

            minValue = 0

            if (maxValue > numberOfDaysInTheMonth - 1) {
                maxValue = numberOfDaysInTheMonth - 1
                displayedValues = days.copyOfRange(0, numberOfDaysInTheMonth)
            } else {
                displayedValues = days.copyOfRange(0, numberOfDaysInTheMonth)
                maxValue = numberOfDaysInTheMonth - 1
            }

            value = 0
            currentDay = 1


            setOnValueChangedListener { _, _, newVal ->
                currentDay = days[newVal].toInt()
            }

        }
    }


}