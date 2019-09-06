package com.jora.socialup.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore
import com.jora.socialup.R
import kotlinx.android.synthetic.main.fragment_dialog_sign_in.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInDialogFragment : DialogFragment() {

    interface SignInDialogFragmentInterface {
        fun onFinish()
        fun informationMissing(email: String)
        fun loginWithGoogle()
        fun loginWithFacebook()
    }

    var retrievedAuthCredential : AuthCredential? = null

    private var firebaseAuthentication : FirebaseAuth? = null

    private var viewToBeCreated : View? = null

    private var listener : SignInDialogFragmentInterface? = null

    companion object {
        fun newInstance(listener: SignInDialogFragmentInterface) : SignInDialogFragment {
            val dialogFragment = SignInDialogFragment()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewToBeCreated = inflater.inflate(R.layout.fragment_dialog_sign_in, container, false)
        firebaseAuthentication = FirebaseAuth.getInstance()

        viewToBeCreated?.fragmentDialogSignInButton?.setOnClickListener {

            val email = viewToBeCreated?.fragmentDialogSignInEmailInput?.text.toString()
            val password = viewToBeCreated?.fragmentDialogSignInPasswordInput?.text.toString()

            firebaseAuthentication?.signInWithEmailAndPassword(email, password)?.addOnSuccessListener {
                if (retrievedAuthCredential != null) retrievedAuthCredential?.let { cred -> firebaseAuthentication?.currentUser?.linkWithCredential(cred) }

                it.user?.uid?.also { userID ->
                    val uiScope = CoroutineScope(Dispatchers.Main)
                    uiScope.launch {
                        if (!FirebaseFirestore.getInstance().collection("users").document(userID).get().await().exists()) listener?.informationMissing(email)
                        else listener?.onFinish()

                        dismiss()
                    }
                }

            }?.addOnFailureListener {
                if (it is FirebaseAuthInvalidCredentialsException)
                    FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (task.result?.signInMethods?.contains("google.com") == true) showAlertDialog(true)
                            else if (task.result?.signInMethods?.contains("facebook.com") == true) showAlertDialog(false)
                        } else dismiss()
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
                this@SignInDialogFragment.dismiss()
                dialogInterface.dismiss()
            }

            setButton(DialogInterface.BUTTON_NEGATIVE, "NO") { dialogInterface, _ ->
                this@SignInDialogFragment.dismiss()
                dialogInterface.dismiss()

            }

            show()
        }
    }

}