package com.jora.socialup.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.jora.socialup.R
import kotlinx.android.synthetic.main.fragment_dialog_sign_in.view.*

class SignInDialogFragment(private val listener: SignInDialogFragmentInterface) : DialogFragment() {

    interface SignInDialogFragmentInterface {
        fun onFinish()
    }

    var retrievedAuthCredential : AuthCredential? = null

    private var firebaseAuthentication : FirebaseAuth? = null

    private var viewToBeCreated : View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_sign_in, container, false)
        firebaseAuthentication = FirebaseAuth.getInstance()

        viewToBeCreated?.fragmentDialogSignInButton?.setOnClickListener {

            val email = viewToBeCreated?.fragmentDialogSignInEmailInput?.text.toString()
            val password = viewToBeCreated?.fragmentDialogSignInPasswordInput?.text.toString()

            firebaseAuthentication?.signInWithEmailAndPassword(email, password)?.addOnSuccessListener {
                if (retrievedAuthCredential != null) retrievedAuthCredential?.let { firebaseAuthentication?.currentUser?.linkWithCredential(it) }
                listener.onFinish()
                dismiss()
            }?.addOnFailureListener {

            }
        }


        return viewToBeCreated
    }
}