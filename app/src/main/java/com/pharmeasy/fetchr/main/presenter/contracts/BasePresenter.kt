package com.pharmeasy.fetchr.main.presenter.contracts

interface BasePresenter<T> {

    fun attachView(view: T)

    fun detachView()

    fun getView():T

    fun tasksOnViewResumed()

    fun tasksOnViewDestroyed()

}