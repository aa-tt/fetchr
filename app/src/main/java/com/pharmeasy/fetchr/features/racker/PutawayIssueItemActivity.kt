package com.pharmeasy.fetchr.features.racker

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ProgressBar
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanErrorActivity
import com.pharmeasy.fetchr.activity.ScannerBaseActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.failure_time
import com.pharmeasy.fetchr.retro.success_time
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.scan_content.*
import kotlinx.android.synthetic.main.scan_issue_item_content.*
import kotlinx.android.synthetic.main.scan_issue_item_main.*

class PutawayIssueItemActivity() : ScannerBaseActivity() {

    private var max_time = 2
    private lateinit var scanner: CodeScanner
    private lateinit var task: UserTask
    private lateinit var type: String
    private var ucode: String? = null
    private var mainTaskId: Long? = -1

    private var barcodes = hashSetOf<String>()
    private var scanned = hashSetOf<String>()
    private var issues = hashSetOf<String>()
    private var currentIssues = hashSetOf<String>()

    private var damagedBarcodes = 0

    private var isCameraReleaseOrNot: Boolean = true

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progressBar

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_issue_item_main)

        title = getString(R.string.scan_issue_medicines)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        done.setOnClickListener {
            finish()
        }

        scanner = CodeScanner.builder()
                .formats(CodeScanner.ALL_FORMATS)
                .autoFocus(true).autoFocusMode(AutoFocusMode.SAFE).autoFocusInterval(2000L)
                .flash(false)
                .onDecoded { result ->
                    runOnUiThread {
                        onBarcodeScanned(result.text)
                    }
                }
                .onError { _ -> }.build(this, scanner_view)

        scanner_view.setOnClickListener {
            if (isCameraReleaseOrNot) {
                isCameraReleaseOrNot = false
                scanner.startPreview()
            } else {
                scanner.stopPreview()
                isCameraReleaseOrNot = true
            }
        }

        if (intent.extras != null && intent.extras.containsKey("mainTaskId")) {
            mainTaskId = intent.getLongExtra("mainTaskId",0)
            type = intent.getStringExtra("type")
            ucode = intent.getStringExtra("ucode")
            damagedBarcodes = intent.getIntExtra("damagedBarcodes", 0)
            updateInfo()
        } else {
            message("Unable to process")
            finish()
        }

        //raise issue
        raise_ticket_button.setOnClickListener{
            raiseTicket()
        }
    }

    fun success() {
        handleStatus("", true)
    }

    fun failure(userAction: UserAction, text: String, barcode: String? = null) {
        //errorBeep()
        Observable.just("racker")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    repo.addEvent(eventOf(userAction, barcode!!))
                }
        handleStatus(text, false)
    }

    private fun handleStatus(text: String, status: Boolean) {
        showStatus()

        if(status) {
            status_ok.visibility = View.VISIBLE
            status_error.visibility = View.GONE
        }else{
            status_ok.visibility = View.GONE
            status_error.visibility = View.VISIBLE
        }

        status_text.text = text

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

    private fun updateInfo() {
        issue_type.text = type
    }

    override fun onResume() {
        super.onResume()

        retrieve()
    }

    override fun onPause() {
        super.onPause()
        isCameraReleaseOrNot = true
        scanner.isFlashEnabled = false
        scanner.releaseResources()
        scanner.stopPreview()
    }

    private fun retrieve() {

        val tasks = repo.getTaskById(mainTaskId!!)
        this.task = tasks.first()

        val items = repo.getItemsByTaskUcode(mainTaskId!!, ucode!!)
        populate(items)
    }

    private fun populate(items: List<TaskItem>) {

        barcodes = items.map { it.barCode!! }.toHashSet()
        scanned = items.filter { it.status == ItemStatus.LIVE.name }.map { it.barCode!! }.toHashSet()
        issues = items.filter { it.status == ItemStatus.IN_ISSUE.name }.map { it.barCode!! }.toHashSet()

        scanned_items.text = currentIssues.size.toString()
    }

    private fun processScanned(barcode: String) {

        if (!(barcode.length == 9 || barcode.length == 13 || barcode.length == 15)) {
            failure(UserAction.ERROR_INVALID_BARCODE, getString(R.string.invalid_barcode), barcode)
            return
        }

        if (!barcodes.contains(barcode)) {
            handleWrongItem(barcode)
            return
        }

        if (issues.contains(barcode) || currentIssues.contains(barcode)) {
            failure(UserAction.ERROR_ALREADY_SCANNED, getString(R.string.already_scanned), barcode)
            return
        }

        val total = barcodes.size
        val pending = total - scanned.size - issues.size - currentIssues.size - damagedBarcodes
        var swapped = false

        if (scanned.contains(barcode)) {
            scanned.remove(barcode)
            swapped = true
        }

        if (swapped || pending != 0) {
            repo.markIssueItem(taskId = mainTaskId!!, barcode = barcode, reason = type, status = ItemStatus.IN_ISSUE)
            currentIssues.add(barcode)

            scanned_items.text = currentIssues.size.toString()
            success()
        } else
            failure(UserAction.ERROR_EXCESS_ITEM, getString(R.string.marked_for_issue), barcode)
    }

    override fun onBarcodeScanned(barcode: String) {
        beep()
        //message(barcode)
        processScanned(barcode)
    }

    private fun handleWrongItem(barcode: String) {
        //errorBeep()
        repo.addEvent(eventOf(UserAction.ERROR_MEDICINE, barcode))
        val intent = Intent(this, ScanErrorActivity::class.java)
        intent.putExtra("title", getString(R.string.scan_error))
        intent.putExtra("message", getString(R.string.wrong_item_scanned))
        startActivity(intent)
    }
}
