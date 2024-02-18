package com.pharmeasy.fetchr.main.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.pharmeasy.fetchr.BuildConfig
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.activity.BreakActivity
import com.pharmeasy.fetchr.activity.DevicesActivity
import com.pharmeasy.fetchr.activity.LoginActivity
import com.pharmeasy.fetchr.constants.TRAYZONE
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderMedicineDetailsActivity
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderCompleteTaskActivity
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderScanTrayActivity
import com.pharmeasy.fetchr.features.racker.PutawayMultipleTrayStep
import com.pharmeasy.fetchr.features.racker.RackScanBinActivity
import com.pharmeasy.fetchr.features.verifier.*
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.*
import com.pharmeasy.fetchr.main.presenter.MainPresenterImpl
import com.pharmeasy.fetchr.main.view.MainView
import com.pharmeasy.fetchr.model.*
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.service.*
import com.pharmeasy.fetchr.type.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.home.*
import kotlinx.android.synthetic.main.racker_home.*

class MainActivity : BaseActivity(), MainView, NavigationView.OnNavigationItemSelectedListener {

    private val TAG = MainActivity::class.java.name
    private lateinit var mPresenter: MainPresenterImpl

    private var pendingTask: UserTask? = null

    private var rackerTask: Task? = null

    private lateinit var name: TextView
    private lateinit var username: TextView

    private var trayPickedZone: String? = null

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    private val green by lazy {
        ContextCompat.getColor(baseContext, R.color.colorPrimary)
    }

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val header_view = nav_view.getHeaderView(0)
        name = header_view.findViewById(R.id.name)
        username = header_view.findViewById(R.id.username)

        initViews()

