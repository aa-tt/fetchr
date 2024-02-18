package com.pharmeasy.fetchr.features.barcoder.presenter

import android.content.Context
import android.util.Log
import com.pharmeasy.fetchr.features.barcoder.presenter.contracts.BarcoderScanIssueMedicinePresenter
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderScanIssueMedicineView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.BarcoderIssueTask
import com.pharmeasy.fetchr.model.BarcoderTask
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.BarcoderService
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.ProcessingStatus
import com.pharmeasy.fetchr.type.TaskStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class BarcoderScanIssueMedicinePresenterImpl(context: Context, val mainTaskId: Long, val returnReason: String) : BarcoderScanIssueMedicinePresenter<BarcoderScanIssueMedicineView>(){

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    private val service by lazy {
        retroWithToken(BarcoderService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = context)
    }

    private lateinit var task: UserTask
    private lateinit var items: MutableList<TaskItem>

    override fun tasksOnViewResumed() {
        repo.refresh()

        task = repo.getTaskById(mainTaskId).first()
        items = repo.getItemsByTask(mainTaskId)

    }

    override fun scanIssueTray(id: String) {

        val items = repo.getAddBarcoderItemsByTaskAndBetweenStatus(mainTaskId, ProcessingStatus.MARKED, ProcessingStatus.SAVED)
        val task = BarcoderIssueTask(id = task.taskId, status = TaskStatus.COMPLETED, items = items)

        compositeDisposable.add(service.addIssueTray(task)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {},
                        { error ->  getView().showError(error)}

                )
        )
    }

    override fun launchBarcodePastebarcodeActivity(id: String) {

    }

    override fun tasksOnViewDestroyed() {
        compositeDisposable.clear()
    }
}