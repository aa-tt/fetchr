package com.pharmeasy.fetchr.features.barcoder.presenter

import android.content.Context
import com.pharmeasy.fetchr.constants.PAGE_SIZE
import com.pharmeasy.fetchr.features.barcoder.presenter.contracts.BarcoderIssueItemPresenter
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderIssueItemView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.BarcoderNextItemTask
import com.pharmeasy.fetchr.model.ProductLotItem
import com.pharmeasy.fetchr.retro.processingStatusOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.BarcoderService
import com.pharmeasy.fetchr.type.ItemStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class BarcoderIssueItemPresenterImpl(private val context: Context, val mainTaskId: Long, val taskId: Long): BarcoderIssueItemPresenter<BarcoderIssueItemView>() {

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    private var pageCount: Int = 0

    private lateinit var productLot: ProductLotItem


    private val service by lazy {
        retroWithToken(BarcoderService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = context)
    }

    override fun tasksOnViewResumed() {
        repo.refresh()
        val tasks = repo.getTaskById(productLot.id)

        val items = repo.getBarcoderItemsByTask(mainTaskId)
        getView().populate(items, tasks.first())
    }

    override fun getNextItem(item: ProductLotItem) {
        compositeDisposable.add(service.nextItem(taskId!!, item.ucode, item.batchNumber, pageCount, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> close(result, item) },
                        { error -> getView().showError(error) }
                )
        )
    }

    override fun populateItem(taskItems: List<TaskItem>) {
        val taskItem = taskItems[0]


        productLot = ProductLotItem(id = mainTaskId, taskId = taskItem.taskId!!, name = taskItem.name!!, ucode = taskItem.ucode, packForm = taskItem.packForm
                ?: "", batchNumber = taskItem.batchNumber!!, expiry = taskItem.expiryDate!!, mrp = taskItem.mrp!!, status = ItemStatus.PENDING)

        val nextItem = repo.getBarcoderItemsByTaskAndUCode(mainTaskId, taskItem.ucode, taskItem.batchNumber)
        if (nextItem.isEmpty()) {
            getNextItem(productLot)
        }else
            tasksOnViewResumed()

        getView().updateInfo(productLot)
    }

    override fun tasksOnViewDestroyed() {
        compositeDisposable.clear()
    }

    private fun close(nextItem: BarcoderNextItemTask, item: ProductLotItem) {
        if (nextItem.hasNext) {
            nextItem.data.forEach { it ->
                it.taskId = item.id
                it.processed = processingStatusOf(it.status!!) // processed status changes
                it.batchNumber = item.batchNumber
                it.expiryDate = item.expiry
                it.mrp = item.mrp
                it.name = item.name
                it.packForm = item.packForm
                repo.addBarcoderItem(it)
            }
            pageCount++
            getNextItem(item)
            return
        } else {
            nextItem.data.forEach { it ->
                it.taskId = item.id
                it.processed = processingStatusOf(it.status!!) // processed status changes
                it.batchNumber = item.batchNumber
                it.expiryDate = item.expiry
                it.mrp = item.mrp
                it.name = item.name
                it.packForm = item.packForm
                repo.addBarcoderItem(it)
            }
            getView().hideProgressBar()
            tasksOnViewResumed()
        }
    }
}