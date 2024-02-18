package com.pharmeasy.fetchr.features.barcoder.presenter

import android.annotation.SuppressLint
import android.content.Context
import com.pharmeasy.fetchr.features.barcoder.presenter.contracts.FragmentComletePresenter
import com.pharmeasy.fetchr.features.barcoder.view.FragmentView
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.BarcoderService
import com.pharmeasy.fetchr.type.TaskStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 *Created by Ravi on 2019-08-20.
 */
class FragmentCompleteTaskPresenterImpl(context: Context, val mainTaskId: Long, val isIssue: Boolean): FragmentComletePresenter<FragmentView>() {

    private val service by lazy {
        retroWithToken(BarcoderService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = context)
    }

    private lateinit var task: UserTask
    private lateinit var items: TaskItem

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

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

    override fun tasksOnViewResumed() {
        repo.refresh()

        task = repo.getTaskById(mainTaskId).first()
        items = repo.getItemsByTask(mainTaskId).first()
    }

    override fun tasksOnViewDestroyed() {
        compositeDisposable.clear()
    }

    private fun complete(){
        repo.updateTaskStatus(mainTaskId, TaskStatus.COMPLETED)

        getView().hideProgressBar()

        if(isIssue)
            getView().openActivity()
        else
            getView().openMainActivity()
    }
}