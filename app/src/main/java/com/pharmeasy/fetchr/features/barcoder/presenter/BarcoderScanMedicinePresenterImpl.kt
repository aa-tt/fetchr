package com.pharmeasy.fetchr.features.barcoder.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.pharmeasy.fetchr.features.barcoder.presenter.contracts.BarcoderScanMedicinePresenter
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderScanMedicineView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.Task
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.BarcoderService
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.type.TaskStatus
import com.pharmeasy.fetchr.type.TaskType
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class BarcoderScanMedicinePresenterImpl(context: Context) : BarcoderScanMedicinePresenter<BarcoderScanMedicineView>() {

    private val service by lazy {
        retroWithToken(BarcoderService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = context)
    }

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    private var mainTaskId: Long? = null
    private var taskId: Long = -1

    @SuppressLint("CheckResult")
    override fun getBarcoderTask(barcode: String, trayId: String) {
        getView().showProgressBar()

        compositeDisposable.add(service.barcoderTaskStarted(barcode, trayId)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->  processInvoice(result)},
                        { error ->  getView().showError(error)}

                )
        )
    }

    private fun processInvoice(task: Task) {

        Log.d("barcoder","$task")

        if (!task.referenceId.isNullOrEmpty())
            SessionService.referenceId = task.referenceId!!

        taskId = task.id!!.toLong()
        repo.addEvent(eventOf(UserAction.INVOICE_BARCODE_SCAN, taskId.toString()))

        mainTaskId = repo.addTask(user = SessionService.userId, type = TaskType.BARCODER, task = task, reference = task.referenceId)

        repo.updateTaskTray(mainTaskId!!, trayId = task.trayId)
        repo.updateTaskStatus(mainTaskId!!, TaskStatus.ASSIGNED)

        getView().hideProgressBar()
        getView().launchBarcodeMedicineDetailsActivity(mainTaskId!!)

    }

    override fun tasksOnViewResumed() {
        repo.refresh()
    }

    override fun tasksOnViewDestroyed() {
        compositeDisposable.clear()
    }
}