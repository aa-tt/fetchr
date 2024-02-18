package com.pharmeasy.fetchr.features.barcoder.view

import com.pharmeasy.fetchr.main.view.BaseView
import com.pharmeasy.fetchr.main.view.ViewTasks

interface BarcoderMedicineDetailsView : BaseView, ViewTasks {

    fun valid()

    fun launchPasteBarcodeActivity()

    fun launchBarcodeAdminActivity()

    fun bottomSheetWrongMedicine()

}