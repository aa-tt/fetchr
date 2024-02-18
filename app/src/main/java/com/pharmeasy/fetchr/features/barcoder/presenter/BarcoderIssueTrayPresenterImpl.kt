package com.pharmeasy.fetchr.features.barcoder.presenter

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.pharmeasy.fetchr.features.barcoder.presenter.contracts.BarcoderIssueTrayPresenter
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderIssueTrayView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.BarcoderItem
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.AssignedTray
import com.pharmeasy.fetchr.model.BarcoderIssueTask
import com.pharmeasy.fetchr.model.BarcoderTask
import com.pharmeasy.fetchr.model.Task
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.BarcoderService
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.ProcessingStatus
import com.pharmeasy.fetchr.type.TaskStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class BarcoderIssueTrayPresenterImpl(context: Context, val mainTaskId: Long) : BarcoderIssueTrayPresenter<BarcoderIssueTrayView>() {

    private val TAG = BarcoderIssueTrayPresenterImpl::class.java.name

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    private var trayId: String? = null
    
    private val service by lazy {
        retroWithToken(BarcoderService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = context)
    }

    private lateinit var task: UserTask

    override fun tasksOnViewResumed() {
        repo.refresh()
        task = repo.getTaskById(mainTaskId).first()
    }

    override fun scanIssueTray(id: String) {

        getView().showProgressBar()

        val items = repo.getAddBarcoderItemsByTaskAndBetweenStatus(mainTaskId, ProcessingStatus.MARKED, ProcessingStatus.SAVED)
        val task = BarcoderIssueTask(verifierTaskId = task.taskId,trayId = id, referenceType = task.referenceType,
                referenceId = task.reference,  items = setReturnReason(items))

        compositeDisposable.add(service.addIssueTray(task)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {result -> processStatus(result)},
                        { error ->  getView().showError(error)}

                )
        )
    }

    private fun setReturnReason(items: List<BarcoderItem>): List<BarcoderItem>{
        items.forEach {
            it.status = ItemStatus.CREATED.name
        }
        Log.d(TAG, "$items")

        repo.refresh()
        return items
    }

    override fun launchIssueMedicineActivity(id: String) {
        if(!id.startsWith("TR")) {
            getView().failure("Invalid Tray")
            return
        }

        getView().success()
        scanIssueTray(id)
    }

    private fun processStatus(assigned: Task) {

        repo.updateTaskProcessingStatusByStatus(mainTaskId, ProcessingStatus.SYNC, ItemStatus.READY_FOR_PUTAWAY)
        repo.updateTaskProcessingStatusByStatus(mainTaskId, ProcessingStatus.SYNC, ItemStatus.IN_ISSUE)

        repo.updateBarcodeTaskProcessingStatusByStatus(mainTaskId, ProcessingStatus.SYNC, ItemStatus.READY_FOR_PUTAWAY)
        repo.updateBarcodeTaskProcessingStatusByStatus(mainTaskId, ProcessingStatus.SYNC, ItemStatus.IN_ISSUE)


        repo.updateTaskTray(mainTaskId, assigned.trayId)
        repo.addEvent(eventOf(UserAction.ADD_TRAY_SCAN, assigned.trayId!!))

        getView().hideProgressBar()
        getView().intentForIssueMedicine(assigned.trayId!!)

    }

    override fun tasksOnViewDestroyed() {
        compositeDisposable.clear()
    }
}