package com.pharmeasy.fetchr.features.barcoder.view

import com.pharmeasy.fetchr.greendao.model.BarcoderItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.main.view.BaseView
import com.pharmeasy.fetchr.main.view.ViewTasks
import com.pharmeasy.fetchr.model.ProductLotItem

interface BarcoderIssueItemView : BaseView, ViewTasks {

    fun showIssueQtyBottomSheet()

    fun populate(items: List<BarcoderItem>, userTask: UserTask)

    fun updateInfo(productLot: ProductLotItem)
}