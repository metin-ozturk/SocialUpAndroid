package com.jora.socialup.activities

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.facebook.*
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.android.synthetic.main.activity_home.*
import com.facebook.login.LoginResult
import com.google.firebase.auth.*
import com.jora.socialup.fragments.SignInDialogFragment
import com.jora.socialup.fragments.SignUpCompleteInformationDialogFragment
import com.jora.socialup.fragments.SignUpDialogFragment
import com.jora.socialup.models.User
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.net.URL


private const val googleSignInRequestCode = 1
private const val googleSignInTag = "GoogleSignInTag"
private const val facebookSignInTag = "FacebookSignInTag"

class HomeActivity : AppCompatActivity() {

    private var googleSignInClient : GoogleSignInClient? = null
    private var signedInAccount: GoogleSignInAccount? = null

    private var firebaseAuthentication : FirebaseAuth? = null

    private var callbackManager : CallbackManager? = null

    private val signUpCompleteInformationDialogFragment : SignUpCompleteInformationDialogFragment by lazy {
        SignUpCompleteInformationDialogFragment(object: SignUpCompleteInformationDialogFragment.SignUpCompleteInformationDialogFragmentInterface {
            override fun onFinish() {
                startActivity(Intent(this@HomeActivity, HomeFeedActivity::class.java))
                signUpCompleteInformationDialogFragment.dismiss()
            }
        })
    }


    private val signUpDialogFragment : SignUpDialogFragment by lazy {
        SignUpDialogFragment(object: SignUpDialogFragment.SignUpDialogFragmentInterface {
            override fun onFinish(email: String) {
                // FIX
                signUpCompleteInformationDialogFragment.apply {
                    completeSignUpInfo(email)
                    show(supportFragmentManager, null)
                }
                    signUpDialogFragment.dismiss()
            }

            override fun loginWithFacebook() {
                LoginManager.getInstance().logIn(this@HomeActivity, arrayListOf("email", "public_profile"))
            }

            override fun loginWithGoogle() {
                signInWithGoogle()
            }
        })
    }

    private val signInDialogFragment : SignInDialogFragment by lazy {
        SignInDialogFragment(object: SignInDialogFragment.SignInDialogFragmentInterface {
            override fun onFinish() {
                startActivity(Intent(this@HomeActivity, HomeFeedActivity::class.java))
            }
        })
    }

