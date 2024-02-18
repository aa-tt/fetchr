package com.pharmeasy.fetchr.features.barcoder.ui

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
import com.pharmeasy.fetchr.constants.PAGE_SIZE
import com.pharmeasy.fetchr.features.barcoder.presenter.BarcoderScanIssueMedicinePresenterImpl
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderScanIssueMedicineView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.BarcoderItem
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.greendao.model.VerifierItem
import com.pharmeasy.fetchr.model.BarcoderNextItemTask
import com.pharmeasy.fetchr.model.NextItemTask
import com.pharmeasy.fetchr.model.ProductLotItem
import com.pharmeasy.fetchr.retro.*
import com.pharmeasy.fetchr.service.BarcoderService
import com.pharmeasy.fetchr.service.VerifierService
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.ProcessingStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.barcode_layout.*
import kotlinx.android.synthetic.main.scan_content.*
import kotlinx.android.synthetic.main.scan_issue_item_content.*
import kotlinx.android.synthetic.main.scan_issue_item_main.*

class BarcoderScanIssueMedicineActivity : ScannerBaseActivity() {

    private var max_time = 2
    private lateinit var scanner: CodeScanner
    private lateinit var task: UserTask
    private lateinit var issueReason: String
    private var mainTaskId: Long? = -1
    private var taskId: Long? = -1

    private var barcodes = hashSetOf<String>()
    private var scanned = hashSetOf<String>()
    private var issues = hashSetOf<String>()
    private var currentIssues = hashSetOf<String>()
    private var processed = hashSetOf<String>()

    private var pageCount: Int = 0

    private var damagedBarcodes: Int = 0

    private var isCameraReleaseOrNot: Boolean = true

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    private val service by lazy {
        retroWithToken(BarcoderService::class.java)
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


        issueReason = intent.getStringExtra("issueReason")
        mainTaskId = intent.getLongExtra("mainTaskId", 0)
        taskId = intent.getLongExtra("taskId", 0)

        val items = repo.getItemsByTask(mainTaskId!!)
        populateItem(items)
        updateInfo()

        initViews()
    }

    private fun initViews(){

        done.setOnClickListener {
            showBarcodeIssueTray()
        }
    }

    private fun success() {
        handleStatus("", true)
    }

    fun failure(userAction: UserAction, text: String, barcode: String? = null) {

        Observable.just("verifier")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    repo.addEvent(eventOf(userAction, barcode!!))
                }

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

    private fun updateInfo() {
        issue_type.text = issueReason
    }

    override fun onResume() {
        super.onResume()

        scanner.isFlashEnabled = false
        scanner.startPreview()
        retrieve()
    }

    override fun onPause() {
        super.onPause()

        isCameraReleaseOrNot = true
        scanner.releaseResources()
        scanner.stopPreview()
        super.onPause()
    }

    private fun retrieve() {
        repo.refresh()

        val tasks = repo.getTaskById(mainTaskId!!)
        this.task = tasks.first()

        val items = repo.getBarcoderItemsByTask(mainTaskId!!)
        populate(items)
    }

    private fun populate(items: List<BarcoderItem>) {

        barcodes = items.filter { it.barCode != null }.map { it.barCode!! }.toHashSet()
        scanned = items.filter { it.status == ItemStatus.READY_FOR_PUTAWAY.name}.map { it.barCode!! }.toHashSet()
        currentIssues = items.filter { it.status == ItemStatus.IN_ISSUE.name }.filter { it.returnReason == issueReason }.map { it.barCode!! }.toHashSet()
        issues = items.filter { it.status == ItemStatus.IN_ISSUE.name }.map { it.barCode!! }.toHashSet()
        processed = items.filter { it.processed == ProcessingStatus.SYNC.value }.map { it.barCode!! }.toHashSet()

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

        if (processed.contains(barcode)) {
            failure(UserAction.ERROR_ALREADY_SCANNED, getString(R.string.already_processed), barcode)
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

            Observable.just(NEW_OBSERVEABLE)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        repo.markBarcoderIssueItem(taskId = mainTaskId!!, barcode = barcode, reason = issueReason, status = ItemStatus.IN_ISSUE)
                    }
            currentIssues.add(barcode)

            scanned_items.text = currentIssues.size.toString()

            success()
        } else
            failure(UserAction.ERROR_EXCESS_ITEM, getString(R.string.marked_for_issue), barcode)
    }

    override fun onBarcodeScanned(barcode: String) {
        beep()
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

    private fun showBarcodeIssueTray(){
        val intent = Intent(this, BarcoderIssueTrayActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("issueReason", issueReason)
        intent.putExtra("quantity", currentIssues.size.toString())
        startActivity(intent)
        finish()
    }

    private fun populateItem(taskItems: List<TaskItem>) {
        val taskItem = taskItems[0]

        val nextItem = repo.getBarcoderItemsByTaskAndUCode(mainTaskId!!, taskItem.ucode, taskItem.batchNumber)
        if (nextItem.isEmpty()) {
            nextTaskItem(taskItem)
        }else
            retrieve()

        updateInfo()
    }

    private fun nextTaskItem(item: TaskItem) {

        showProgress()
        service.nextItem(taskId!!, item.ucode, item.batchNumber, pageCount, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> close(result, item) },
                        { error -> processError(error) }
                )
    }

    private fun close(nextItem: BarcoderNextItemTask, item: TaskItem) {
        if (nextItem.hasNext) {
            nextItem.data.forEach { it ->
                it.taskId = item.taskId
                it.processed = processingStatusOf(it.status!!) // processed status changes
                it.batchNumber = item.batchNumber
                it.expiryDate = item.expiryDate
                it.mrp = item.mrp
                it.packForm = item.packForm
                it.name = item.name
                repo.addBarcoderItem(it)
            }
            pageCount++
            nextTaskItem(item)
            return
        } else {
            nextItem.data.forEach { it ->
                it.taskId = item.taskId
                it.processed = processingStatusOf(it.status!!) // processed status changes
                it.batchNumber = item.batchNumber
                it.expiryDate = item.expiryDate
                it.mrp = item.mrp
                it.packForm = item.packForm
                it.name = item.name
                repo.addBarcoderItem(it)
            }

            hideProgress()
            retrieve()
        }
    }
}
