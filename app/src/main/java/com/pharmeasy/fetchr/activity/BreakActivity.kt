package com.pharmeasy.fetchr.activity

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.UserStatus
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.retro.toBreakStatus
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.service.UserService
import com.pharmeasy.fetchr.type.BreakStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.break_content.*
import kotlinx.android.synthetic.main.break_main.*
import java.util.*

class BreakActivity() : BaseActivity() {

    private val timer = Timer("timer")

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progress

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    private val service by lazy {
        retroWithToken(UserService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.break_main)
        title = getString(R.string.take_break)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            if (canGoBack())
                finish()
        }

        activate_break.setOnClickListener {
            activateBreak()
        }

        stop_break.setOnClickListener { stopBreak() }
    }

    override fun onResume() {
        super.onResume()

        if (SessionService.breakStatus == BreakStatus.BREAK) {
            showBreak()
            startBreak()
        } else {

            val tasks = repo.getTasksForUser(SessionService.userId)

            if (tasks.isEmpty())
                showFree()
            else {
                val task = tasks.first()
                if(task.dnd > 0)
                    showScheduled()
                else
                    showFree()
            }
        }
    }

    override fun onBackPressed() {
        if (canGoBack())
            finish()
    }

    private fun canGoBack(): Boolean {
        return SessionService.breakStatus != BreakStatus.BREAK
    }

    private fun activateBreak() {

        val tasks = repo.getTasksForUser(SessionService.userId)

        if (tasks.isEmpty())
            activateBreakNow()
        else {
            val task = tasks.first()
            scheduleBreakLater(task._id!!)
        }
    }

    private fun activateBreakNow(){

        SessionService.breakStart = System.currentTimeMillis()
        showProgress()

        service.status(UserStatus(userId = SessionService.userId, action = BreakStatus.BREAK.value))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { _ ->
                            repo.addEvent(eventOf(UserAction.BREAK_START, SessionService.userId))
                            startBreak()
                        },
                        { error -> processError(error) }
                )
    }

    private fun scheduleBreakLater(taskId: Long){

        repo.updateDND(taskId, true)
        message(getString(R.string.break_scheduled))

        finish()
    }

    private fun stopBreak() {
        showProgress()

        val tasks = repo.getTasksForUser(SessionService.userId)

        if (tasks.isNotEmpty()){
            val task = tasks.first()
            repo.updateDND(task._id!!, false)
        }

        service.status(UserStatus(userId = SessionService.userId, action = BreakStatus.AVAILABLE.value))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { _ ->
                            repo.addEvent(eventOf(UserAction.BREAK_COMPLETE, SessionService.userId))
                            close()
                        },
                        { error -> processError(error) }
                )
    }

    private fun startBreak() {

        hideProgress()
        showBreak()

        SessionService.breakStatus = BreakStatus.BREAK

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread { updateTime() }
            }
        }, 0, 1000)
    }

    private fun showBreak() {

        break_icon.visibility = View.GONE

        break_message.visibility = View.VISIBLE
        timer_text.visibility = View.VISIBLE

        activate_break.visibility = View.GONE
        stop_break.visibility = View.VISIBLE

        stop_break.text = getString(R.string.stop_break)
        break_message.text = getString(R.string.break_message)
    }

    private fun showFree() {

        break_icon.visibility = View.VISIBLE

        break_message.visibility = View.GONE
        timer_text.visibility = View.GONE

        activate_break.visibility = View.VISIBLE
        stop_break.visibility = View.GONE
    }

    private fun showScheduled() {

        break_icon.visibility = View.GONE

        break_message.visibility = View.VISIBLE
        timer_text.visibility = View.GONE

        activate_break.visibility = View.GONE
        stop_break.visibility = View.VISIBLE

        stop_break.text = "CANCEL BREAK"
        break_message.text = getString(R.string.break_scheduled)
    }

    private fun updateTime() {

        val elapsed = (System.currentTimeMillis() - SessionService.breakStart) / 1000

        val s = elapsed % 60
        var m = elapsed / 60
        val h = m / 60
        m -= h * 60

        timer_text.text = String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun close() {
        hideProgress()
        SessionService.breakStatus = BreakStatus.AVAILABLE

        timer.cancel()
        finish()
    }
}
