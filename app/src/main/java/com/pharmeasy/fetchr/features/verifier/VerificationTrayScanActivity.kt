package com.pharmeasy.fetchr.features.verifier

import android.content.Intent
import android.os.Bundle
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.AssignedTray
import com.pharmeasy.fetchr.model.TaskTray
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.VerifierService
import com.pharmeasy.fetchr.type.Source
import com.pharmeasy.fetchr.type.TaskStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class VerificationTrayScanActivity() : ScanActivity() {

    private var mainTaskId: Long = -1
    private var taskId: Long = -1
    private lateinit var trayId: String

    private val service by lazy {
        retroWithToken(VerifierService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        mainTaskId = intent.getLongExtra("mainTaskId", -1)
        taskId = intent.getLongExtra("taskId", -1)

        if (mainTaskId < 0 || taskId < 0) {
            error("Unable to process")
            finish()
        }
    }

    override fun title(): String = this.getString(R.string.scan_tray_for_items)

    override fun processScannedItem(id: String) {

        if(!id.startsWith("TR")){
            failure(getString(R.string.invalid_tray))
            return
        }

        showProgress()
        processing()

        val tasks = repo.getTaskById(mainTaskId)
        getTrayPickedZone(tasks.first())

        val task = tasks.first()
        val reference = task.reference!!
        service.addInitialTray(TaskTray(sourceId = taskId.toString(), sourceType = Source.VERIFIER_TASK, trayId = id, referenceId = reference, referenceType = task.referenceType, trayPickedZone = getTrayPickedZone(task)))
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> processStatus(result) },
                        { error -> processError(error) }
                )
    }

    override fun next() {

        showVerificationList()
    }

    private fun processStatus(assigned: AssignedTray) {

        repo.updateTaskTray(mainTaskId, trayId = assigned.trayId)
        repo.addEvent(eventOf(UserAction.ADD_TRAY_SCAN, assigned.trayId))

        trayId = assigned.trayId

        hideProgress()
        success("Items Tray# ${assigned.trayId}")
    }

    private fun showVerificationList() {

        val intent = Intent(this, VerificationActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("taskId", taskId)
        intent.putExtra("trayId", trayId)

        startActivity(intent)
        finish()
    }

    private fun getTrayPickedZone(userTask: UserTask): String?{
        return when {
            userTask.referenceType == Source.RECTIFICATION.name -> TaskStatus.AUDITOR_ZONE.name
            userTask.referenceType == Source.AUDITOR_TASK.name -> TaskStatus.AUDITOR_ZONE.name
            userTask.referenceType == Source.INVOICE_JIT.name -> TaskStatus.JIT_ZONE.name
            userTask.referenceType == Source.INVOICE_PROCUREMENT.name -> TaskStatus.PROCUREMENT_ZONE.name
            userTask.referenceType == Source.SALE_RETURN.name -> TaskStatus.SALE_RETURN_ZONE.name
            else -> null
        }

    }
}
