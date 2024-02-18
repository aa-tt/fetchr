package com.pharmeasy.fetchr.features.barcoder.presenter.contracts

import com.pharmeasy.fetchr.features.barcoder.view.BarcoderIssueTrayView
import com.pharmeasy.fetchr.main.presenter.BasePresenterImpl

abstract class BarcoderIssueTrayPresenter <V: BarcoderIssueTrayView> : BasePresenterImpl<V>() {

    abstract fun launchIssueMedicineActivity(id: String)

    abstract fun scanIssueTray(id: String)

}