        mPresenter = MainPresenterImpl(repo)
        mPresenter.attachView(this)
    }

    private fun initViews(){
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        btn_task.setOnClickListener {
            processTask()
        }

        self_assignment.setOnClickListener {

            showProgress()
            Log.d(TAG, "$pendingTask")
            mPresenter!!.selfAssignment()
        }

        updateUserInfo()
        version.text = "v ${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}"
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_devices -> showDevices()
            R.id.nav_break -> verifyAndTakeBreak()
            //R.id.reset -> verifyAndResetData()
            R.id.nav_logout -> mPresenter!!.onLogoutClicked()
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.refresh, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> mPresenter.refreshTask()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        repo.refresh()

        pendingTask = null
        mPresenter.tasksOnViewResumed()
    }

    private fun updateUserInfo() {

        name.text = SessionService.name
        username.text = SessionService.username
        display_name.text = "Hello ${SessionService.name}"
    }

    override fun showNewTask() {

        when {
            SessionService.role == verifier_role -> {
                verifier_layout.visibility = View.VISIBLE
                txt_new_task.visibility = View.VISIBLE
                btn_task.text = getString(R.string.scan_tray_for_items)
                txt_name.text = "Hello ${SessionService.name}"

                mPresenter?.checkAssignedTask()
                return
            }
            SessionService.role == racker_role -> {
                verifier_layout.visibility = View.GONE
                racker_home.visibility = View.VISIBLE

                tray_picked_zone.visibility = View.GONE
                self_assignment.visibility = View.GONE
            }
            SessionService.role == barcoder_role -> {
                verifier_layout.visibility = View.VISIBLE
                btn_task.text = getString(R.string.start_new_task)
                txt_name.text = "Hello ${SessionService.name}"
                txt_new_task.visibility = View.VISIBLE
            }
        }

        view()
        pendingTask = null

    }

    override fun launchBarcoderScanTrayActivity() {
        val intent = Intent(this, BarcoderScanTrayActivity::class.java)
        startActivity(intent)
    }

    override fun showPendingTask(task: UserTask, userTaskList: MutableList<String>) {
        pendingTask = task //db task

        if (SessionService.role == racker_role) {
            verifier_layout.visibility = View.GONE
            racker_home.visibility = View.VISIBLE
            tray_picked_zone.visibility = View.VISIBLE
            self_assignment.visibility = View.VISIBLE
        } else {
            verifier_layout.visibility = View.VISIBLE
            btn_task.text = getString(R.string.start_new_task)
            txt_name.text = "Hello ${SessionService.name}"
            txt_new_task.visibility = View.VISIBLE
        }

        val type = TaskType.valueOf(task.type!!)

        if (type == TaskType.PUTAWAY && userTaskList.isNotEmpty() && !userTaskList.contains(TaskStatus.PICKED.name) && !userTaskList.contains(TaskStatus.ASSIGNED.name)) {
            verifier_layout.visibility = View.VISIBLE
            racker_home.visibility = View.GONE
            txt_empty_message.text = getString(R.string.cant_start_task)
            return
        }

        when (type) {

            TaskType.PUTAWAY -> if (userTaskList.contains(TaskStatus.PICKED.name)) {
                racker_home.visibility = View.VISIBLE
                self_assignment.visibility = View.VISIBLE
            } else {
                new_racker_task.visibility = View.VISIBLE
            }

            TaskType.VERIFICATION -> if (task.trayId == null) {
                txt_new_task.visibility = View.GONE
                btn_task.text = getString(R.string.scan_tray_for_items)
            } else {
                txt_new_task.visibility = View.GONE
                btn_task.text = getString(R.string.start_scanning)
            }

            TaskType.BARCODER -> if (task.trayId == null) {
                txt_new_task.visibility = View.GONE
            } else {
                txt_new_task.visibility = View.GONE
                btn_task.text = getString(R.string.start_scanning)
            }

            TaskType.ISSUE_VERIFICATION -> if (task.status == TaskStatus.IN_PROGRESS.name && rackerTask == null) {
                txt_new_task.visibility = View.GONE
                btn_task.text = getString(R.string.scan_tray_for_items)
            } else if (task.status == TaskStatus.IN_PROGRESS.name && rackerTask != null) {
                txt_new_task.visibility = View.GONE
                btn_task.text = getString(R.string.start_scanning)
            } else {
                txt_new_task.visibility = View.VISIBLE
                btn_task.text = "SCAN TRAY# ${task.trayId}"
            }

        }
    }

    private fun rackerTrayZone(userTask: UserTask) {
        when {
            userTask.trayPickedZone == null -> error(getString(R.string.no_tray_assigned))
            userTask.trayPickedZone == TaskStatus.SIDELINED_ZONE.name -> tray_picked_zone.text = getString(R.string.sideline_zone)
            userTask.trayPickedZone == TaskStatus.CANCELLED_ZONE.name -> tray_picked_zone.text = getString(R.string.cancelled_zone)
            userTask.trayPickedZone == TaskStatus.JIT_ZONE.name -> tray_picked_zone.text = getString(R.string.jit_zone)
            userTask.trayPickedZone == TaskStatus.SALE_RETURN_ZONE.name -> tray_picked_zone.text = getString(R.string.sale_return_zone)
            userTask.trayPickedZone == TaskStatus.AUDITOR_ZONE.name -> tray_picked_zone.text = getString(R.string.auditor_zone)
            userTask.trayPickedZone == TaskStatus.PROCUREMENT_ZONE.name -> tray_picked_zone.text = getString(R.string.procurement_zone)
            userTask.trayPickedZone == TaskStatus.RECTIFICATION_ZONE.name -> tray_picked_zone.text = getString(R.string.rectification_zone)
            userTask.trayPickedZone == TaskStatus.BILLER_ZONE.name -> tray_picked_zone.text = getString(R.string.biller_zone)
            userTask.trayPickedZone == TaskStatus.JIT_BIN_ZONE.name-> tray_picked_zone.text = getString(R.string.jit_bin_zone)

        }

    }

    private fun startTrayScan() {
        startActivity(Intent(this, VerificationFirstTrayScanActivity::class.java))
    }

    private fun processTask() {

        if (SessionService.role == verifier_role && pendingTask == null) {
            startTrayScan()
            return
        }

        if(SessionService.role == barcoder_role && pendingTask == null){
            launchBarcoderScanTrayActivity()
            return
        }

        val intent = when (TaskType.valueOf(pendingTask!!.type!!)) {

            TaskType.VERIFICATION -> if (pendingTask!!.trayId == null)
                Intent(this, VerificationTrayScanActivity::class.java)
            else {
                if (pendingTask!!.referenceType == Source.INVOICE_PROCUREMENT.name)
                    Intent(this, VerifyItemActivity::class.java)
                else
                    Intent(this, VerificationActivity::class.java)
            }

            TaskType.BARCODER -> when {
                pendingTask!!.trayId == null -> Intent(this, BarcoderScanTrayActivity::class.java)
                pendingTask!!.status == TaskStatus.IN_PROGRESS.name -> Intent(this, BarcoderCompleteTaskActivity::class.java)
                else -> Intent(this, BarcoderMedicineDetailsActivity::class.java)
            }

            TaskType.ISSUE_VERIFICATION -> if (pendingTask!!.status == TaskStatus.IN_PROGRESS.name && rackerTask == null)
                Intent(this, VerificationTrayScanActivity::class.java)
            else if (pendingTask!!.status == TaskStatus.IN_PROGRESS.name && rackerTask != null) {
                if (pendingTask!!.referenceType == Source.INVOICE_PROCUREMENT.name)
                    Intent(this, VerifyItemActivity::class.java)
                else
                    Intent(this, VerificationActivity::class.java)
            }
            else
                Intent(this, VerificationIssueTrayScanActivity::class.java)

            else -> return
        }

        intent.putExtra("mainTaskId", pendingTask!!._id)
        intent.putExtra("taskId", pendingTask!!.taskId)
        intent.putExtra("trayId", pendingTask!!.trayId)

        startActivity(intent)
    }

    override fun setNextBin(startRacking: StartRacking) {
        hideProgress()
        val intent = Intent(this, RackScanBinActivity::class.java)
        intent.putExtra("mainTaskId", pendingTask!!._id)
        intent.putExtra("binId", startRacking.bin)
        intent.putExtra("trayId", startRacking.trayId)
        intent.putExtra("ucode", startRacking.ucode)
        intent.putExtra("uCount", startRacking.ucodeScanRequired)
        intent.putExtra(TRAYZONE, pendingTask!!.trayPickedZone)
        startActivity(intent)
    }

    override fun setTrayZone(userTask: UserTask) {
        hideProgress()

        if (userTask.trayPickedZone != null) {
            tray_picked_zone.visibility = View.VISIBLE
            self_assignment.visibility = View.VISIBLE
        }

        trayPickedZone = userTask.trayPickedZone
        rackerTrayZone(userTask)
    }

    private fun verifyAndTakeBreak() {
        takeBreak()
    }

    override fun takeBreak() {
        startActivity(Intent(this, BreakActivity::class.java))
    }

    private fun showDevices() {
        startActivity(Intent(this, DevicesActivity::class.java))
    }

    private fun view() {

    }

    override fun launchLoginActivity() {
        repo.addEvent(eventOf(UserAction.LOGOUT, SessionService.userId))

        SessionService.token = ""
        SessionService.userId = ""
        SessionService.role = ""
        SessionService.name = ""
        SessionService.username = ""

        hideProgress()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun showRackView() {
        tray_picked_zone.visibility = View.GONE
        self_assignment.visibility = View.GONE
    }

    override fun launchPutawayMultipleTrayStep() {
        val intent = Intent(this, PutawayMultipleTrayStep::class.java)
        intent.putExtra(TRAYZONE, trayPickedZone)
        startActivity(intent)
    }

    override fun showMessage(messageString: String) {
        message(messageString)
    }

    override fun showProgressBar() {
        showProgress()
    }

    override fun hideProgressBar() {
       hideProgress()
    }

    override fun showError(error: Throwable) {
        processError(error)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter.detachView()
    }
}
