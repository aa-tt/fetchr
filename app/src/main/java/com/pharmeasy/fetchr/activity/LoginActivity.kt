package com.pharmeasy.fetchr.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ProgressBar
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.model.*
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retro
import com.pharmeasy.fetchr.retro.toBreakStatus
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.service.UserService
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.login.*
import kotlinx.android.synthetic.main.login_main.*
import java.text.SimpleDateFormat
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.BuildConfig
import com.pharmeasy.fetchr.main.ui.MainActivity
import kotlinx.android.synthetic.main.login_content.*


class LoginActivity() : BaseActivity(), GoogleApiClient.OnConnectionFailedListener {

    private val camera_request = 100
    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progress

    private val loginService by lazy {
        retro(UserService::class.java)
    }

    private val TAG = "GSA"
    private val RC_SIGN_IN = 9001

    private var googleApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_main)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        title = ""

        checkPermissions()

        sign_in.setOnClickListener {
            login()
        }

        google_sign_in.setOnClickListener {
            googleSignIn()
        }

        username.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                hideGooglePanel()
        }

        username.setOnClickListener {
            hideGooglePanel()
        }

        password.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                hideGooglePanel()
        }

        password.setOnClickListener {
            hideGooglePanel()
        }

        toolbar.setNavigationOnClickListener {
            showGooglePanel()
        }

        repo.refresh()

        //username.setText("barcoder1")
        //password.setText("12345")

        version.text = "${BuildConfig.BUILD_TYPE} v${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}"
    }

    private fun showGooglePanel() {

        if (google_panel.visibility == View.GONE) {
            google_panel.visibility = View.VISIBLE
            hideKeyboard()
            username.clearFocus()
            password.clearFocus()
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun hideGooglePanel() {
        if (google_panel.visibility != View.GONE) {
            google_panel.visibility = View.GONE
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    public override fun onStart() {
        super.onStart()

        /*val opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient)
        if (opr.isDone) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in")
            hideProgress()
            val result = opr.get()
            handleSignInResult(result)
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            showProgress()
            opr.setResultCallback { googleSignInResult ->
                hideProgress()
                handleSignInResult(googleSignInResult)
            }
        }*/
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {

        if (result.isSuccess) {
            // Signed in successfully, authenticate with firebase
            result.signInAccount

            //firebaseAuthWithGoogle(acct)
        } else {
            // Signed out, show unauthenticated UI.
        }
    }

    private fun googleSignIn() {
        message("Functionality not supported yet")
        //startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(googleApiClient), RC_SIGN_IN)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        //Log.d(TAG, "onConnectionFailed: $connectionResult")
    }

    private fun valid(): Boolean {
        return (username.text.trim().isNotEmpty() && password.text.trim().isNotEmpty())
    }

    fun login() {

        if (!valid()) {
            error("Invalid Username/Password")
            return
        }

        showProgress()
        loginService.signIn(Credentials(username.text.trim().toString(), password.text.trim().toString()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> process(result) },
                        { error -> processError(error) }
                )
    }

    private fun process(profile: Profile) {

        with(SessionService) {
            token = profile.token
            userId = profile.id
            name = profile.name
            username = profile.userId
        }

        val role = profile.roles.firstOrNull { it.id == verifier_role || it.id == racker_role || it.id == picker_role  || it.id == auditor_role || it.id == barcoder_role}
        if(role == null){
            error("UNKNOWN USER ROLE!")
            return
        }

        SessionService.role = role.id
        SessionService.breakStatus = toBreakStatus(profile.status)
        SessionService.breakStart = epoch(profile.lastBreakTime)

        repo.addEvent(eventOf(UserAction.LOGIN, SessionService.userId))
        //repo.clearTasksForUser(SessionService.userId)

        launchMain()
    }

    private fun epoch(text: String?): Long {

        if (text == null)
            return System.currentTimeMillis()

        val date = sdf.parse(text)
        return date.time + (330 * 60 * 1000)
    }

    private fun launchMain() {
       val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    camera_request )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            camera_request -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)  ||
                        (grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                }else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
