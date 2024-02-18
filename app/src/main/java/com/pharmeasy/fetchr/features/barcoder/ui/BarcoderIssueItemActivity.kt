package com.pharmeasy.fetchr.features.barcoder.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import android.widget.ProgressBar
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.google.gson.Gson
import com.pharmeasy.fetchr.BuildConfig
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.activity.ScanErrorActivity
import com.pharmeasy.fetchr.features.barcoder.presenter.BarcoderIssueItemPresenterImpl
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderIssueItemView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.BarcoderItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.*
import com.pharmeasy.fetchr.retro.*
import com.pharmeasy.fetchr.type.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_barcoder_issue_item.*
import kotlinx.android.synthetic.main.barcode_layout.*
import kotlinx.android.synthetic.main.button_layout.*
import kotlinx.android.synthetic.main.medicine_item_details_layout.*
import kotlinx.android.synthetic.main.scan_content.*
import kotlin.math.max

class BarcoderIssueItemActivity : BaseActivity(), BarcoderIssueItemView {


    private var max_time = 2
    private lateinit var scanner: CodeScanner
    private lateinit var task: UserTask
    private lateinit var productLot: ProductLotItem

    private var taskId: Long? = null

    private var barcodes = hashSetOf<String>()
    private var scanned = hashSetOf<String>()
    private var issues = hashSetOf<String>()

    private var selectedIssue = -1
    private var damagedBarcodes = 0
    private var missingBarcodes = 0
    private var total = 0
    private var currentIssues = 0

    private var count = 1
    private var jsonString: String? = null

    private lateinit var gson: Gson
    private lateinit var jsonData: JsonData

    private var isCameraReleaseOrNot: Boolean = true
    private var processed = hashSetOf<String>()

    private var mainTaskId: Long? = null

