package com.pharmeasy.fetchr.features.barcoder.view

import com.pharmeasy.fetchr.main.view.BaseView
import com.pharmeasy.fetchr.main.view.ViewTasks

/**
 *Created by Ravi on 2019-08-20.
 */
interface FragmentView: BaseView, ViewTasks {

    fun openActivity()

    fun openMainActivity()
}