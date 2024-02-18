package com.pharmeasy.fetchr.main.presenter.contracts

import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.main.presenter.BasePresenterImpl
import com.pharmeasy.fetchr.main.view.MainView
import com.pharmeasy.fetchr.model.Task

abstract class MainPresenter<V: MainView>: BasePresenterImpl<V>() {

    abstract fun onLogoutClicked()

    abstract fun selfAssignment()

    abstract fun verifyAndSignOut()

    abstract fun refreshTask():Boolean

    abstract fun checkAssignedTask()

    abstract fun saveTask(task: Task, issueTrayId: String?)

    abstract fun syncEvents()

    abstract fun barcoder()
}