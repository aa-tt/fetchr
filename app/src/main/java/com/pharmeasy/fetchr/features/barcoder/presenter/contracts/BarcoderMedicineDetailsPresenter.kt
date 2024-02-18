package com.pharmeasy.fetchr.features.barcoder.presenter.contracts

import com.pharmeasy.fetchr.features.barcoder.view.BarcoderMedicineDetailsView
import com.pharmeasy.fetchr.main.presenter.BasePresenterImpl

abstract class BarcoderMedicineDetailsPresenter  <V: BarcoderMedicineDetailsView> : BasePresenterImpl<V>() {

    abstract fun validateNearExpiry(): Boolean

    abstract fun validateWrongBarcode(barcode: String, mrp: String): Boolean

    abstract fun batchValidationCompare() : String

    /**
     * mark Status IN-PROGRESS
     */
    abstract fun markStatus()

    /**
     * mark NearExpiry/Wrong barcode
     */
    abstract fun markNearExpiry()

    abstract fun batchValidation(batchNo: String): Boolean


}