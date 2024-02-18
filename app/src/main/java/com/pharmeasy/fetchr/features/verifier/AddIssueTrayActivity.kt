package com.pharmeasy.fetchr.features.verifier

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.AssignedTray
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.VerifierService
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AddIssueTrayActivity() : ScanActivity() {

    private var mainTaskId: Long = -1
    private var taskId: Long = -1

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

    override fun title(): String = this.getString(R.string.add_issue_tray)

    override fun processScannedItem(id: String) {

        if(!id.startsWith("TR")){
            failure(getString(R.string.invalid_tray))
            return
        }

        showProgress()
        processing()

        val tasks = repo.getTaskById(mainTaskId)

        val task = tasks.first()
        val reference = task.reference!!
        service.addIssueTray(AssignedTray(trayId = id, verifierTaskId = taskId, referenceId = reference, referenceType = task.referenceType))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { _ -> updateIssueTray(id) },
                        { error -> processError(error) }
                )
    }

    private fun updateIssueTray(trayId: String) {

        repo.updateVeriTrayForStatus(mainTaskId, trayId, ItemStatus.IN_ISSUE)
        repo.addEvent(eventOf(UserAction.ISSUE_TRAY_SCAN, trayId))

        hideProgress()
        success("Tray# $trayId")

    }

    override fun next() {
        setResult(Activity.RESULT_OK, Intent())
        finish()
    }
}
