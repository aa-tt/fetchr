package com.pharmeasy.fetchr.features.barcoder.view

import com.pharmeasy.fetchr.greendao.model.BarcoderItem
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.main.view.BaseView
import com.pharmeasy.fetchr.main.view.ViewTasks
import com.pharmeasy.fetchr.model.BarcoderTasks

interface BarcoderCompleteTaskView: BaseView, ViewTasks {

    fun onFinish()

    fun getTaskItem(item: TaskItem, items: List<TaskItem>, barcode: List<BarcoderItem>, trayId: String)

    fun getIssueItemByList(items: List<BarcoderTasks>, taskItems: List<TaskItem>, barcoderItems: List<BarcoderItem>)
}