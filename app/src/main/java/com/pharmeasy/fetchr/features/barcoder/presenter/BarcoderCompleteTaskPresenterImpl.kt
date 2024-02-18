package com.pharmeasy.fetchr.features.barcoder.presenter

import android.annotation.SuppressLint
import android.content.Context

import android.util.Log
import com.pharmeasy.fetchr.adapter.BarcoderCompleteTaskIssueListAdapter
import com.pharmeasy.fetchr.constants.ISSUE_STATUS
import com.pharmeasy.fetchr.constants.PAGE_SIZE
import com.pharmeasy.fetchr.constants.PAGE_SIZE_COUNT
import com.pharmeasy.fetchr.constants.STATUS
import com.pharmeasy.fetchr.features.barcoder.presenter.contracts.BarcoderCompleteTaskPresenter
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderCompleteTaskView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.BarcoderItem
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.BarcoderItemTask
import com.pharmeasy.fetchr.model.BarcoderNextItemTask
import com.pharmeasy.fetchr.model.BarcoderTasks
import com.pharmeasy.fetchr.model.Task
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.BarcoderService
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.ProcessingStatus
import com.pharmeasy.fetchr.type.TaskStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class BarcoderCompleteTaskPresenterImpl(context: Context, val mainTaskId: Long, val showIssueItems: Boolean): BarcoderCompleteTaskPresenter<BarcoderCompleteTaskView>(){

    private val service by lazy {
        retroWithToken(BarcoderService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = context)
    }

    private var pageCount: Int = 0

    private lateinit var task: UserTask
    private lateinit var items: MutableList<TaskItem>
    private lateinit var barcodeItems: MutableList<BarcoderItem>
    private var newBarcodeItems = hashSetOf<TaskItem>()

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun tasksOnViewResumed() {
        repo.refresh()

        task = repo.getTaskById(mainTaskId).first()
        items = repo.getItemsByTask(mainTaskId)
        barcodeItems = repo.getBarcoderItemsByTask(mainTaskId)

        getView().getTaskItem(items.first(), items, barcodeItems, task.trayId)

        getAllItemsByTask()
        //if(showIssueItems)
            //getView().getIssueItemByList(barcodeItems.filter { it.status == ItemStatus.IN_ISSUE.name })
    }

    override fun tasksOnViewDestroyed() {
        compositeDisposable.clear()
    }

    @SuppressLint("CheckResult")
    override fun markCompleteTask() {
        getView().showProgressBar()

        compositeDisposable.add(service.completeTask(task.taskId)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->  complete()},
                        { error ->  getView().showError(error)}

                )
        )
    }

    private fun getAllItemsByTask() {
        getView().showProgressBar()

        compositeDisposable.add(service.getTask(task.reference, ISSUE_STATUS,"DETAIL", pageCount, PAGE_SIZE)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> close(result)},
                        { error ->  getView().showError(error)}

                )
        )
    }

    private fun close(nextItem: BarcoderItemTask) {
        newBarcodeItems.clear()
        if(nextItem.data.isNotEmpty()) {
            if (nextItem.hasNext) {
                nextItem.data.forEach{
                    it.items.forEach{i ->
                        newBarcodeItems.add(i)
                        repo.updateBarcodeItems(mainTaskId, ProcessingStatus.SYNC, ItemStatus.IN_ISSUE, i.barCode)
                    }

                }

                pageCount++
                getAllItemsByTask()
                return
            } else {
                nextItem.data.forEach{
                    it.items.forEach{i ->
                        newBarcodeItems.add(i)
                        repo.updateBarcodeItems(mainTaskId, ProcessingStatus.SYNC, ItemStatus.IN_ISSUE, i.barCode)
                    }
                }
                showIssueItems()
            }
        }


        getView().hideProgressBar()

    }

    private fun showIssueItems(){

        val groupBy = newBarcodeItems.groupBy { it.returnReason }
        val lots = groupBy.keys.map {
            val item = groupBy[it]
            Log.d("showIssueItems", "${item!!.size}, $it")

            BarcoderTasks(reason = it, quantity = item.size)
        }

        getView().getIssueItemByList(lots, newBarcodeItems.toList(), barcodeItems)
    }

    private fun complete(){

        repo.updateTaskStatus(mainTaskId, TaskStatus.COMPLETED)

        getView().hideProgressBar()
        getView().onFinish()
    }
}