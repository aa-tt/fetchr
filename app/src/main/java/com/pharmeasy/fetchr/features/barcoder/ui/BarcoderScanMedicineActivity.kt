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
import com.pharmeasy.fetchr.features.barcoder.presenter.BarcoderScanMedicinePresenterImpl
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderScanMedicineView
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

class BarcoderScanMedicineActivity  : ScannerBaseActivity(), BarcoderScanMedicineView {

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progressBar

    private lateinit var mPresenter: BarcoderScanMedicinePresenterImpl

    private var trayId: String? = null

    private var max_time = 2
    private lateinit var scanner: CodeScanner


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcoder_scan_tray)

        initViews()

        if(intent.extras != null && intent.getStringExtra("trayId") != null) {
            trayId = intent.getStringExtra("trayId")
        }else
            finish()

        mPresenter = BarcoderScanMedicinePresenterImpl(this)
        mPresenter.attachView(this)
    }

    private fun initViews(){

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

        tv_scan.text = getString(R.string.scan_any_medicine_barcode)
        tv_scanner.text = getString(R.string.scan_barcode)
        tvTitle.text=getString(R.string.scan_item_barcode)
    }

    override fun onResume() {
        super.onResume()
        scanner.startPreview()
        mPresenter.tasksOnViewResumed()
    }

    override fun onPause() {
        super.onPause()
        scanner.stopPreview()
    }

    override fun success() {
        handleStatus("", true)
    }

    override fun failure( text: String) {
        handleStatus(text, false)
    }

    override fun handleStatus(text: String, status: Boolean) {
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

    override fun showStatus() {
        scanner.isFlashEnabled = false
        scanner.stopPreview()

        scanner_view.visibility = View.INVISIBLE
        status_panel.visibility = View.VISIBLE
    }

    override fun showScanner() {
        scanner.isFlashEnabled = false
        scanner.startPreview()

        scanner_view.visibility = View.VISIBLE
        status_panel.visibility = View.INVISIBLE
    }

    override fun startTimer() {

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

    override fun showProgressBar() {
        showProgress()
    }

    override fun hideProgressBar() {
        hideProgress()
    }

    override fun showError(error: Throwable) {
        processError(error)
    }

    private fun processScanned(id: String){

        if(id.startsWith("TR")){
            failure(getString(R.string.invalid_barcode))
            return
        }

        success()
        mPresenter.getBarcoderTask(id, trayId!!)

    }

    override fun onBarcodeScanned(barcode: String) {
        processScanned(barcode)
    }

    override fun launchBarcodeMedicineDetailsActivity(mainTaskId: Long){
        val intent = Intent(this, BarcoderMedicineDetailsActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("trayId", trayId)
        startActivity(intent)
        finish()

    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter.detachView()
    }
}
