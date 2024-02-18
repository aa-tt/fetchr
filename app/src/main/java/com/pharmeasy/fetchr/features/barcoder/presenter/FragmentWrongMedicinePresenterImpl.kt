package com.pharmeasy.fetchr.features.barcoder.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import com.pharmeasy.fetchr.features.barcoder.presenter.contracts.FragmentComletePresenter
import com.pharmeasy.fetchr.features.barcoder.presenter.contracts.FragmentWrongMedicinePresenter
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderAdminActivity
import com.pharmeasy.fetchr.features.barcoder.view.FragmentView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.BarcoderTask
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.BarcoderService
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.TaskStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class FragmentWrongMedicinePresenterImpl(context: Context, val mainTaskId: Long): FragmentWrongMedicinePresenter<FragmentView>() {

    private val service by lazy {
        retroWithToken(BarcoderService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = context)
    }

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    private lateinit var task: UserTask
    private lateinit var items: MutableList<TaskItem>

    @SuppressLint("CheckResult")
    override fun markWrongMedicine() {
        getView().showProgressBar()

        val task = BarcoderTask(id = task.taskId, status = TaskStatus.COMPLETED, items = setReturnReason("WRONG MEDICINE"))
        compositeDisposable.add(service.addAllIssues(task)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->  complete()},
                        { error ->  getView().showError(error)}

                )
        )
    }

    private fun setReturnReason(returnReason: String): List<TaskItem>{
        items.forEach {
            it.returnReason = returnReason
            it.status = ItemStatus.CREATED.name
            repo.updateItemByTaskId(returnReason, mainTaskId)
        }

        Log.d("wrongMedicine", "$items")

        repo.refresh()
        return items
    }


    override fun tasksOnViewResumed() {
        repo.refresh()

        task = repo.getTaskById(mainTaskId).first()
        items = repo.getItemsByTask(mainTaskId)
    }

    override fun tasksOnViewDestroyed() {
        compositeDisposable.clear()
    }

    private fun complete(){
        repo.updateTaskStatus(mainTaskId, TaskStatus.COMPLETED)

        getView().hideProgressBar()
        getView().openActivity()
    }
}