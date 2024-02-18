package com.pharmeasy.fetchr.features.racker

import android.content.Intent
import android.os.Bundle
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.picked_status
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.RackerService
import com.pharmeasy.fetchr.type.TaskStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class StartPutawayTrayScanActivity() : ScanActivity() {

    private var mainTaskId: Long = -1
    private var taskId: Long = -1
    private var trayId: String? = null
    private val service by lazy {
        retroWithToken(RackerService::class.java)
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

        if (id != trayId!!.trim()) {
            failure("Scanned Tray# $id doesn't match with task")
            return
        }

        showProgress()
        processing()

        service.markTrayAsPicked(trayId!!, picked_status)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { _ -> trayScanned() },
                        { error -> processError(error) }
                )

    }

    private fun trayScanned() {

        repo.updateTaskStatus(mainTaskId, TaskStatus.PICKED)
        repo.addEvent(eventOf(UserAction.TRAY_SCAN, trayId.toString()))

        hideProgress()
        success("")
    }

    override fun next() {
        showPutawayList()
    }

    private fun showPutawayList() {

        val intent = Intent(this, PutawayActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("taskId", taskId)

        startActivity(intent)
        finish()
    }

}
