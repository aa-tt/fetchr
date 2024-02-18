package com.pharmeasy.fetchr.features.barcoder.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ProgressBar
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScannerBaseActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.retro.failure_time
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.retro.success_time
import com.pharmeasy.fetchr.service.BarcoderService
import kotlinx.android.synthetic.main.activity_rack_scan_bin.*
import kotlinx.android.synthetic.main.barcode_layout.*
import kotlinx.android.synthetic.main.complete_task.*
import kotlinx.android.synthetic.main.scan_content.*
import kotlinx.android.synthetic.main.scan_text_layout.*

class BarcoderScanTrayActivity : ScannerBaseActivity() {

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progressBar

    private var max_time = 2
    private lateinit var scanner: CodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcoder_scan_tray)

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

        tv_scan.text = getString(R.string.scan_empty_tray)
        tv_scanner.text = getString(R.string.scan_tray)
        tvTitle.text= getString(R.string.scan_empty_tray)

    }

    override fun onResume() {
        super.onResume()
        scanner.startPreview()
    }

    override fun onPause() {
        super.onPause()
        scanner.stopPreview()
    }

     fun success() {
        handleStatus("", true)
    }

     fun failure( text: String) {
        handleStatus(text, false)
    }

     fun handleStatus(text: String, status: Boolean) {
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

     fun showStatus() {
        scanner.isFlashEnabled = false
        scanner.stopPreview()

        scanner_view.visibility = View.INVISIBLE
        status_panel.visibility = View.VISIBLE
    }

     fun showScanner() {
        scanner.isFlashEnabled = false
        scanner.startPreview()

        scanner_view.visibility = View.VISIBLE
        status_panel.visibility = View.INVISIBLE
    }

     fun startTimer() {

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

    override fun onBarcodeScanned(barcode: String) {
        intentForBarcoderMedicine(barcode)
    }

    private fun intentForBarcoderMedicine(id: String){

        if(!id.startsWith("TR")){
            failure(getString(R.string.invalid_tray))
            return
        }

        val intent = Intent(this, BarcoderScanMedicineActivity::class.java)
        intent.putExtra("trayId", id)
        startActivity(intent)
        finish()

    }
}
