package com.pharmeasy.fetchr.activity

import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ProgressBar
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.retro.failure_time
import com.pharmeasy.fetchr.retro.success_time
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.scan_content.*
import kotlinx.android.synthetic.main.scan_main.*

abstract class ScanActivity() : ScannerBaseActivity() {

    private var max_time = 2
    private lateinit var scanner: CodeScanner

    override val rootView: View?
        get() = root

    override val progressIndicator: ProgressBar?
        get() = progressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_main);

        title = title()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        scanner = CodeScanner.builder()
            .formats(CodeScanner.ALL_FORMATS)
            .autoFocus(true).autoFocusMode(AutoFocusMode.SAFE).autoFocusInterval(2000L)
            .flash(false)
            .onDecoded { result ->
                runOnUiThread {
                    onBarcodeScanned(result.text.trim())
                }
            }
            .onError { _ -> }.build(this, scanner_view)

        scanner_view.setOnClickListener {
            scanner.startPreview()
        }


        //raise issue
        raise_ticket_button.setOnClickListener{
            raiseTicket()
        }

    }

    override fun onResume() {
        super.onResume()
        scanner.startPreview()
    }

    override fun onPause() {
        scanner.releaseResources()
        super.onPause()
    }

    override fun error(message: String, duration: Int) {
        failure(message)
    }

    protected abstract fun title(): String

    protected abstract fun next()

    protected abstract fun processScannedItem(id: String)

    fun processing() {
        handleStatus(getString(R.string.processing), null)
    }

    fun success(text: String) {
        handleStatus(text, true)
    }

    fun failure(text: String) {
        //errorBeep()
        handleStatus(text, false)
    }

    private fun handleStatus(text: String, status: Boolean?) {
        showStatus()

        status_text.text = text

        if (status != null) {

            if(status) {
                status_ok.visibility = View.VISIBLE
                status_error.visibility = View.GONE
            }else{
                errorMessage(text)
                status_ok.visibility = View.GONE
                status_error.visibility = View.GONE
            }

            max_time = if(status) success_time else failure_time
            startTimer(status)
        } else {
            status_ok.visibility = View.GONE
            status_error.visibility = View.GONE
            progressBar.isIndeterminate = true
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun showStatus() {
        scanner.isFlashEnabled = false
        scanner.stopPreview()

        scanner_view.visibility = View.INVISIBLE
        status_panel.visibility = View.VISIBLE
    }

    private fun showScanner() {

        scanner.startPreview()

        scanner_view.visibility = View.VISIBLE
        status_panel.visibility = View.INVISIBLE
    }

    private fun startTimer(status: Boolean) {

        progressBar.isIndeterminate = false
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        val countDownTimer = object : CountDownTimer((max_time * 1000).toLong(), 100) {

            override fun onTick(millisUntilFinished: Long) {

                val elapsed = (max_time * 1000) - millisUntilFinished.toInt()
                var progress = elapsed / (max_time * 10)
                if (progress < 0)
                    progress = 100

                progressBar.progress = progress
            }

            override fun onFinish() {

                progressBar.visibility = View.INVISIBLE

                if (status) {
                    scanner.releaseResources()

                    next()
                } else
                    showScanner()
            }
        }

        countDownTimer.start()
    }

    override fun onBarcodeScanned(barcode: String) {
        beep()
        //message(barcode)
        processScannedItem(barcode)
    }
}
