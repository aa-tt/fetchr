package com.pharmeasy.fetchr.features.racker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.google.gson.Gson
import com.pharmeasy.fetchr.BuildConfig
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanErrorActivity
import com.pharmeasy.fetchr.activity.ScannerBaseActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.StartRacking
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.*
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.failure_time
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.retro.success_time
import com.pharmeasy.fetchr.service.RackerService
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.service.UserService
import com.pharmeasy.fetchr.type.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.barcode_layout.*
import kotlinx.android.synthetic.main.bin_tray_scan_text.*
import kotlinx.android.synthetic.main.button_layout.*
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.main.rack_single_ucode_in_tray.*
import kotlinx.android.synthetic.main.scan_content.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class PutawayItemActivity : ScannerBaseActivity() {

    private var max_time = 2
    private lateinit var scanner: CodeScanner
    private lateinit var task: UserTask

    private var barcodes = hashSetOf<String>()
    private var scanned = hashSetOf<String>()
    private var issues = hashSetOf<String>()
    private var issuesList = hashMapOf<String, HashSet<String>>()

    private var selectedIssue = -1
    private var damagedBarcodes = 0
    private var missingBarcodes = 0
    private var shortQuantities = 0
    private var total = 0
    private var showing_issues = false
    private var uCount: Boolean? = null
    private var jsonString: String? = null

    private var binStatus: Boolean? = null

    private var count = 0

    private lateinit var gson: Gson
    private lateinit var jsonData: JsonData

    private var isCameraReleaseOrNot: Boolean = true

    private var mainTaskId: Long = 0
    private var ucode: String? = null
    private var binId: String? = null
    private var trayId: String? = null

    private val userService by lazy {
        retroWithToken(UserService::class.java)
    }

    private val service by lazy {
        retroWithToken(RackerService::class.java)
    }

    private val issueTypes by lazy {
        resources.getStringArray(R.array.racker_issue_types)
    }

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progressBar

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.rack_single_ucode_in_tray)

        title = getString(R.string.putaway_medicines)

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

        if (intent.extras != null && intent.extras.containsKey("mainTaskId")) {
            mainTaskId = intent.getLongExtra("mainTaskId",0)
            ucode = intent.getStringExtra("ucode")
            binId = intent.getStringExtra("binId")
            trayId = intent.getStringExtra("trayId")
            uCount = intent.getBooleanExtra("uCount", false)
            binStatus = intent.getBooleanExtra("binStatus", false)
        } else {
            message("Unable to process")
            finish()
        }

        Log.d("PutawayItemActivity", "$uCount")

        jsonData.version = BuildConfig.VERSION_CODE.toString()

        //raise issue
        raise_ticket_button.setOnClickListener{
            raiseTicket()
        }

        button_raise_issue.setOnClickListener {
            showIssueTypes()
        }

        button_complete.setOnClickListener {
            completeCurrentLot()
        }

        show_issues.setOnClickListener{
            if (showing_issues) {
                scanned_panel.visibility = View.VISIBLE
                issues_panel.visibility = View.INVISIBLE
            } else {
                scanned_panel.visibility = View.INVISIBLE
                issues_panel.visibility = View.VISIBLE
            }

            showing_issues = !showing_issues
        }

        tray_scan_text.visibility = View.VISIBLE
        tray_scan_name_text.visibility = View.VISIBLE
        button_proceed.visibility = View.GONE
        bin_scan_name_text.text = binId
        tray_scan_name_text.text = trayId
        tv_scanner.text = "Scan Medicine"
    }

    override fun onBackPressed() {

        if (canGoBack())
            super.onBackPressed()
    }

    private fun canGoBack(): Boolean {

        if (damagedBarcodes > 0 || missingBarcodes > 0 || shortQuantities > 0 || binStatus!!) {
            message(getString(R.string.unsaved_changes))
            return false
        }

        return true
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

    private fun showIssueTypes() {

        selectedIssue = -1

        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.issue_type)
                .setSingleChoiceItems(R.array.racker_issue_types, -1) { _, which -> selectedIssue = which }
                .setPositiveButton(R.string.continu) { _, _ -> onIssueTypeSelected() }
                .setNegativeButton(R.string.cancel, null)

        builder.create().show()
    }

    private fun onIssueTypeSelected() {

        if (selectedIssue < 0)
            return

        when (selectedIssue) {
            0 -> showShortQuantitySelection()
            1 -> showMissingBarcodeQuantitySelection()
            2 -> showDamagedBarcodeQuantitySelection()
            else -> scanIssueMedicines(issueTypes[selectedIssue])
        }
    }

    private fun showShortQuantitySelection() {

        val quantityPicker = NumberPicker(this)
        quantityPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        quantityPicker.minValue = 0
        quantityPicker.maxValue = max(shortQuantities, total - issues.size - scanned.size - damagedBarcodes - missingBarcodes)
        quantityPicker.value = shortQuantities

        if ((quantityPicker.minValue > quantityPicker.maxValue) || (quantityPicker.minValue == 0 && quantityPicker.maxValue == 0)) {
            message(getString(R.string.no_items_left))
            return
        }

        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.short_quantity)
                .setView(quantityPicker)
                .setPositiveButton(R.string.ok) { _, _ -> onShortQuantitySelected(quantityPicker.value) }
                .setNegativeButton(R.string.cancel, null)

        builder.create().show()
    }

    private fun onShortQuantitySelected(quantity: Int) {

        shortQuantities = quantity

        updateItemsCount()
    }

    private fun showMissingBarcodeQuantitySelection() {

        val quantityPicker = NumberPicker(this)
        quantityPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        quantityPicker.minValue = 0
        quantityPicker.maxValue = max(missingBarcodes, total - issues.size - scanned.size - damagedBarcodes - shortQuantities)
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
        quantityPicker.maxValue = max(damagedBarcodes, total - issues.size - scanned.size - missingBarcodes - shortQuantities)
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

        scanned_items.text = scanned.size.toString()
        issue_items.text = (issues.size + damagedBarcodes + missingBarcodes + shortQuantities).toString()

        checkForCompletion()
    }

    private fun scanIssueMedicines(type: String) {
        val intent = Intent(this, PutawayIssueItemActivity::class.java)
        //intent.putExtra("data", mainTaskId)
        intent.putExtra("type", type)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("ucode", ucode)
        intent.putExtra("damagedBarcodes", damagedBarcodes + missingBarcodes + shortQuantities)

        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        retrieve()
    }

    override fun onPause() {
        isCameraReleaseOrNot = true
        scanner.releaseResources()
        scanner.stopPreview()
        uCount = true
        super.onPause()
    }

    private fun retrieve() {
        repo.refresh()

        val tasks = repo.getTaskById(mainTaskId)
        if(tasks.isNotEmpty())
            this.task = tasks.first()
        else
            finish()

        val items = repo.getItemsByTaskUcode(mainTaskId, ucode!!)
        if(items.isNotEmpty())
            populate(items)
        else
            errorMessage("List is empty")

        if (task.referenceType == Source.RECTIFICATION.name)
            button_raise_issue.visibility = View.GONE
    }

    private fun populate(items: List<TaskItem>) {

        barcodes = items.map { it.barCode!! }.toHashSet()
        scanned = items.filter { it.status == ItemStatus.LIVE.name }.map { it.barCode!! }.toHashSet()
        issues = items.filter { it.status == ItemStatus.IN_ISSUE.name }.map { it.barCode!! }.toHashSet()

        name.text = items.first().name

        items.forEach {
            // Log.d("PIA", "${it.ucode} ${it.batchNumber} ${it.status} ${it.trayId} ${it.returnReason} ${it.barCode}")
            if (!uCount!!) {
                jsonString = gson.toJson(jsonData)
                scanned.add(it.barCode.toString())
                updateItems(it.barCode.toString())
            }
        }

        total = items.size
        populateIssuesList(items)
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

        val pending = total - scanned.size - issues.size - damagedBarcodes - missingBarcodes - shortQuantities
        var swapped = false

        if (issues.contains(barcode)) {
            swapped = true
            issues.remove(barcode)

            issuesList.keys.forEach { key ->
                val entries = issuesList[key]
                if (entries != null && entries.contains(barcode))
                    entries.remove(barcode)
            }
        }

        if (swapped || pending != 0) {
            if (scanned.isEmpty())
                repo.addEvent(eventOf(UserAction.TASK_START, task.taskId.toString()))

            Observable.just("racker")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        repo.markCompletedItem(taskId = mainTaskId, barcode = barcode, trayId = task.trayId!!, status = ItemStatus.LIVE)
                    }

            scanned.add(barcode)
            jsonData.quantity = count

            updateItemsCount()
            success()
        } else {
            failure(UserAction.ERROR_EXCESS_ITEM, getString(R.string.marked_for_issue), barcode)
        }

        jsonString = gson.toJson(jsonData)
    }

    private fun updateItems(barcode: String) {

        Observable.just("racker")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    repo.markCompletedItem(taskId = mainTaskId, barcode = barcode, trayId = task.trayId!!, status = ItemStatus.LIVE)

                    updateItemsCount()
                    success()
                }
    }

    private fun checkForCompletion() {

        updateIssuesList()
        if ((scanned.size + issues.size + damagedBarcodes + missingBarcodes + shortQuantities) < total) {
            button_complete.visibility = View.GONE
            return
        }

        button_complete.visibility = View.VISIBLE
    }

    /*private fun launchBinScan() {
        val intent = Intent(this, CompletePutawayBinScanActivity::class.java)
        intent.putExtra("binId", binId)
        startActivityForResult(intent, SCAN_BIN_TRAY_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_BIN_TRAY_CODE) {
            completeCurrentLot()
        }
    }*/

    private fun completeCurrentLot() {

        showProgress()

        val short = getString(R.string.short_quantity)
        val damaged = getString(R.string.damaged_barcode)
        val missing = getString(R.string.missing_barcode)

        val reasons = mutableListOf<String>()
        for (i in 1..shortQuantities)
            reasons.add(short)

        for (i in 1..damagedBarcodes)
            reasons.add(damaged)

        for (i in 1..missingBarcodes)
            reasons.add(missing)

        shortQuantities = 0
        damagedBarcodes = 0
        missingBarcodes = 0

        val pending = barcodes.subtract(issues).subtract(scanned)
        pending.forEachIndexed { index, it ->
            repo.markIssueItem(taskId = mainTaskId, barcode = it, reason = reasons[index], status = ItemStatus.IN_ISSUE)
        }

        repo.refresh()

        Observable.just("racker")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val item = repo.getItemsByTaskUcode(mainTaskId, ucode!!)
                    complete(item)
                }
    }

    private fun complete(items: List<TaskItem>) {

        val created = items.filter { it.status == ItemStatus.CREATED.name }
        if (created.isNotEmpty()) {
            //Log.d("unable to process", "${created.joinToString()}")
            message("Unable to process. Inconsistent state with barcodes ${created.joinToString()} ")
            hideProgress()
            return
        }

        showProgress()
        val info = ProductLotLiveInfo(ucode = ucode!!, binId = binId!!, rackerTask = Task(id = task.taskId, items = items))
        service.completeBin(task.taskId!!, info)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> close(result) },
                        { error -> processError(error) }
                )
    }

    private fun close(startRacking: StartRacking) {
        hideProgress()

        if(startRacking.bin == null && startRacking.trayId == null){

            //complete_task_button.visibility = View.VISIBLE
            updateTask()
            return
        }

        if(startRacking.bin == binId && startRacking.trayId == trayId){
            repo.clearTasksForUser(SessionService.userId)
            getNextUcode()
            return
        }else if(startRacking.bin == binId && startRacking.trayId != trayId){
            repo.clearTasksForUser(SessionService.userId)
            scanTrayActivity(startRacking)
            return
        }else if(startRacking.bin != binId && startRacking.trayId != trayId){
            repo.clearTasksForUser(SessionService.userId)
            scanBinActivity(startRacking)
            return
        }else if(startRacking.bin != binId && startRacking.trayId == trayId){
            repo.clearTasksForUser(SessionService.userId)
            scanBinActivity(startRacking)
            return
        }

    }

    private fun getNextUcode(){
        showProgress()
        service.getItemToScan(trayId!!, binId!!)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> setNextUcode(result) },
                        { error -> processError(error) }

                )
    }

    private fun setNextUcode(task: Task){

        val taskId = repo.addTask(SessionService.userId, TaskType.PUTAWAY, task, trayId, task.referenceId)
        mainTaskId = taskId
        ucode = task.items[0].ucode

        retrieve()
        hideProgress()
    }

    private fun scanTrayActivity(startRacking: StartRacking){
        val intent = Intent(this, RackScanTrayActivity::class.java)
        intent.putExtra("binId", startRacking.bin)
        intent.putExtra("trayId", startRacking.trayId)
        intent.putExtra("ucode", startRacking.ucode)
        intent.putExtra("uCount", startRacking.ucodeScanRequired)
        startActivity(intent)
        finish()
    }

    private fun scanBinActivity(startRacking: StartRacking){
        val intent = Intent(this, RackScanBinActivity::class.java)
        intent.putExtra("binId", startRacking.bin)
        intent.putExtra("trayId", startRacking.trayId)
        intent.putExtra("ucode", startRacking.ucode)
        intent.putExtra("uCount", startRacking.ucodeScanRequired)
        startActivity(intent)
        finish()
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

    @SuppressLint("CheckResult")
    private fun updateTask() {

        repo.updateTaskStatus(mainTaskId, TaskStatus.COMPLETED)

        if (task.dnd > 0) {

            showProgress()
            userService.status(UserStatus(userId = SessionService.userId, action = BreakStatus.BREAK.value))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { _ -> updateDND() },
                            { error -> processError(error) }
                    )
        } else {
            repo.addEvent(eventOf(UserAction.TASK_COMPLETE, taskId.toString()))
            repo.clearItem()

            hideProgress()
            message(getString(R.string.task_completed))
            finish()
        }
    }

    private fun updateDND() {
        repo.addEvent(eventOf(UserAction.TASK_COMPLETE, taskId.toString()))
        repo.addEvent(eventOf(UserAction.BREAK_START, SessionService.userId))

        SessionService.breakStart = System.currentTimeMillis()
        SessionService.breakStatus = BreakStatus.BREAK

        repo.updateDND(mainTaskId, false)
        repo.clearItem()

        hideProgress()
        message(getString(R.string.task_completed))
        finish()
    }

    private fun populateIssuesList(items: List<TaskItem>) {

        val issues = items.filter { it.status == ItemStatus.IN_ISSUE.name }
        issues.forEach { item ->

            if (!issuesList.containsKey(item.returnReason)) {
                issuesList[item.returnReason!!] = hashSetOf()
            }

            issuesList[item.returnReason]!!.add(item.barCode!!)
        }

        updateIssuesList()
    }

    private fun updateIssuesList() {

        issues_list.removeAllViews()

        val inflater = LayoutInflater.from(this)
        issuesList.keys.forEach { key ->
            if (issuesList[key]!!.isNotEmpty()) {
                val view = getIssueItem(inflater, key, issuesList[key]!!.size)
                issues_list.addView(view)
            }
        }

        if (shortQuantities > 0) {
            val short = getIssueItem(inflater, getString(R.string.short_quantity), shortQuantities)
            issues_list.addView(short)
        }

        if (missingBarcodes > 0) {
            val missing = getIssueItem(inflater, getString(R.string.missing_barcode), missingBarcodes)
            issues_list.addView(missing)
        }

        if (damagedBarcodes > 0) {
            val damaged = getIssueItem(inflater, getString(R.string.damaged_barcode), damagedBarcodes)
            issues_list.addView(damaged)
        }
    }

    private fun getIssueItem(inflater: LayoutInflater, issueType: String, qty: Int): View {
        val view = inflater.inflate(R.layout.issue_item_row, null, false)
        val type = view.findViewById<TextView>(R.id.type)
        val quantity = view.findViewById<TextView>(R.id.quantity)

        type.text = issueType
        quantity.text = qty.toString()

        return view
    }
}
