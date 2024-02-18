package com.pharmeasy.fetchr.features.verifier

import android.os.Bundle
import android.util.Log
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.AssignedTray
import com.pharmeasy.fetchr.model.NewTray
import com.pharmeasy.fetchr.model.VerifierTask
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.VerifierService
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.ProcessingStatus
import com.pharmeasy.fetchr.type.TaskStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class AddTrayActivity : ScanActivity() {

    private var mainTaskId: Long = -1
    private var taskId: Long = -1
    private var ucode: String? = null
    private var batchno: String? = null

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
        ucode = intent.getStringExtra("ucode")
        batchno = intent.getStringExtra("batchno")
        if (mainTaskId < 0 || taskId < 0) {
            error("Unable to process")
            finish()
        }
        //Log.d("idsssss", "$ucode $mainTaskId $taskId")
    }

    override fun title(): String = this.getString(R.string.add_new_tray)

    override fun processScannedItem(id: String) {

        if (!id.startsWith("TR")) {
            failure(getString(R.string.invalid_tray))
            return
        }

        showProgress()
        processing()

        val items = repo.getAddItemsByTaskAndBetweenStatus(mainTaskId, ProcessingStatus.MARKED, ProcessingStatus.SAVED)

        items.forEach {
            Log.d("ATA", "${it.ucode} ${it.batchNumber} ${it.status} ${it.trayId} ${it.returnReason} ${it.processed}")
        }

        val task = VerifierTask(id = taskId, status = TaskStatus.IN_PROGRESS, items = items)
        val request = NewTray(id, task)
        service.addNewTray(taskId, request)
                .debounce(5000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> processStatus(result) },
                        { error -> processError(error) }
                )
    }

    override fun next() {
        finish()
    }

    private fun processStatus(assigned: AssignedTray) {

        repo.updateVerifyTaskProcessingStatusByStatus(mainTaskId, ProcessingStatus.SYNC, ItemStatus.READY_FOR_PUTAWAY)
        repo.updateVerifyTaskProcessingStatusByStatus(mainTaskId, ProcessingStatus.SYNC, ItemStatus.IN_ISSUE)

        repo.updateTaskProcessingStatusByStatus(mainTaskId, ProcessingStatus.SYNC, ItemStatus.READY_FOR_PUTAWAY)
        repo.updateTaskProcessingStatusByStatus(mainTaskId, ProcessingStatus.SYNC, ItemStatus.IN_ISSUE)

        repo.updateTaskTray(mainTaskId, assigned.trayId)
        repo.addEvent(eventOf(UserAction.ADD_TRAY_SCAN, assigned.trayId))

        hideProgress()
        success("Tray# ${assigned.trayId}")

    }
}
