package com.pharmeasy.fetchr.features.racker

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ProgressBar
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.main.ui.MainActivity
import com.pharmeasy.fetchr.activity.ScannerBaseActivity
import com.pharmeasy.fetchr.constants.TRAYZONE
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.Task
import com.pharmeasy.fetchr.retro.failure_time
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.retro.success_time
import com.pharmeasy.fetchr.service.RackerService
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.type.TaskStatus
import com.pharmeasy.fetchr.type.TaskType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_rack_scan_bin.*
import kotlinx.android.synthetic.main.barcode_layout.*
import kotlinx.android.synthetic.main.bin_tray_scan_text.*
import kotlinx.android.synthetic.main.scan_content.*
import kotlinx.android.synthetic.main.scan_text_layout.*
import java.util.concurrent.TimeUnit

class RackScanTrayActivity : ScannerBaseActivity() {

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
    get() = progressBar

    private val service by lazy {
        retroWithToken(RackerService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    private var binId: String? = null
    private var trayId: String? = null
    private var ucode: String? = null
    private var trayPickedZone: String? = null
    private var uCount: Boolean? = null
    private var mainTaskId: Long? = -1

    private var max_time = 2

    private lateinit var scanner: CodeScanner

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


        tv_scan.text = getString(R.string.scan_tray_small)
        tv_scanner.text = getString(R.string.scan_tray_small)
        tv_Id.text = trayId
        bin_scan_name_text.text = binId
    }

    fun success() {
        handleStatus("", true)
    }

    fun failure( text: String) {
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
        if(id != trayId) {
            failure("Invalid tray Id")
            return
        }

        showProgress()

        if(SessionService.jitPickingEnabled!! && trayPickedZone == TaskStatus.JIT_ZONE.name) {
            service.getJitItemToScan(binId!!, trayId!!)
                    .debounce(500, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ result -> trayJitZoneScanned(result) },
                            { error -> processError(error) }
                    )

            return
        }

        service.getItemToScan(trayId!!, binId!!)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> trayZoneScanned(result) },
                        { error -> processError(error) }

                    )

    }

    private fun trayJitZoneScanned(task: Task){

        repo.refresh()
        success()

        val oldTask = repo.getTasksForUser(SessionService.userId)
        if(oldTask.isNotEmpty()) {
            mainTaskId = oldTask.first()._id
        }

        hideProgress()

        if (task.trayId == null) {
            repo.updateTaskStatus(mainTaskId!!, TaskStatus.COMPLETED)
            showActivity(this, MainActivity::class.java, null, null, null)
            return
        }

        showActivity(this, RackScanBinActivity::class.java, task.trayId, task.binId, trayPickedZone)
    }

    private fun trayZoneScanned(task: Task){

        repo.refresh()
        success()

        hideProgress()
        val oldTask = repo.getTasksForUser(SessionService.userId)
        if(oldTask.isNotEmpty()) {
            mainTaskId = oldTask.first()._id
            uCount = true
        }else
            mainTaskId = repo.addTask(SessionService.userId, TaskType.PUTAWAY, task, trayId, task.referenceId)

        success()

        val intent = Intent(this, PutawayItemActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("ucode", ucode)
        intent.putExtra("binId", binId)
        intent.putExtra("trayId", trayId)
        intent.putExtra("binStatus", true)
        intent.putExtra("uCount", uCount)
        intent.putExtra(TRAYZONE, trayPickedZone)
        startActivity(intent)
        finish()
    }

    override fun onBarcodeScanned(barcode: String) {
       processScanned(barcode)
    }

}
