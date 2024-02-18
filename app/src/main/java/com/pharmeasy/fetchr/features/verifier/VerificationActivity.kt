package com.pharmeasy.fetchr.features.verifier

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import com.google.gson.Gson
import com.pharmeasy.fetchr.BuildConfig
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.adapter.VerificationAdapter
import com.pharmeasy.fetchr.constants.ADD_ISSUE_TRAY_CODE
import com.pharmeasy.fetchr.constants.PAGE_SIZE
import com.pharmeasy.fetchr.event.ItemSelectedListener
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.greendao.model.VerifierItem
import com.pharmeasy.fetchr.model.*
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.processingStatusOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.service.UserService
import com.pharmeasy.fetchr.service.VerifierService
import com.pharmeasy.fetchr.type.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.verification_list_content.*
import kotlinx.android.synthetic.main.verification_list_main.*
import java.io.Serializable

class VerificationActivity() : BaseActivity(), ItemSelectedListener {


    private var mainTaskId: Long = -1
    private var taskId: Long = -1
    private var trayId: String? = null
    private var task: UserTask? = null
    private var pageCount: Int = 0

    private var jsonString: String? = null
    private lateinit var gson: Gson
    private lateinit var jsonData: JsonData

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
        get() = progress

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.verification_list_main)

        title = getString(R.string.verification_list)

        gson = Gson()
        jsonData = JsonData()

        mainTaskId = intent.getLongExtra("mainTaskId", -1)
        taskId = intent.getLongExtra("taskId", -1)
        trayId = intent.getStringExtra("trayId")

        if (mainTaskId < 0 || taskId < 0 || trayId == null) {
            error("Unable to process")
            finish()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        list.layoutManager = layoutManager

        complete_task.visibility = View.GONE
        complete_task.setOnClickListener {
            completeTask()
        }

        //raise issue
        raise_ticket_button.setOnClickListener{
            raiseTicket()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tray, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.trays -> launchTraysList()
            R.id.filter -> launchFilter()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        reload()
    }

    private fun reload() {
        repo.refresh()
        val tasks = repo.getTaskById(mainTaskId)

        if (tasks.isNotEmpty())
            this.task = tasks.first()

        val items = repo.getItemsByTask(mainTaskId)

        val verifyItem = repo.getVerifierItemsByTask(mainTaskId)
        populate(items, verifyItem)
    }

    private fun populate(taskItems: List<TaskItem>, verifyItem: List<VerifierItem>) {
        //Log.d("VA", "${taskItems.size}")

        if (task != null) {
            current_tray.text = task!!.trayId
            if (task!!.reference != null) {
                tran_panel.visibility = View.VISIBLE
                tran_no.text = task!!.reference
            } else {
                tran_panel.visibility = View.GONE
            }
        }

        val grouped = taskItems.groupBy {
            ProductLotItem(id = mainTaskId, name = it.name!!, ucode = it.ucode, packForm = it.packForm
                    ?: "", batchNumber = it.batchNumber!!, expiry = it.expiryDate!!, mrp = it.mrp!!, status = ItemStatus.PENDING)

        }
        val lots = grouped.keys.map {

            val items = grouped[it]!!
            val pending = items.any { it.processed < ProcessingStatus.SAVED.value }
            val status = if (pending) {
                if (items.size == 1)
                    translateStatus(items.first().status!!).name
                else
                    items.map { it.status }.reduce { x, y -> min(x, y) }
            } else
                ItemStatus.DONE.name
            ProductLotItem(id = mainTaskId, taskId = task!!.taskId!!, name = it.name, ucode = it.ucode, packForm = it.packForm, batchNumber = it.batchNumber, expiry = it.expiry, mrp = it.mrp, status = ItemStatus.valueOf(status!!))
        }

        val verificationAdapter = VerificationAdapter(this, lots.sortedBy { it.name }.sortedByDescending { it.status != ItemStatus.DONE }, this)
        list.adapter = verificationAdapter

        val hasIssues = taskItems.any { it.status == ItemStatus.COMPLETED_WITH_ISSUE.name || it.status == ItemStatus.IN_ISSUE.name } || verifyItem.any { it.status == ItemStatus.IN_ISSUE.name }


        checkForCompletion(lots, hasIssues)
    }

    private fun min(x: String? = null, y: String? = null): String {

        val p = translateStatus(x!!)
        val q = translateStatus(y!!)

        return if (p.ordinal < q.ordinal) p.name else q.name
    }

    private fun translateStatus(status: String): ItemStatus {

        return when (status) {
            ItemStatus.CREATED.name -> ItemStatus.PENDING
            ItemStatus.READY_FOR_PUTAWAY.name, ItemStatus.IN_ISSUE.name, ItemStatus.IN_PROGRESS.name -> ItemStatus.IN_PROGRESS
            else -> ItemStatus.valueOf(status)
        }
    }

    private fun launchTraysList(): Boolean {
        val intent = Intent(this, VerificationTraysActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        startActivity(intent)

        return true
    }

    private fun launchFilter(): Boolean {
        val intent = Intent(this, VerificationFilterActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("taskId", taskId)
        startActivity(intent)

        return true
    }


    private fun checkForCompletion(lotItems: List<ProductLotItem>, hasIssues: Boolean) {

        val pending = lotItems.any { it.status == ItemStatus.PENDING || it.status == ItemStatus.IN_PROGRESS }
        if (!pending) {
            complete_task.visibility = View.VISIBLE

            complete_task.text = if (hasIssues && task!!.issueTrayId == null) getString(R.string.add_issue_tray) else getString(R.string.complete_task)
        }
    }

    private fun completeTask() {

        val it = repo.getVerifierItemsByTask(mainTaskId)
        val items = repo.getItemsByTask(mainTaskId)

        completeItems(items = it, listItems = items)

    }

    private fun completeItems(items: List<VerifierItem>, listItems: List<TaskItem>) {

        val hasIssues = items.any { it.status == ItemStatus.IN_ISSUE.name } || listItems.any { it.status == ItemStatus.COMPLETED_WITH_ISSUE.name }

        if (hasIssues) {
            if (task!!.issueTrayId != null) {
                items.filter { it.status == ItemStatus.IN_ISSUE.name }.forEach { it.trayId = task!!.issueTrayId }
                complete(items)
            } else
                launchAddIssueTray()
        }else {
            val itemsWithoutIssue = repo.getItemsByTaskAndBetweenStatus(mainTaskId, ProcessingStatus.MARKED, ProcessingStatus.SAVED)
            complete(itemsWithoutIssue)
        }
    }

    private fun launchAddIssueTray() {

        val intent = Intent(this, AddIssueTrayActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("taskId", taskId)

        startActivityForResult(intent, ADD_ISSUE_TRAY_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode != Activity.RESULT_CANCELED && requestCode == ADD_ISSUE_TRAY_CODE) {

            val it = repo.getItemsByTaskAndBetweenStatus(mainTaskId, ProcessingStatus.MARKED, ProcessingStatus.SAVED)
            complete(items = it)
        }
    }

    private fun complete(items: List<VerifierItem>) {
        //Log.d("VerificationActivity", " Items Size on Complete:: \n${items.size} and items \n${items}")

        showProgress()
        val task = VerifierTask(id = taskId, status = TaskStatus.COMPLETED, items = items)
        service.completeTask(taskId, task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { _ ->
                            val hasIssues = items.any { it.status == ItemStatus.IN_ISSUE.name }
                            updateTask(hasIssues)
                        },
                        { error -> processError(error) }
                )
    }

    private fun updateTask(hasIssues: Boolean) {

        jsonData.taskId = taskId
        jsonData.version = BuildConfig.VERSION_CODE.toString()
        jsonString = gson.toJson(jsonData)

        if (hasIssues)
            repo.addEvent(eventOf(UserAction.ISSUE_RAISED, taskId.toString()))

        repo.updateTaskStatus(mainTaskId, TaskStatus.COMPLETED)

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

        repo.updateDND(mainTaskId, false)
        repo.clearVerifyItem()

        message(getString(R.string.task_completed))
        hideProgress()
        finish()
    }

    private fun launchVerifyItemActivity(item: ProductLotItem) {
        pageCount = 0
        val intent = Intent(this, VerifyItemActivity::class.java)
        intent.putExtra("data", item as Serializable)
        startActivity(intent)
        finish()
    }

    //fetching next items based on paginated
    private fun nextTaskItem(item: ProductLotItem) {

        service.nextItem(item.taskId!!, item.ucode, item.batchNumber, pageCount, PAGE_SIZE)
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
            launchVerifyItemActivity(item)
        }
    }

    override fun onItemSelected(item: ProductLotItem) {
        val nextItem = repo.getVerifierItemsByTaskAndUCode(item.id, item.ucode, item.batchNumber)
        if (nextItem.isEmpty()) {
            showProgress()
            nextTaskItem(item)
        } else
            launchVerifyItemActivity(item)
    }
}
