package com.pharmeasy.fetchr.features.racker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScannerBaseActivity
import com.pharmeasy.fetchr.adapter.PutawayMultipleTrayStepAdapter
import com.pharmeasy.fetchr.constants.TRAYZONE
import com.pharmeasy.fetchr.event.TrayItemSelectedListener
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.StartRacking
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.*
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.RackerService
import com.pharmeasy.fetchr.type.TaskStatus
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.barcode_layout.*
import kotlinx.android.synthetic.main.bin_tray_scan_text.*
import kotlinx.android.synthetic.main.rack_scan_multiple_trays.*
import kotlinx.android.synthetic.main.scan_content.*
import java.util.*
import java.util.concurrent.TimeUnit


class PutawayMultipleTrayStep : ScannerBaseActivity(), TrayItemSelectedListener {

    private val TAG = PutawayMultipleTrayStep::class.java.name

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progressBar

    private var mainTaskId: Long = -1

    private lateinit var task: UserTask

    private lateinit var scanner: CodeScanner

    private var adapter: PutawayMultipleTrayStepAdapter? = null

    private val service by lazy {
        retroWithToken(RackerService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    private val trayArrayList = ArrayList<String>()
    private var trayZoneList = hashSetOf<String>()
    private var trayZone: String? = null
    private var scannerText: String? = "SCAN TRAY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.rack_scan_multiple_trays)

        mainTaskId = intent.getLongExtra("mainTaskId", -1)
        trayZone = intent.getStringExtra(TRAYZONE)

        list.layoutManager = LinearLayoutManager(this)

        adapter = PutawayMultipleTrayStepAdapter(this, trayArrayList, this)
        list.adapter = adapter

        scanner = CodeScanner.builder()
                .formats(CodeScanner.ALL_FORMATS)
                .autoFocus(true).autoFocusMode(AutoFocusMode.SAFE).autoFocusInterval(2000L)
                .flash(false)
                .onDecoded { result ->
                    runOnUiThread {
                        onBarcodeScanned(result.text.trim())
                    }
                }
                .onError { _ -> }.build(this, scanner_view)

        scanner_view.setOnClickListener {
            scanner.startPreview()

        }

        startRacking.setOnClickListener {
            startRacking()
        }

        raise_ticket_button.setOnClickListener {
            raiseTicket()
        }


        back_icon.setOnClickListener {
            finish()
        }

        bin_scan_name_text.text = trayZone
        tv_scanner.text = scannerText
        bin_scan_text.text = "Zone: "

    }

    override fun onResume() {
        super.onResume()

        val tasks = repo.getTrayList()
        if (!tasks.isEmpty()) {
            Log.d(TAG, "$tasks")
            trayArrayList.clear()
            tasks.forEach {
                if (it.status == TaskStatus.ASSIGNED.name && it.trayId != null)
                    if (!trayArrayList.contains(it.trayId!!))
                        trayArrayList.add(it.trayId!!)
            }
            adapter?.notifyDataSetChanged()
        }
    }

    @SuppressLint("CheckResult")
    fun processScannedItem(id: String) {

        showProgress()

        service.markTrayAsPicked(id, assigned_status)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> trayScanned(result, id) },
                        { error -> processError(error) }

                )

    }

    @SuppressLint("CheckResult")
    private fun startRacking() {

        service.startRacking(picked_status)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> trayList(result) },
                        { error -> processError(error) }

                )
    }

    private fun trayScanned(userTask: UserTask, trayId: String) {

        if (userTask.status == TaskStatus.READY_FOR_PUTAWAY.name)
            trayArrayList.remove(trayId)
        else {
            if (!trayArrayList.contains(trayId))
                trayArrayList.add(trayId)

            else{
                Toast.makeText(this, "Tray has been already scanned!", Toast.LENGTH_LONG).show()
            }
        }

        adapter?.notifyDataSetChanged()
        repo.addEvent(eventOf(UserAction.TRAY_SCAN, trayId))

        startRacking.isEnabled = trayArrayList.size != 0

        hideProgress()

        trayZoneList.add(userTask.trayPickedZone!!)
        bin_scan_name_text.text = trayZoneList.map { it }.toString()
    }


    private fun trayZoneScanned(userTask: UserTask, trayId: String) {

        if (userTask.status == TaskStatus.READY_FOR_PUTAWAY.name)
            trayArrayList.remove(trayId)
        else
            if (!trayArrayList.contains(trayId))
                trayArrayList.add(trayId)

        repo.addEvent(eventOf(UserAction.TRAY_SCAN, trayId))

        startRacking.isEnabled = trayArrayList.size > 0

        hideProgress()
    }

    private fun trayList(task: StartRacking) {

        //mainTaskId = repo.addTask(user = SessionService.userId, type = TaskType.PUTAWAY, task = task, reference = task.referenceId)
        if(task.rackerId != null) {
            val intent = Intent(this, RackScanBinActivity::class.java)
            intent.putExtra("mainTaskId", mainTaskId)
            intent.putExtra("binId", task.bin)
            intent.putExtra("trayId", task.trayId)
            intent.putExtra("ucode", task.ucode)
            intent.putExtra("uCount", task.ucodeScanRequired)
            startActivity(intent)
            finish()
        }
    }

    override fun onBarcodeScanned(barcode: String) {
        processScannedItem(barcode)
    }

    override fun onTrayItemSelected(trayId: String, status: Status) {

        service.markTrayAsPicked(trayId, status)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> trayZoneScanned(result, trayId) },
                        { error -> processError(error) }

                )

    }

}
