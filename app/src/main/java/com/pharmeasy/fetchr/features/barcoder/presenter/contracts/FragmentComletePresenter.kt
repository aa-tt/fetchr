package com.pharmeasy.fetchr.features.barcoder.presenter.contracts

import com.pharmeasy.fetchr.features.barcoder.view.FragmentView
import com.pharmeasy.fetchr.main.presenter.BasePresenterImpl

abstract class FragmentComletePresenter <V: FragmentView> : BasePresenterImpl<V>(){

    abstract fun markCompleteTask()
}