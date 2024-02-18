package com.pharmeasy.fetchr.features.barcoder.presenter.contracts

import com.pharmeasy.fetchr.features.barcoder.view.BarcoderScanIssueMedicineView
import com.pharmeasy.fetchr.main.presenter.BasePresenterImpl

abstract class BarcoderScanIssueMedicinePresenter<V: BarcoderScanIssueMedicineView> : BasePresenterImpl<V>() {

    abstract fun launchBarcodePastebarcodeActivity(id: String)

    abstract fun scanIssueTray(id: String)
}