package com.pharmeasy.fetchr.features.verifier

import android.content.Intent
import android.os.Bundle
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.*
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.service.VerifierService
import com.pharmeasy.fetchr.type.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class VerificationInvoiceScanActivity() : ScanActivity() {

    private var mainTaskId: Long? = null
    private var taskId: Long = -1
    private lateinit var trayId: String
    private var item: ProductLotItem? = null

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    private val service by lazy {
        retroWithToken(VerifierService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        trayId = intent.getStringExtra("trayId")
    }

    override fun title(): String = this.getString(R.string.scan_medicine)

    override fun processScannedItem(id: String) {

        if(id.startsWith("TR")){
            failure(getString(R.string.invalid_barcode))
            return
        }

        processing()
        showProgress()

        service.markTaskStartedV2(id, in_progress_status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> processInvoice(result) },
                        { error -> processError(error) }
                )
    }

    override fun next() {
        showScanTray()
    }

    private fun valid(id: String): Boolean {

        val no = id.toIntOrNull()
        return (no != null)
    }

    private fun processInvoice(task: Task) {

        if (!task.referenceId.isNullOrEmpty())
            SessionService.referenceId = task.referenceId!!

        taskId = task.id!!.toLong()
        repo.addEvent(eventOf(UserAction.INVOICE_BARCODE_SCAN, taskId.toString()))

        mainTaskId = repo.addTask(user = SessionService.userId, type = TaskType.VERIFICATION, task = task, reference = task.referenceId)

        setTrayForTask()

    }

    private fun setTrayForTask() {
        val tasks = repo.getTaskById(mainTaskId!!)

        val task = tasks.first()
        val reference = task.reference!!
        service.addInitialTray(TaskTray(sourceId = taskId.toString(), sourceType = Source.VERIFIER_TASK, trayId = trayId, referenceId = reference, referenceType = task.referenceType, trayPickedZone = getTrayPickedZone(task)))
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> processStatus(result) },
                        { error -> processError(error) }
                )
    }

    private fun processStatus(assigned: AssignedTray) {

        repo.updateTaskTray(mainTaskId!!, trayId = assigned.trayId)
        repo.addEvent(eventOf(UserAction.ADD_TRAY_SCAN, assigned.trayId))

        trayId = assigned.trayId

        hideProgress()

      /*  val tasks = repo.getTaskById(mainTaskId!!)

        val task = tasks.first()

        if (task.referenceType == TaskStatus.JIT_ZONE.name)*/
            success("Items Tray# ${assigned.trayId}")

    }

    private fun showScanTray() {
        val tasks = repo.getTaskById(mainTaskId!!)

        val task = tasks.first()
        var intent: Intent? = null
        intent = if (getTrayPickedZone(task) == TaskStatus.PROCUREMENT_ZONE.name) {
            Intent(this, VerifyItemActivity::class.java)
        } else {
            Intent(this, VerificationActivity::class.java)
        }

        intent.putExtra("mainTaskId", mainTaskId!!)
        intent.putExtra("taskId", taskId)
        intent.putExtra("trayId", trayId)
        startActivity(intent)
        finish()
    }

    private fun getTrayPickedZone(userTask: UserTask): String {
        return when {
            userTask.referenceType == Source.RECTIFICATION.name -> TaskStatus.AUDITOR_ZONE.name
            userTask.referenceType == Source.INVOICE_JIT.name -> TaskStatus.JIT_ZONE.name
            userTask.referenceType == Source.INVOICE_PROCUREMENT.name -> TaskStatus.PROCUREMENT_ZONE.name
            userTask.referenceType == Source.SALE_RETURN.name -> TaskStatus.SALE_RETURN_ZONE.name
            else -> ""
        }

    }
}
