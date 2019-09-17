package com.jora.socialup.fragments.authentication

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.android.synthetic.main.fragment_dialog_sign_up.view.*
import com.jora.socialup.R

class SignUpDialogFragment : DialogFragment() {

    interface SignUpDialogFragmentInterface {
        fun onFinish(email: String)
        fun loginWithFacebook()
        fun loginWithGoogle()
    }
    private var firebaseAuthentication : FirebaseAuth? = null
    private var listener: SignUpDialogFragmentInterface? = null

    private var viewToBeCreated : View? = null

    companion object {
        fun newInstance(listener: SignUpDialogFragmentInterface) : SignUpDialogFragment {
            val dialogFragment = SignUpDialogFragment()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_sign_up, container, false)

        firebaseAuthentication = FirebaseAuth.getInstance()

        viewToBeCreated?.apply {
            fragmentDialogSignUpCreateUserButton?.setOnClickListener {
                val password = fragmentDialogSignUpPasswordInput.text.toString()
                val email = fragmentDialogSignUpEmailInput.text.toString()

                firebaseAuthentication?.createUserWithEmailAndPassword(email, password)?.addOnSuccessListener {
                    listener?.onFinish(email)
                }?.addOnFailureListener {exception ->
                    if (exception is FirebaseAuthUserCollisionException) {
                        firebaseAuthentication?.fetchSignInMethodsForEmail(email)?.addOnSuccessListener { result ->
                                if (result.signInMethods?.contains("google.com") == true) {
                                    showAlertDialog(true)
                                    dismiss()
                                } else if ( (result.signInMethods?.contains("facebook.com") == true)) {
                                    showAlertDialog(false)
                                    dismiss()
                            }
                        }
                    }
                }
        }

        }

        return viewToBeCreated
    }

    private fun showAlertDialog(isGoogle: Boolean) {
        val alertDialog = AlertDialog.Builder(activity!!).create()
        val providerName = if (isGoogle) "Google" else "Facebook"
        alertDialog.apply {
            setTitle("$providerName Account Found")
            setMessage("You previously logged in with your $providerName Account. Please sign in to continue.")
            setButton(DialogInterface.BUTTON_POSITIVE, "YES") { dialogInterface, _ ->
                if (isGoogle) listener?.loginWithGoogle() else listener?.loginWithFacebook()
                dialogInterface.dismiss()
            }

            setButton(DialogInterface.BUTTON_NEGATIVE, "NO") { dialogInterface, _ ->
                dialogInterface.dismiss()

            }

            show()
        }
    }
}