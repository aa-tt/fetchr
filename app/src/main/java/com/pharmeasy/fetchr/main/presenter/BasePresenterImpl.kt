package com.pharmeasy.fetchr.main.presenter

import com.pharmeasy.fetchr.main.presenter.contracts.BasePresenter
import com.pharmeasy.fetchr.main.view.BaseView
import java.lang.RuntimeException

abstract class BasePresenterImpl<V: BaseView>: BasePresenter<V> {

    private var mView: V? = null

    override fun getView(): V {
        return if (isViewAttached) {
            mView!!
        } else {
            throw MvpViewNotAttachedException()
        }
    }

    private val isViewAttached: Boolean
        get() = mView != null

    override fun attachView(view: V) {
        mView = view
    }

    override fun detachView() {
        mView = null
        tasksOnViewDestroyed()
    }

    class MvpViewNotAttachedException : RuntimeException("Please call Presenter.onAttach(mView) before requesting data to the Presenter")

}