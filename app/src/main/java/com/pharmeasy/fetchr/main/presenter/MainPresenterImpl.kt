package com.pharmeasy.fetchr.main.presenter

import android.annotation.SuppressLint
import android.util.Log
import com.pharmeasy.fetchr.constants.PAGE_SIZE_COUNT
import com.pharmeasy.fetchr.constants.REFRESH_DEBOUNCE
import com.pharmeasy.fetchr.constants.STATUS
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.Event
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.main.presenter.contracts.MainPresenter
import com.pharmeasy.fetchr.main.view.MainView
import com.pharmeasy.fetchr.model.*
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.*
import com.pharmeasy.fetchr.type.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MainPresenterImpl(val repo: TaskRepoNew): MainPresenter<MainView>() {

    private var trayTaskList = mutableListOf<String>()

    private var pendingTask: UserTask? = null
    private var rackerTask: Task? = null

    private val userService by lazy {
        retroWithToken(UserService::class.java)
    }

    private val verifierService by lazy {
        retroWithToken(VerifierService::class.java)
    }

    private val rackerService by lazy {
        retroWithToken(RackerService::class.java)
    }

    private val barcoderService by lazy {
        retroWithToken(BarcoderService::class.java)
    }

    private val eventService by lazy {
        retroWithToken(EventService::class.java)
    }

    override fun tasksOnViewResumed() {
        repo.refresh()

        if (SessionService.breakStatus == BreakStatus.BREAK)
            getView().takeBreak()
        else
            checkPendingTasks()

        syncEvents()
    }

    private fun checkPendingTasks() {

        val tasks = repo.getTasksForUser(SessionService.userId)

        if (SessionService.role == racker_role) {
            racker()
        }

        if (tasks.isEmpty()) {
            getView().showNewTask()
        } else
            initializePendinTask(tasks.first())

    }

    private fun initializePendinTask(tasks: UserTask){
        pendingTask = tasks
        getView().showPendingTask(tasks, trayTaskList)
    }

    override fun refreshTask(): Boolean {
        when (SessionService.role) {
            verifier_role -> refreshVerifier()
            racker_role -> racker()
            barcoder_role -> refreshBarcoder()
            else -> return true
        }
        return true
    }

    @SuppressLint("CheckResult")
    private fun racker() {
        getView().showProgressBar()
        rackerService.getListOfTrays(PAGE_SIZE_COUNT, STATUS, SessionService.userId).subscribeOn(Schedulers.io())
                .debounce(REFRESH_DEBOUNCE, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { assigned ->
                            if (assigned != null) {

                                if (assigned.data.isNotEmpty()) {
                                    saveMultipleTask(assigned.data)
                                    initializePendinTask(assigned.data.first())
                                } else if (assigned.data.isEmpty() && pendingTask != null) {
                                    getView().showRackView()

                                    getTrayZone()
                                    repo.clearAllUserTasks()
                                    checkPendingTasks()
                                } else {
                                    getView().showRackView()

                                    getTrayZone()
                                    repo.clearAllUserTasks()
                                }

                                getView().hideProgressBar()
                            }

                        },
                        { error -> getView().showError(error) }
                )
    }

    @SuppressLint("CheckResult")
    private fun getAssignedVerifier() {
        verifierService.getAssignedTask().subscribeOn(Schedulers.io())
                .debounce(REFRESH_DEBOUNCE, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { assigned ->

                            if (assigned != null) {
                                val task = when (SessionService.role) {
                                    verifier_role -> assigned.verifierTask
                                    racker_role -> assigned.rackerTask
                                    else -> null
                                }

                                if (task != null) {
                                    repo.clearTasksForUser(SessionService.userId)
                                    pendingTask = null

                                    if (!task.referenceId.isNullOrEmpty())
                                        SessionService.referenceId = task.referenceId!!

                                    if (assigned.verifierTask != null && assigned.rackerTask != null) {
                                        rackerTask = assigned.rackerTask
                                    } else {
                                        rackerTask = null
                                    }

                                    task.trayId = task.trayId
                                            ?: if (SessionService.role == verifier_role) assigned.rackerTask?.trayId else assigned.rackerIssue?.trayId
                                    repo.addEvent(eventOf(UserAction.TASK_ASSIGNED, task.id.toString()))
                                    saveTask(task, assigned.verifierIssue?.trayId)

                                }

                                getView().hideProgressBar()
                            }

                        },
                        { error -> getView().showError(error) }
                )
    }

    @SuppressLint("CheckResult")
    private fun getAssignedBarcoder() {
        barcoderService.getAssignedTask(TaskType.BARCODER.name).subscribeOn(Schedulers.io())
                .debounce(REFRESH_DEBOUNCE, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { assigned ->

                            if (assigned != null) {
                                val task = when (SessionService.role) {
                                    barcoder_role -> assigned.verifierTask
                                    racker_role -> assigned.rackerTask
                                    else -> null
                                }

                                if (task != null) {
                                    repo.clearTasksForUser(SessionService.userId)
                                    pendingTask = null

                                    if (!task.referenceId.isNullOrEmpty())
                                        SessionService.referenceId = task.referenceId!!

                                    if (assigned.verifierTask != null && assigned.rackerTask != null) {
                                        rackerTask = assigned.rackerTask
                                    } else {
                                        rackerTask = null
                                    }

                                    task.trayId = task.trayId
                                            ?: if (SessionService.role == verifier_role) assigned.rackerTask?.trayId else assigned.rackerIssue?.trayId
                                    repo.addEvent(eventOf(UserAction.TASK_ASSIGNED, task.id.toString()))
                                    saveTask(task, assigned.verifierIssue?.trayId)

                                }

                                getView().hideProgressBar()
                            }

                        },
                        { error -> getView().showError(error) }
                )
    }

    @SuppressLint("CheckResult")
    private fun refreshVerifier() {
        getView().showProgressBar()
        verifierService.getAssignedTask().subscribeOn(Schedulers.io())
                .debounce(REFRESH_DEBOUNCE, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { assigned ->

                            if (assigned != null) {
                                val task = when (SessionService.role) {
                                    verifier_role -> assigned.verifierTask
                                    racker_role -> assigned.rackerTask
                                    else -> null
                                }

                                if (task != null && (pendingTask == null || task.id != pendingTask!!.taskId)) {

                                    repo.clearTasksForUser(SessionService.userId)
                                    pendingTask = null

                                    if (!task.referenceId.isNullOrEmpty())
                                        SessionService.referenceId = task.referenceId!!

                                    task.trayId = task.trayId
                                            ?: if (SessionService.role == verifier_role) assigned.rackerTask?.trayId else assigned.rackerIssue?.trayId
                                    repo.addEvent(eventOf(UserAction.TASK_ASSIGNED, task.id.toString()))
                                    saveTask(task, assigned.verifierIssue?.trayId)

                                } else if (task == null && pendingTask != null) {
                                    repo.clearTasksForUser(SessionService.userId)
                                    pendingTask = null
                                    checkPendingTasks()

                                } else if (task == null && pendingTask == null) {
                                    repo.clearTasksForUser(SessionService.userId)
                                    checkPendingTasks()
                                } else {
                                    val tasks = repo.getTasksForUser(SessionService.userId)
                                    task!!.trayId = task.trayId
                                            ?: if (SessionService.role == verifier_role) assigned.rackerTask?.trayId else assigned.rackerIssue?.trayId
                                    if (tasks.isNotEmpty())
                                        repo.updateTask(tasks.first()._id!!, task)

                                    if (assigned.verifierTask != null && assigned.rackerTask != null) {
                                        rackerTask = assigned.rackerTask
                                    } else {
                                        rackerTask = null
                                    }

                                    repo.refresh()
                                    checkPendingTasks()
                                }

                                getView().hideProgressBar()
                            }

                        },
                        { error -> getView().showError(error) }


                )
    }

    @SuppressLint("CheckResult")
    private fun refreshBarcoder() {
        getView().showProgressBar()
        barcoderService.getAssignedTask(TaskType.BARCODER.name).subscribeOn(Schedulers.io())
                .debounce(REFRESH_DEBOUNCE, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { assigned ->

                            if (assigned != null) {
                                val task = when (SessionService.role) {
                                    barcoder_role -> assigned.verifierTask
                                    racker_role -> assigned.rackerTask
                                    else -> null
                                }

                                if (task != null && (pendingTask == null || task.id != pendingTask!!.taskId)) {

                                    repo.clearTasksForUser(SessionService.userId)
                                    pendingTask = null

                                    if (!task.referenceId.isNullOrEmpty())
                                        SessionService.referenceId = task.referenceId!!

                                    task.trayId = task.trayId
                                            ?: if (SessionService.role == verifier_role) assigned.rackerTask?.trayId else assigned.rackerIssue?.trayId
                                    repo.addEvent(eventOf(UserAction.TASK_ASSIGNED, task.id.toString()))
                                    saveTask(task, assigned.verifierIssue?.trayId)

                                } else if (task == null && pendingTask != null) {
                                    repo.clearTasksForUser(SessionService.userId)
                                    pendingTask = null
                                    checkPendingTasks()

                                } else if (task == null && pendingTask == null) {
                                    repo.clearTasksForUser(SessionService.userId)
                                    checkPendingTasks()
                                } else {
                                    val tasks = repo.getTasksForUser(SessionService.userId)
                                    task!!.trayId = task.trayId
                                            ?: if (SessionService.role == verifier_role) assigned.rackerTask?.trayId else assigned.rackerIssue?.trayId
                                    if (tasks.isNotEmpty())
                                        repo.updateTask(tasks.first()._id!!, task)

                                    if (assigned.verifierTask != null && assigned.rackerTask != null) {
                                        rackerTask = assigned.rackerTask
                                    } else {
                                        rackerTask = null
                                    }

                                    repo.refresh()
                                    checkPendingTasks()
                                }

                                getView().hideProgressBar()
                            }

                        },
                        { error -> getView().showError(error) }


                )
    }


    override fun checkAssignedTask() {
        getView().showProgressBar()

        when (SessionService.role) {
            verifier_role -> getAssignedVerifier()
            racker_role -> racker()
            barcoder_role -> getAssignedBarcoder()
            else -> return
        }
    }

    override fun saveTask(task: Task, issueTrayId: String?) {
        val user = SessionService.userId
        val type = when {
            SessionService.role == racker_role -> TaskType.PUTAWAY
            SessionService.role == barcoder_role -> TaskType.BARCODER
            task.sourceType == Source.INVOICE -> TaskType.VERIFICATION
            task.sourceType == Source.RECTIFICATION -> TaskType.VERIFICATION // rectification
            task.sourceType == Source.SALE_RETURN -> TaskType.VERIFICATION
            task.sourceType == Source.VERIFIER_ISSUE || task.sourceType == Source.RACKER_ISSUE -> TaskType.ISSUE_VERIFICATION
            else -> TaskType.UNKNOWN
        }

        if (type == TaskType.UNKNOWN) {
            getView().showMessage("Received unknown task of type ${task.sourceType}")
            return
        }

        val taskId = repo.addTask(user, type, task, task.trayId, reference = task.referenceId, issueTrayId = issueTrayId)
        val tasks = repo.getTaskById(taskId)

        if (tasks.isNotEmpty())
            initializePendinTask(tasks.first())

        getView().hideProgressBar()
    }

    private fun saveMultipleTask(userTask: List<UserTask>) {

        trayTaskList.clear()

        if (userTask.first().status == TaskStatus.ASSIGNED.name)
            repo.clearAllUserTasks()

        userTask.forEach {
            it.type = TaskType.PUTAWAY.name

            if (it.status == TaskStatus.ASSIGNED.name) {
                repo.addUserList(it)
            }

            trayTaskList.add(it.status!!)
        }

        Log.d("", "${repo.getTrayList()}")
        if (trayTaskList.isNotEmpty())
            initializePendinTask(userTask.first())

    }

    @SuppressLint("CheckResult")
    private fun getNextBinTray() {
        rackerService.startRacking(picked_status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> getView().setNextBin(result) },
                        { error -> getView().showError(error) }
                )
    }

    @SuppressLint("CheckResult")
    private fun getTrayZone() {
        getView().showProgressBar()
        rackerService.getTrayZone()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> getView().setTrayZone(result) },
                        { error -> getView().showError(error) }
                )
    }

    @SuppressLint("CheckResult")
    override fun onLogoutClicked() {
        userService.logout(UserStatus(SessionService.userId, "logout"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { getView().launchLoginActivity() },
                        { error -> getView().showError(error) }
                )
    }

    override fun selfAssignment() {

        if (pendingTask != null) {
            val type = TaskType.valueOf(pendingTask!!.type!!)
            if (type == TaskType.PUTAWAY && trayTaskList.contains(TaskStatus.PICKED.name)) {
                getNextBinTray()
                return
            }
        }

        getView().launchPutawayMultipleTrayStep()
    }


    override fun tasksOnViewDestroyed() {

    }

    override fun verifyAndSignOut() {
        val tasks = repo.getTasksForUser(SessionService.userId)

        if (tasks.isEmpty())
            onLogoutClicked()
        else {
            val task = tasks.first()
            if (task.status == TaskStatus.CREATED.name || task.status == TaskStatus.ASSIGNED.name || task.status == TaskStatus.SIDELINED_ASSIGNED.name)
                onLogoutClicked()
            else
                getView().showMessage("Task is in ${task.status} state. Can't signout")
        }
    }

    override fun syncEvents() {
        val items = repo.getPendingEvents()
        saveEvents(items)
    }

    @SuppressLint("CheckResult")
    private fun saveEvents(events: List<Event>) {

        if (events.isEmpty())
            return

        eventService.events(events)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { repo.markEvents() },
                        { error -> getView().showError(error) }
                )
    }

    override fun barcoder() {
    }
}