    private var facebookCredential : AuthCredential? = null
    private var googleCredential : AuthCredential? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.jora.socialup.R.layout.activity_home)

        setSignUpWithEmailButton()
        setSignInButton()

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.jora.socialup.R.string.google_sign_in_server_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
        firebaseAuthentication = FirebaseAuth.getInstance()

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        callbackManager = CallbackManager.Factory.create()
        facebookLoginButton.setPermissions(arrayListOf("email", "public_profile"))

        facebookLoginButton.registerCallback(callbackManager, object: FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                Log.d(facebookSignInTag, "SUCCESS WITH FACEBOOK LOGIN")

                val accessToken = result?.accessToken ?: AccessToken.getCurrentAccessToken()

                val request = GraphRequest.newMeRequest(accessToken) { jsonObject, response ->
                    val birthday = jsonObject.get("birthday").toString().let { it.substring(3,5) + it.substring(0, 2) + it.substring(6,10) }
                    firebaseAuthWithFacebook(accessToken, jsonObject.get("email").toString(), birthday)
                }

                val parameters = Bundle()
                parameters.putString("fields", "id,name,email,birthday")
                request.parameters = parameters

                request.executeAsync()

            }

            override fun onCancel() {
                Log.d(facebookSignInTag, "FACEBOOK LOGIN CANCELED")
            }

            override fun onError(error: FacebookException?) {
                Log.d(facebookSignInTag, "ERROR DURING FACEBOOK LOGIN", error)

            }
        })

    }



    override fun onStart() {
        super.onStart()

//        if (firebaseAuthentication?.currentUser != null) startActivity(Intent(this, HomeFeedActivity::class.java))
    }

    private fun setSignUpWithEmailButton() {
        signUpWithEmail.setOnClickListener {
            signUpDialogFragment.show(supportFragmentManager, null)
        }
    }

    private fun setSignInButton() {
        signInButton.setOnClickListener {
            signInDialogFragment.show(supportFragmentManager, null)
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, googleSignInRequestCode)

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == googleSignInRequestCode) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                // Google Sign In was successful, authenticate with Firebase
                signedInAccount = task.getResult(ApiException::class.java)
                checkOtherThanGoogleAuthProvidersPresent(signedInAccount ?: return)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(googleSignInTag, "Google sign in failed", e)
            }
        } else {
            callbackManager?.onActivityResult(requestCode, resultCode, data)
        }

    }

    private fun checkOtherThanGoogleAuthProvidersPresent(googleSingInAccount: GoogleSignInAccount) {
        val gmailAddress = googleSingInAccount.email ?: return
        googleCredential = GoogleAuthProvider.getCredential(googleSingInAccount.idToken, null)


        val uiScope = CoroutineScope(Dispatchers.Main)

        uiScope.launch {
            val currentSignInMethods = firebaseAuthentication?.fetchSignInMethodsForEmail(gmailAddress)?.await()?.signInMethods ?: return@launch
            when {
                currentSignInMethods.contains("facebook.com") &&  !currentSignInMethods.contains("google.com") -> {

                    showAlertDialog("Google", "Facebook") {
                        LoginManager.getInstance().logIn(this@HomeActivity, arrayListOf("email", "public_profile"))
                    }


                    uiScope.cancel()
                }


                currentSignInMethods.contains("password") &&  !currentSignInMethods.contains("google.com") -> {

                    showAlertDialog("Google", "email") {
                        signInDialogFragment.retrievedAuthCredential = googleCredential
                        googleCredential = null
                        signInDialogFragment.show(supportFragmentManager, null)
                    }

                    uiScope.cancel()

                }

                else -> {
                    firebaseAuthWithGoogle(googleSingInAccount)
                    uiScope.cancel()
                }
            }

        }
    }


    private fun firebaseAuthWithFacebook(token: AccessToken, facebookEmail: String, birthday: String) {
        val credential = FacebookAuthProvider.getCredential(token.token)

        firebaseAuthentication?.signInWithCredential(credential)?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    if (task.result?.additionalUserInfo?.isNewUser == true) {
                        val user = task.result?.user
                        val profileImageURL = ((task.result?.additionalUserInfo?.profile?.get("picture") as Map<String, Any>)
                            .get("data") as Map<String, Any>)
                            .get("url") as String


                        signUpCompleteInformationDialogFragment.apply {
                            completeSignUpInfo(user?.email ?: "", user?.displayName ?: "", profileImageURL, birthday)
                            show(supportFragmentManager, null)
                        }


                        return@addOnCompleteListener
                    } else if (googleCredential != null) {
                        firebaseAuthentication?.currentUser?.linkWithCredential(googleCredential ?: return@addOnCompleteListener)
                        googleCredential = null
                    }


                    startActivity(Intent(this, HomeFeedActivity::class.java))


                } else {
                    // If sign in fails, display a message to the user.
                    facebookCredential = credential
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        val uiScope = CoroutineScope(Dispatchers.Main)
                        uiScope.launch {
                            val currentSignInMethods = firebaseAuthentication?.fetchSignInMethodsForEmail(facebookEmail)?.await()?.signInMethods

                            if (currentSignInMethods?.contains("google.com") == true) {

                                showAlertDialog("Facebook", "Google") {
                                    signInWithGoogle()
                                }
                                uiScope.cancel()

                            } else if (currentSignInMethods?.contains("password") == true) {

                                showAlertDialog("Facebook", "email") {
                                    signInDialogFragment.retrievedAuthCredential = facebookCredential
                                    facebookCredential = null
                                    signInDialogFragment.show(supportFragmentManager, null)
                                }

                                uiScope.cancel()

                            }
                        }


                    } else {
                        Log.e(facebookSignInTag, "Facebook Authentication Failed", task.exception)
                    }
                }

            }
    }

    private fun firebaseAuthWithGoogle(googleSingInAccount: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(googleSingInAccount.idToken, null)

        firebaseAuthentication?.signInWithCredential(credential)?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {

                if (task.result?.additionalUserInfo?.isNewUser == true) {
                    val user = task.result?.user
                    val profileImageURL = task.result?.additionalUserInfo?.profile?.get("picture") as? String

                    signUpCompleteInformationDialogFragment.apply {
                        completeSignUpInfo(user?.email ?: "", user?.displayName ?: "", profileImageURL ?: "" )
                        show(supportFragmentManager, null)
                    }

                    return@addOnCompleteListener
                } else if (facebookCredential != null) {
                    firebaseAuthentication?.currentUser?.linkWithCredential(facebookCredential ?: return@addOnCompleteListener)
                    facebookCredential = null
                }

                // Sign in success, update UI with the signed-in user's information

                startActivity(Intent(this, HomeFeedActivity::class.java))

            } else {
                // If sign in fails, display a message to the user.
                Log.w(googleSignInTag, "signInWithCredential:failure", task.exception)
            }

        }
    }

    private fun showAlertDialog(link: String, to: String, linkingFunction: () -> Unit) {
        val alertDialog = AlertDialog.Builder(this@HomeActivity).create()
        alertDialog.apply {
            setTitle("Link Your $link Account to $to Account?")
            setMessage("You previously signed in with your $to account. Would you like to sign in with your $to account and link your $link account to it?")
            setButton(DialogInterface.BUTTON_POSITIVE, "YES") { dialogInterface, _ ->
                linkingFunction()
                dialogInterface.dismiss()
            }

            setButton(DialogInterface.BUTTON_NEGATIVE, "NO") { dialogInterface, _ ->
                dialogInterface.dismiss()

            }

            show()
        }
    }


}