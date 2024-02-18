package com.pharmeasy.fetchr.features.racker

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.adapter.PutawayAdapter
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.BinItem
import com.pharmeasy.fetchr.model.Task
import com.pharmeasy.fetchr.model.UserStatus
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.RackerService
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.service.UserService
import com.pharmeasy.fetchr.type.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.putaway_list_content.*
import kotlinx.android.synthetic.main.putaway_list_main.*

class PutawayActivity : BaseActivity() {

    private var mainTaskId: Long = -1
    private var taskId: Long = -1
    private var task: UserTask? = null

    private val service by lazy {
        retroWithToken(RackerService::class.java)
    }

    private val userService by lazy {
        retroWithToken(UserService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progress

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.putaway_list_main)

        title = getString(R.string.putaway_list)

        mainTaskId = intent.getLongExtra("mainTaskId", -1)
        taskId = intent.getLongExtra("taskId", -1)
        if (mainTaskId < 0 || taskId < 0) {
            error("Unable to process")
            finish()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        list.layoutManager = layoutManager

        complete_task.visibility = View.GONE
        complete_task.setOnClickListener {
            complete()
        }

        //raise issue
        raise_ticket_button.setOnClickListener{
            raiseTicket()
        }
    }

    override fun onResume() {
        super.onResume()

        reload()
    }

    private fun reload() {
        repo.refresh()
        val tasks = repo.getTaskById(mainTaskId)

        if (tasks.isNotEmpty())
            this.task = tasks.first()

        val items = repo.getItemsByTask(mainTaskId)
        populate(items)
    }

    private fun populate(taskItems: List<TaskItem>) {

        val grouped = taskItems.groupBy { it.binId }
        val lots = grouped.keys.map {

            val items = grouped[it]!!
            val ucodes = items.groupBy { it.ucode }
            val pending = items.any { it.processed < ProcessingStatus.SYNC.value }
            val status = if (pending) {
                if (items.size == 1)
                    translateStatus(items.first().status!!).name
                else
                    items.map { it.status }.reduce { x, y -> min(x, y) }
            } else
                ItemStatus.DONE.name
            BinItem(mainTaskId, it!!, ucodes.keys.size, items.size, ItemStatus.valueOf(status!!))
        }

        val adapter = PutawayAdapter(this, lots.sortedBy { it.binId }.sortedByDescending { it.status != ItemStatus.DONE }, taskId)

        list.adapter = adapter

        checkForCompletion(lots)
    }

    private fun min(x: String? = null, y: String? = null): String {

        val p = translateStatus(x!!)
        val q = translateStatus(y!!)

        return if (p.ordinal < q.ordinal) p.name else q.name
    }

    private fun translateStatus(status: String): ItemStatus {

        return when (status) {
            ItemStatus.CREATED.name -> ItemStatus.PENDING
            ItemStatus.LIVE.name, ItemStatus.IN_ISSUE.name -> ItemStatus.IN_PROGRESS
            else -> ItemStatus.valueOf(status)
        }
    }

    private fun checkForCompletion(items: List<BinItem>) {

        val pending = items.any { it.status == ItemStatus.PENDING || it.status == ItemStatus.IN_PROGRESS }
        if (!pending)
            complete_task.visibility = View.VISIBLE
    }

    private fun complete() {

        showProgress()
        val task = Task(id = taskId, status = TaskStatus.COMPLETED)

        service.completeTask()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { _ -> updateTask() },
                        { error -> processError(error) }
                )
    }

    private fun updateTask() {

        repo.updateTaskStatus(mainTaskId, TaskStatus.COMPLETED)

        if (task!!.dnd > 0) {

            userService.status(UserStatus(userId = SessionService.userId, action = BreakStatus.BREAK.value))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { _ -> updateDND() },
                            { error -> processError(error) }
                    )
        } else {
            repo.addEvent(eventOf(UserAction.TASK_COMPLETE, taskId.toString()))
            repo.clearItem()

            hideProgress()
            message(getString(R.string.task_completed))
            finish()
        }
    }

    private fun updateDND() {
        repo.addEvent(eventOf(UserAction.TASK_COMPLETE, taskId.toString()))
        repo.addEvent(eventOf(UserAction.BREAK_START, SessionService.userId))

        SessionService.breakStart = System.currentTimeMillis()
        SessionService.breakStatus = BreakStatus.BREAK

        repo.updateDND(mainTaskId, false)
        repo.clearItem()

        hideProgress()
        message(getString(R.string.task_completed))
        finish()
    }
}
