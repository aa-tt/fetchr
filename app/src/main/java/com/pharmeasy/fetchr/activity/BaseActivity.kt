package com.pharmeasy.fetchr.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.retro.httpErrorMessage
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import android.media.MediaPlayer
import android.provider.MediaStore
import android.provider.Settings
import com.pharmeasy.fetchr.main.ui.MainActivity
import com.pharmeasy.fetchr.service.SessionService
import org.jetbrains.anko.indeterminateProgressDialog

abstract class BaseActivity : AppCompatActivity() {

    protected abstract val rootView: View?

    protected abstract val progressIndicator: ProgressBar?

    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initProgress()
        deviceId()
    }

    private fun initProgress() {
        dialog = indeterminateProgressDialog(resources.getString(R.string.waiting))
        dialog!!.setCanceledOnTouchOutside(false)
        dialog!!.setCancelable(false)
        dialog!!.dismiss()
    }

    override fun onStop() {
        super.onStop()
        dialog?.dismiss()
    }

    protected fun hideKeyboard() {
        val view = currentFocus ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("cancelled", true)
        startActivity(intent)
        finish()
    }

    private fun openLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    protected open fun error(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        val view = rootView
        if (view == null) {
            message(message)
            return
        }

        Snackbar.make(view, message, duration).setAction("DISMISS") { }.show()
    }

    protected fun message(message: String) {

        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM, 0, 200)
        toast.show()
    }

    protected fun errorMessage(message: String) {
        if (rootView != null)
            Snackbar.make(rootView!!, message, 40000).setAction("Take ScreenShot") {
                takePicture() }.show()
    }

    protected fun homePage(message: String) {
        val builder = AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.ok) { _, _ -> startMainActivity() }

        builder.create().show()
    }

    private fun errorMessage2(message: String) {
        val builder = AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)

        builder.create().show()
    }

    protected fun showProgress() {
        hideKeyboard()
        dialog?.show()
    }

    protected fun hideProgress() {
        dialog?.dismiss()
    }

    protected fun processError(error: Throwable) {
        hideProgress()
        when (error) {
            is HttpException -> handleHttpError(error)
            is SocketTimeoutException -> error(getString(R.string.request_timeout))
            is IOException -> error(getString(R.string.network_error))
            else -> {
                if(error.message != null)
                    error(error.message!!)
            }
        }
    }

    protected fun handleHttpError(error: Throwable) {

        val message = httpErrorMessage(error) ?: getString(R.string.unable_to_process)

        if(message.contains("READY_FOR_PUTAWAY")){
            errorMessage2("No tray(s) are available in Zones.")
            return
        }

        if (message == "401") {
            error("Session Expired. Please Re-login")
            openLoginActivity()
            return
        }

        error(message)
    }

    protected fun beep() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 1000)
        } catch (e: Exception) {
            //Crashlytics.logException(e)
            e.printStackTrace()
        }
    }

    protected fun errorBeep() {
        try {
            val mp = MediaPlayer.create(baseContext, R.raw.errorbeep)
            mp.start()
        } catch (e: Exception) {
            Crashlytics.logException(e)
        }
    }

    @SuppressLint("HardwareIds")
    fun deviceId() {
        val androidId = Settings.Secure.getString(contentResolver,
                Settings.Secure.ANDROID_ID);

        SessionService.deviceId = androidId
    }

    fun raiseTicket() {
        val i = Intent(this, CreateTicket::class.java)
        startActivity(i)
    }

    private fun takePicture(){
        val screenShot = takeScreenShot(rootView!!)

        MediaStore.Images.Media.insertImage(
                contentResolver,
                screenShot,
                "Image",
                "Captured ScreenShot"
        )

        Toast.makeText(applicationContext, "Screen Captured.", Toast.LENGTH_SHORT).show();

    }

    private fun takeScreenShot(rootView: View): Bitmap {

        // Screenshot taken for the specified root view and its child elements.
        val bitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        rootView.draw(canvas)
        return bitmap
    }

    public fun <T> showActivity(context: Context, activity: Class<T>, value: String?, binValue: String?, trayPickedZone: String?, mainTaskId: Long? = null){
        val intent = Intent(context, activity)
        intent.putExtra("trayId", value)
        intent.putExtra("binId", binValue)
        intent.putExtra("trayPickedZone", trayPickedZone)
        intent.putExtra("mainTaskId", mainTaskId)
        startActivity(intent)
        finishAffinity()
    }


}

