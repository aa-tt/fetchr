package com.pharmeasy.fetchr.main.view

import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.StartRacking

/**
 *Created by Ravi on 2019-07-30.
 */
interface MainView: BaseView, ViewTasks {

    fun launchLoginActivity()

    fun showMessage(messageString: String)

    fun launchPutawayMultipleTrayStep()

    fun showPendingTask(task: UserTask, userTaskList: MutableList<String>)

    fun setTrayZone(userTask: UserTask)

    fun takeBreak()

    fun showNewTask()

    fun setNextBin(startRacking: StartRacking)

    fun showRackView()

    fun launchBarcoderScanTrayActivity()
}