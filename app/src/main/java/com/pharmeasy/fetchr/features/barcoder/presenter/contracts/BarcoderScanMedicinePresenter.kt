package com.pharmeasy.fetchr.features.barcoder.presenter.contracts

import com.pharmeasy.fetchr.features.barcoder.view.BarcoderScanMedicineView
import com.pharmeasy.fetchr.main.presenter.BasePresenterImpl

abstract class BarcoderScanMedicinePresenter <V: BarcoderScanMedicineView> : BasePresenterImpl<V>() {

    abstract fun getBarcoderTask(barcode: String, trayId: String)

}