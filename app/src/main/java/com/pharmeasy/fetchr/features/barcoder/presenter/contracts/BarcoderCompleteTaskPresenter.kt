package com.pharmeasy.fetchr.features.barcoder.presenter.contracts

import com.pharmeasy.fetchr.features.barcoder.view.BarcoderCompleteTaskView
import com.pharmeasy.fetchr.main.presenter.BasePresenterImpl

abstract class BarcoderCompleteTaskPresenter <V: BarcoderCompleteTaskView> : BasePresenterImpl<V>(){

    abstract fun markCompleteTask()

}