package com.pharmeasy.fetchr.features.barcoder.presenter.contracts

import com.pharmeasy.fetchr.features.barcoder.view.BarcoderIssueItemView
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.main.presenter.BasePresenterImpl
import com.pharmeasy.fetchr.model.ProductLotItem

abstract class BarcoderIssueItemPresenter<V: BarcoderIssueItemView> : BasePresenterImpl<V>() {

    abstract fun getNextItem(item: ProductLotItem)

    abstract fun populateItem(taskItems: List<TaskItem>)
}