package com.pharmeasy.fetchr.features.barcoder.view

import com.pharmeasy.fetchr.main.view.BaseView
import com.pharmeasy.fetchr.main.view.ViewTasks

interface BarcoderScanIssueMedicineView : BaseView, ViewTasks {

    fun success()

    fun failure( text: String)

    fun handleStatus(text: String, status: Boolean)

    fun showStatus()

    fun showScanner()

    fun startTimer()

}