    private lateinit var mPresenter: BarcoderIssueItemPresenterImpl

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcoder_issue_item)

        gson = Gson()
        jsonData = JsonData()

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

        if (intent.extras != null) {
            mainTaskId = intent.getLongExtra("mainTaskId", 0)
            taskId = intent.getLongExtra("taskId", 0)
        }

        mPresenter = BarcoderIssueItemPresenterImpl(this, mainTaskId!!, taskId!!)
        mPresenter.attachView(this)

        val items = repo.getItemsByTask(mainTaskId!!)
        mPresenter.populateItem(items)

        initViews()
    }

    private fun initViews(){

        tvTitle.text = "Scan All Items"
        tv_scanner.text = "SCAN ITEM"
        iv_raise_issue_arrow.visibility = View.VISIBLE

        button_proceed.isEnabled = true

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            if (canGoBack())
                finish()
        }

        iv_raise_issue_arrow.setOnClickListener {
            showIssueTypes()
        }

        button_proceed.setOnClickListener {
            UpdateCompletedItemAsync().execute()
        }

        raise_ticket_button.setOnClickListener {
            raiseTicket()
        }
    }

    override fun onBackPressed() {

        if (canGoBack())
            super.onBackPressed()
    }

    private fun canGoBack(): Boolean {

        if (damagedBarcodes > 0 || missingBarcodes > 0 ) {
            message(getString(R.string.unsaved_changes))
            return false
        }

        return true
    }

    override fun updateInfo(product: ProductLotItem) {

        productLot = product

        jsonData.ucode = productLot.ucode
        jsonData.taskId = productLot.taskId
        jsonData.version = BuildConfig.VERSION_CODE.toString()

        medicine_name_text.text = "${productLot.ucode} - ${productLot.name} ${productLot.packForm}"
        medicine_batch_no_text.text = productLot.batchNumber
        value_mrp.text = productLot.mrp.toString()

        value_scanned.text = "0"
        value_issues.text = "0"
        value_pending.text = "0"
    }

    private fun showIssueTypes() {

        selectedIssue = -1

        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.issue_type)
                .setSingleChoiceItems(R.array.barcoder_issue_types, -1) { _, which -> selectedIssue = which }
                .setPositiveButton(R.string.continu) { _, _ -> onIssueTypeSelected() }
                .setNegativeButton(R.string.cancel, null)

        builder.create().show()
    }

    private fun onIssueTypeSelected() {

        if (selectedIssue < 0)
            return

        when (selectedIssue) {
            0 -> showMissingBarcodeQuantitySelection()
            1 -> showDamagedBarcodeQuantitySelection()
        }
    }

    private fun showMissingBarcodeQuantitySelection() {

        val quantityPicker = NumberPicker(this)
        quantityPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        quantityPicker.minValue = 0
        quantityPicker.maxValue = max(missingBarcodes, total - issues.size - scanned.size - damagedBarcodes)
        quantityPicker.value = missingBarcodes

        if ((quantityPicker.minValue > quantityPicker.maxValue) || (quantityPicker.minValue == 0 && quantityPicker.maxValue == 0)) {
            message(getString(R.string.no_items_left))
            return
        }

        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.missing_barcode_quantity)
                .setView(quantityPicker)
                .setPositiveButton(R.string.ok) { _, _ -> onMissingMedicineQuantitySelected(quantityPicker.value) }
                .setNegativeButton(R.string.cancel, null)

        builder.create().show()
    }

    private fun onMissingMedicineQuantitySelected(quantity: Int) {

        missingBarcodes = quantity

        updateItemsCount()
    }

    private fun showDamagedBarcodeQuantitySelection() {

        val quantityPicker = NumberPicker(this)
        quantityPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        quantityPicker.minValue = 0
        quantityPicker.maxValue = max(damagedBarcodes, total - issues.size - scanned.size - missingBarcodes)
        quantityPicker.value = damagedBarcodes

        if ((quantityPicker.minValue > quantityPicker.maxValue) || (quantityPicker.minValue == 0 && quantityPicker.maxValue == 0)) {
            message(getString(R.string.no_items_left))
            return
        }

        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.damaged_barcode_quantity)
                .setView(quantityPicker)
                .setPositiveButton(R.string.ok) { _, _ -> onDamagedBarcodeQuantitySelected(quantityPicker.value) }
                .setNegativeButton(R.string.cancel, null)

        builder.create().show()
    }

    private fun onDamagedBarcodeQuantitySelected(quantity: Int) {

        damagedBarcodes = quantity
        updateItemsCount()
    }

    private fun updateItemsCount() {

        value_scanned.text = scanned.size.toString()
        value_issues.text = (issues.size + damagedBarcodes + missingBarcodes).toString()
        value_pending.text = (total - scanned.size - issues.size - damagedBarcodes - missingBarcodes).toString()

        if (value_pending.text == "0"){
            button_proceed.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()

        mPresenter.tasksOnViewResumed()
        scanner.isFlashEnabled = false
    }

    override fun onPause() {
        isCameraReleaseOrNot = true
        scanner.releaseResources()
        scanner.stopPreview()
        super.onPause()
    }

    override fun populate(items: List<BarcoderItem>, userTask: UserTask) {

        Log.d("SIA", items.size.toString())
        /*items.forEach {
            Log.d("SIA", "${it.ucode} ${it.batchNumber} ${it.barCode} ${it.status} ${it.trayId} ${it.returnReason} ${it.processed}")
        }*/

        task = userTask

        barcodes = items.filter { it.barCode != null }.map { it.barCode!! }.toHashSet()
        scanned = items.filter { it.status == ItemStatus.READY_FOR_PUTAWAY.name }.map { it.barCode!! }.toHashSet()
        issues = items.filter { it.status == ItemStatus.IN_ISSUE.name }.map { it.barCode!! }.toHashSet()
        processed = items.filter { it.processed == ProcessingStatus.SYNC.value }.map { it.barCode!! }.toHashSet()

        total = items.size

        updateItemsCount()
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

        if (scanned.contains(barcode)) {
            failure(UserAction.ERROR_ALREADY_SCANNED, getString(R.string.already_scanned), barcode)
            return
        }

        if (processed.contains(barcode)) {
            failure(UserAction.ERROR_ALREADY_SCANNED, getString(R.string.already_processed), barcode)
            return
        }

        val pending = total - scanned.size - issues.size - damagedBarcodes - missingBarcodes
        var swapped = false

        if (issues.contains(barcode)) {
            swapped = true
            issues.remove(barcode)
        }

        if (swapped || pending != 0) {

            if (scanned.isEmpty())
                repo.addEvent(eventOf(UserAction.TASK_START, task.taskId.toString(), jsonString))

            Observable.just(NEW_OBSERVEABLE)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        repo.markBarcoderCompletedItem(taskId = productLot.id, barcode = barcode, trayId = task.trayId!!, status = ItemStatus.READY_FOR_PUTAWAY)
                    }

            scanned.add(barcode)

            jsonData.quantity = count
            jsonString = gson.toJson(jsonData)

            updateItemsCount()
            success()

        } else {
            jsonString = gson.toJson(jsonData)
            failure(UserAction.ERROR_EXCESS_ITEM, getString(R.string.marked_for_issue), barcode)
        }
    }

    private fun completeItem() {


        val damaged = getString(R.string.damaged_barcode)
        val missing = getString(R.string.missing_barcode)

        val reasons = mutableListOf<String>()

        for (i in 1..damagedBarcodes)
            reasons.add(damaged)

        for (i in 1..missingBarcodes)
            reasons.add(missing)

        currentIssues = damagedBarcodes + missingBarcodes

        damagedBarcodes = 0
        missingBarcodes = 0

        val pending = barcodes.subtract(issues).subtract(scanned)

        pending.forEachIndexed { index, it ->
            repo.markBarcoderIssueItem(taskId = productLot.id, barcode = it, reason = reasons[index], status = ItemStatus.IN_ISSUE)
        }

    }

    fun onBarcodeScanned(barcode: String) {
        beep()
        processScanned(barcode)
    }

    private fun handleWrongItem(barcode: String) {
        //errorBeep()
        repo.addEvent(eventOf(UserAction.ERROR_MEDICINE, barcode, jsonString))
        val intent = Intent(this, ScanErrorActivity::class.java)
        intent.putExtra("title", getString(R.string.scan_error))
        intent.putExtra("message", getString(R.string.wrong_item_scanned))
        startActivity(intent)
    }

    /**
     * AsyncClass to update all items to server and keeping user to wait while processing
     * */
    @SuppressLint("StaticFieldLeak")
    inner class UpdateCompletedItemAsync : AsyncTask<Void, Void, Void>() {

        override fun onPreExecute() {
            super.onPreExecute()
            showProgress()
        }

        override fun doInBackground(vararg p0: Void?): Void? {
            completeItem()
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            hideProgress()
            completeTask()

        }
    }


    private fun completeTask() {

        val intent = Intent(this, BarcoderIssueTrayActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId!!)
        intent.putExtra("issueReason", "Missing/Damaged Barcode")
        intent.putExtra("quantity", currentIssues.toString())
        startActivity(intent)
        finish()
    }

    override fun showIssueQtyBottomSheet() {

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

    fun success() {
        handleStatus("", true)
    }

    fun failure(userAction: UserAction, text: String, barcode: String? = null) {
        //errorBeep()
        repo.addEvent(eventOf(userAction, barcode!!))
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
}