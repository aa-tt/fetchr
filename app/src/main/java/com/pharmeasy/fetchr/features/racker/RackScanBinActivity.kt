package com.pharmeasy.fetchr.features.racker

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ProgressBar
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScannerBaseActivity
import com.pharmeasy.fetchr.constants.TRAYZONE
import com.pharmeasy.fetchr.retro.failure_time
import com.pharmeasy.fetchr.retro.success_time
import kotlinx.android.synthetic.main.activity_rack_scan_bin.*
import kotlinx.android.synthetic.main.rack_scan_next_tray_content.*
import kotlinx.android.synthetic.main.scan_content.*
import kotlinx.android.synthetic.main.scan_text_layout.*

class RackScanBinActivity : ScannerBaseActivity() {

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progressBar

    private lateinit var scanner: CodeScanner

    private var binId: String? = null
    private var trayId: String? = null
    private var ucode: String? = null
    private var uCount: Boolean? = null
    private var trayPickedZone: String? = null

    private var max_time = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rack_scan_bin)

        binId = intent.getStringExtra("binId")
        trayId = intent.getStringExtra("trayId")
        ucode = intent.getStringExtra("ucode")
        uCount = intent.getBooleanExtra("uCount", false)
        trayPickedZone = intent.getStringExtra(TRAYZONE)

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

        if(binId == null){
            jitInitViews()
        }else{
            initViews()
        }
    }



    private fun initViews(){
        tv_scan.text = "Scan Bin"
        tv_Id.text = binId
        bin_tray_scan_text.visibility = View.GONE
    }

    private fun jitInitViews(){
        tv_scan.text = "Scan JIT Bin"
        tv_Id.text = binId
        bin_tray_scan_text.visibility = View.GONE
    }

    fun success() {
        handleStatus("", true)
    }

    fun failure( text: String) {
        //errorBeep()
        handleStatus(text, false)
    }

    private fun handleStatus(text: String, status: Boolean) {
        showStatus()

        if (status) {
            status_ok.visibility = View.VISIBLE
            status_error.visibility = View.GONE
        } else {
            status_ok.visibility = View.GONE
            status_error.visibility = View.VISIBLE
        }


        if (text.isEmpty())
            status_text.visibility = View.GONE
        else {
            status_text.visibility = View.VISIBLE
            status_text.text = text
        }

        max_time = if (status) success_time else failure_time
        startTimer()

    }

    private fun showStatus() {
        scanner.isFlashEnabled = false
        scanner.stopPreview()

        scanner_view.visibility = View.INVISIBLE
        status_panel.visibility = View.VISIBLE
    }

    private fun showScanner() {
        scanner.isFlashEnabled = false
        scanner.startPreview()

        scanner_view.visibility = View.VISIBLE
        status_panel.visibility = View.INVISIBLE
    }

    private fun startTimer() {

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
                showScanner()
            }
        }

        countDownTimer.start()
    }

    private fun processScanned(id: String){
        if(binId != null && binId != id) {
            failure("Wrong barcode scanned")
            return
        }

        val intent = Intent(this, RackScanTrayActivity::class.java)
        intent.putExtra("binId", id)
        intent.putExtra("trayId", trayId)
        intent.putExtra("ucode", ucode)
        intent.putExtra("uCount", uCount)
        intent.putExtra(TRAYZONE, trayPickedZone)
        startActivity(intent)
        success()
        finish()
    }

    override fun onBarcodeScanned(barcode: String) {
        processScanned(barcode)

    }
}
