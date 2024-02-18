package com.pharmeasy.fetchr.features.verifier

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
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
import com.pharmeasy.fetchr.constants.ADD_ISSUE_TRAY_CODE
import com.pharmeasy.fetchr.constants.PAGE_SIZE
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.*
import com.pharmeasy.fetchr.model.*
import com.pharmeasy.fetchr.retro.*
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.service.UserService
import com.pharmeasy.fetchr.service.VerifierService
import com.pharmeasy.fetchr.type.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.scan_content.*
import kotlinx.android.synthetic.main.verify_item_content.*
import kotlinx.android.synthetic.main.verify_item_main.*
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class VerifyItemActivity() : ScannerBaseActivity() {

    private val TAG = VerifyItemActivity::class.java.simpleName

    private var max_time = 2
    private lateinit var scanner: CodeScanner
    private lateinit var task: UserTask
    private lateinit var productLot: ProductLotItem

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

    private var count = 1
    private var jsonString: String? = null

    private lateinit var gson: Gson
    private lateinit var jsonData: JsonData

    private var isCameraReleaseOrNot: Boolean = true

    private val sdf_from = SimpleDateFormat("yyyy-MM-dd")
    private val sdf_to = SimpleDateFormat("MM/yy")

    private var mainTaskId: Long? = null
    private var taskId: Long? = null
    private var pageCount: Int = 0

    private val issueTypes by lazy {
        resources.getStringArray(R.array.verifier_issue_types)
    }

    private val service by lazy {
        retroWithToken(VerifierService::class.java)
    }

    private val userService by lazy {
        retroWithToken(UserService::class.java)
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
        setContentView(R.layout.verify_item_main)

        title = getString(R.string.scan_medicines)

        gson = Gson()
        jsonData = JsonData()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            if (canGoBack())
                finish()
        }

        raise_issue.setOnClickListener {
            showIssueTypes()
        }

        add_tray.setOnClickListener {
            addTray()
        }

        complete.setOnClickListener {
            UpdateCompletedItemAsync().execute();
        }

        show_issues.setOnClickListener {

            if (showing_issues) {
                scanned_panel.visibility = View.VISIBLE
                issues_panel.visibility = View.INVISIBLE
            } else {
                scanned_panel.visibility = View.INVISIBLE
                issues_panel.visibility = View.VISIBLE
            }

            showing_issues = !showing_issues
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

        if (intent.extras != null) {
            mainTaskId = intent.getLongExtra("mainTaskId", 0)
        }

        if (intent.extras != null && intent.extras.containsKey("data")) {
            productLot = intent.extras.get("data") as ProductLotItem
            taskId = productLot.taskId
            updateInfo()
        } else {

            taskId = intent.getLongExtra("taskId", 0)
            val items = repo.getItemsByTask(mainTaskId!!)
            populateItem(items)
        }

        jsonData.ucode = productLot.ucode
        jsonData.taskId = productLot.taskId
        jsonData.version = BuildConfig.VERSION_CODE.toString()


        //raise issue
        raise_ticket_button.setOnClickListener {
            raiseTicket()
        }
    }

    override fun onBackPressed() {

        if (canGoBack())
            super.onBackPressed()
    }

    private fun canGoBack(): Boolean {

        if (damagedBarcodes > 0 || missingBarcodes > 0 || shortQuantities > 0) {
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

    private fun updateInfo() {

        name.text = "${productLot.ucode} - ${productLot.name} ${productLot.packForm}"
        batch.text = productLot.batchNumber
        mrp.text = productLot.mrp.toString()

        val date = sdf_from.parse(productLot.expiry)
        expiry.text = sdf_to.format(date)

        scanned_items.text = "0"
        issue_items.text = "0"
        pending_items.text = "0"
    }

    private fun showIssueTypes() {

        selectedIssue = -1

        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.issue_type)
                .setSingleChoiceItems(R.array.verifier_issue_types, -1, { _, which -> selectedIssue = which })
                .setPositiveButton(R.string.continu, { _, _ -> onIssueTypeSelected() })
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
                .setPositiveButton(R.string.ok, { _, _ -> onShortQuantitySelected(quantityPicker.value) })
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
                .setPositiveButton(R.string.ok, { _, _ -> onMissingMedicineQuantitySelected(quantityPicker.value) })
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
        pending_items.text = (total - scanned.size - issues.size - damagedBarcodes - missingBarcodes - shortQuantities).toString()
        issue_items.text = (issues.size + damagedBarcodes + missingBarcodes + shortQuantities).toString()

        checkForCompletion()
    }

    private fun scanIssueMedicines(type: String) {
        val intent = Intent(this, VerifyIssueItemActivity::class.java)
        intent.putExtra("data", productLot as Serializable)
        intent.putExtra("type", type)
        intent.putExtra("damagedBarcodes", damagedBarcodes + missingBarcodes + shortQuantities)

        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        scanner.isFlashEnabled = false
        retrieve()
    }

    override fun onPause() {
        isCameraReleaseOrNot = true
        scanner.releaseResources()
        scanner.stopPreview()
        super.onPause()
    }

    private fun retrieve() {
        repo.refresh()

        val tasks = repo.getTaskById(productLot.id)
        this.task = tasks.first()

        val nextItem = repo.getVerifierItemsByTaskAndUCode(productLot.id, productLot.ucode, productLot.batchNumber)
        populate(nextItem)

        if (task.referenceType == Source.RECTIFICATION.name)
            raise_issue.visibility = View.GONE
    }

    private fun populate(items: List<VerifierItem>) {

        Log.d("SIA", items.size.toString())
        /*items.forEach {
            Log.d("SIA", "${it.ucode} ${it.batchNumber} ${it.barCode} ${it.status} ${it.trayId} ${it.returnReason} ${it.processed}")
        }*/

        tray.text = task.trayId

        barcodes = items.filter { it.barCode != null }.map { it.barCode!! }.toHashSet()
        scanned = items.filter { it.status == ItemStatus.READY_FOR_PUTAWAY.name }.map { it.barCode!! }.toHashSet()
        issues = items.filter { it.status == ItemStatus.IN_ISSUE.name }.map { it.barCode!! }.toHashSet()

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
                repo.addEvent(eventOf(UserAction.TASK_START, task.taskId.toString(), jsonString))

            Observable.just(NEW_OBSERVEABLE)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        repo.markVerifierCompletedItem(taskId = productLot.id, barcode = barcode, trayId = task.trayId!!, status = ItemStatus.READY_FOR_PUTAWAY)
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

    private fun checkForCompletion() {

        updateIssuesList()

        if ((scanned.size + issues.size + damagedBarcodes + missingBarcodes + shortQuantities) < total) {
            add_tray.visibility = View.VISIBLE
            complete.visibility = View.GONE
            return
        }

        val it = repo.getVerifierItemsByTask(mainTaskId!!)
        val items = repo.getItemsByTask(mainTaskId!!)

        val hasIssues = items.any { it.status == ItemStatus.COMPLETED_WITH_ISSUE.name || it.status == ItemStatus.IN_ISSUE.name } || it.any { it.status == ItemStatus.IN_ISSUE.name }

        add_tray.visibility = View.GONE
        complete.visibility = View.VISIBLE

        complete.text = if (hasIssues && task.issueTrayId == null) getString(R.string.scan_issue_tray) else getString(R.string.complete)

    }

    private fun completeItem() {

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

            repo.markVeriferIssueItem(taskId = productLot.id, barcode = it, reason = reasons[index], status = ItemStatus.IN_ISSUE)
        }

        if (pending.isNotEmpty())
            repo.markTaskIssueItem(taskId = productLot.id, ucode = productLot.ucode, batch = productLot.batchNumber, trayId = task.trayId!!, status = ItemStatus.IN_ISSUE)
        else
            repo.markCompletedUcode(taskId = productLot.id, ucode = productLot.ucode, batch = productLot.batchNumber, trayId = task.trayId!!, status = ItemStatus.READY_FOR_PUTAWAY)

        repo.updateTaskProcessingStatusByStatusProcessed(productLot.id, ProcessingStatus.SAVED, ItemStatus.READY_FOR_PUTAWAY, ProcessingStatus.MARKED)
        repo.updateTaskProcessingStatusByStatusProcessed(productLot.id, ProcessingStatus.SAVED, ItemStatus.IN_ISSUE, ProcessingStatus.MARKED)

    }

    private fun addTray() {
        repo.refresh()
        val items = repo.getItemsByTaskAndBetweenStatus(productLot.id, ProcessingStatus.MARKED, ProcessingStatus.SAVED)

        val scanned = items.filter { it.status == ItemStatus.READY_FOR_PUTAWAY.name }
        if (scanned.isEmpty())
            error(getString(R.string.current_tray_empty))
        else
            launchAddTray()
    }

    private fun launchAddTray() {
        val intent = Intent(this, AddTrayActivity::class.java)
        intent.putExtra("mainTaskId", task._id)
        intent.putExtra("taskId", task.taskId)
        intent.putExtra("ucode", productLot.ucode)
        intent.putExtra("batchno", productLot.batchNumber)
        startActivity(intent)
    }

    private fun populateIssuesList(items: List<VerifierItem>) {

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


    override fun onBarcodeScanned(barcode: String) {
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
            if (task.referenceType == Source.INVOICE_PROCUREMENT.name)
                completeTask()
            else
                startVerificationActivity()
        }
    }

    private fun startVerificationActivity() {
        val intent = Intent(this, VerificationActivity::class.java)
        intent.putExtra("mainTaskId", task._id)
        intent.putExtra("taskId", task.taskId)
        intent.putExtra("trayId", task.trayId)
        startActivity(intent)
        finish()
    }

    private fun populateItem(taskItems: List<TaskItem>) {
        val taskItem = taskItems[0]

        productLot = ProductLotItem(id = mainTaskId!!, taskId = taskId!!, name = taskItem.name!!, ucode = taskItem.ucode, packForm = taskItem.packForm
                ?: "", batchNumber = taskItem.batchNumber!!, expiry = taskItem.expiryDate!!, mrp = taskItem.mrp!!, status = ItemStatus.PENDING)

        val nextItem = repo.getVerifierItemsByTaskAndUCode(productLot.id, productLot.ucode, productLot.batchNumber)
        if (nextItem.isEmpty()) {
            nextTaskItem(productLot)
        }else
            retrieve()

        updateInfo()
    }

    //fetching next items based on paginated
    private fun nextTaskItem(item: ProductLotItem) {

        showProgress()
        service.nextItem(taskId!!, item.ucode, item.batchNumber, pageCount, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> close(result, item) },
                        { error -> processError(error) }
                )
    }

    private fun close(nextItem: NextItemTask, item: ProductLotItem) {
        if (nextItem.hasNext) {
            nextItem.data.forEach { it ->
                it.taskId = item.id
                it.processed = processingStatusOf(it.status!!) // processed status changes
                it.batchNumber = item.batchNumber
                repo.addVerifierItem(it)
            }
            pageCount++
            nextTaskItem(item)
            return
        } else {
            nextItem.data.forEach { it ->
                it.taskId = item.id
                it.processed = processingStatusOf(it.status!!) // processed status changes
                it.batchNumber = item.batchNumber
                repo.addVerifierItem(it)
            }
            hideProgress()
            retrieve()
        }
    }

    private fun completeTask() {

        val it = repo.getVerifierItemsByTask(mainTaskId!!)
        val items = repo.getItemsByTask(mainTaskId!!)

        completeItems(items = it, listItems = items)

    }

    private fun completeItems(items: List<VerifierItem>, listItems: List<TaskItem>) {

        val hasIssues = items.any { it.status == ItemStatus.IN_ISSUE.name } || listItems.any { it.status == ItemStatus.COMPLETED_WITH_ISSUE.name }

        if (hasIssues) {
            if (task!!.issueTrayId != null) {
                items.filter { it.status == ItemStatus.IN_ISSUE.name }.forEach { it.trayId = task.issueTrayId }
                complete(items)
            } else
                launchAddIssueTray()
        }else {
            val itemsWithoutIssue = repo.getItemsByTaskAndBetweenStatus(mainTaskId!!, ProcessingStatus.MARKED, ProcessingStatus.SAVED)
            complete(itemsWithoutIssue)
        }
    }

    private fun complete(items: List<VerifierItem>) {
        Log.d("VerificationActivity", " Items Size on Complete:: \n$taskId and items \n$items")

        showProgress()
        val task = VerifierTask(id = taskId, status = TaskStatus.COMPLETED, items = items)
        service.completeTask(taskId!!, task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { val hasIssues = items.any { it.status == ItemStatus.IN_ISSUE.name }
                          updateTask(hasIssues)
                        },
                        { error -> processError(error) }
                )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode != Activity.RESULT_CANCELED && requestCode == ADD_ISSUE_TRAY_CODE) {

            val it = repo.getItemsByTaskAndBetweenStatus(mainTaskId!!, ProcessingStatus.MARKED, ProcessingStatus.SAVED)
            complete(items = it)
        }
    }

    private fun launchAddIssueTray() {
        val intent = Intent(this, AddIssueTrayActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("taskId", taskId)
        startActivityForResult(intent, ADD_ISSUE_TRAY_CODE)
    }

    private fun updateTask(hasIssues: Boolean) {

        jsonData.taskId = taskId
        jsonData.version = BuildConfig.VERSION_CODE.toString()
        jsonString = gson.toJson(jsonData)

        if (hasIssues)
            repo.addEvent(eventOf(UserAction.ISSUE_RAISED, taskId.toString()))

        repo.updateTaskStatus(mainTaskId!!, TaskStatus.COMPLETED)

        if (task!!.dnd > 0) {
            userService.status(UserStatus(userId = SessionService.userId, action = BreakStatus.BREAK.value))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { _ -> updateDND() },
                            { error -> processError(error) }
                    )
        } else {
            repo.addEvent(eventOf(UserAction.TASK_COMPLETE, taskId.toString(), jsonString))
            repo.clearVerifyItem()
            message(getString(R.string.task_completed))
            hideProgress()
            finish()
        }
    }

    private fun updateDND() {

        repo.addEvent(eventOf(UserAction.TASK_COMPLETE, taskId.toString(), jsonString))
        repo.addEvent(eventOf(UserAction.BREAK_START, SessionService.userId))

        SessionService.breakStart = System.currentTimeMillis()
        SessionService.breakStatus = BreakStatus.BREAK

        repo.updateDND(mainTaskId!!, false)
        repo.clearVerifyItem()

        message(getString(R.string.task_completed))
        hideProgress()
        finish()
    }
}
