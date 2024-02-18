package com.pharmeasy.fetchr.features.verifier

import android.content.Intent
import android.os.Bundle
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.in_progress_status
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.VerifierService
import com.pharmeasy.fetchr.type.TaskStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class VerificationIssueTrayScanActivity() : ScanActivity() {

    private var mainTaskId: Long = -1
    private var taskId: Long = -1
    private var trayId: String? = null

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
        trayId = intent.getStringExtra("trayId")

        if (mainTaskId < 0 || taskId < 0 || trayId == null) {
            error("Unable to process")
            finish()
        }

        title = "${getString(R.string.scan_tray)}# $trayId"
    }

    override fun title(): String = this.getString(R.string.scan_tray)

    override fun processScannedItem(id: String) {

        if(!id.startsWith("TR")){
            failure(getString(R.string.invalid_tray))
            return
        }

        showProgress()
        processing()

        if (trayId!!.trim() != id) {
            failure("Wrong tray scanned")
            return
        }

        service.markTaskStarted(taskId, in_progress_status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { _ -> processStatus() },
                        { error -> processError(error) }
                )
    }

    override fun next() {

        launchTrayScan()
    }

    private fun processStatus() {

        repo.updateTaskTray(mainTaskId, null)
        repo.updateTaskStatus(mainTaskId, TaskStatus.IN_PROGRESS)
        repo.addEvent(eventOf(UserAction.VERIFIER_ISSUE_TRAY_SCAN, taskId.toString()))

        hideProgress()
        success("Tray# $trayId")
    }

    private fun launchTrayScan() {

        val intent = Intent(this, VerificationTrayScanActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("taskId", taskId)

        startActivity(intent)
        finish()
    }
}
