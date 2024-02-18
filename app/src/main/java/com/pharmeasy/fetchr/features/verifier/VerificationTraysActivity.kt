package com.pharmeasy.fetchr.features.verifier

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.adapter.VerificationTraysAdapter
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.model.TrayItem
import kotlinx.android.synthetic.main.verification_trays_content.*
import kotlinx.android.synthetic.main.verification_trays_main.*

class VerificationTraysActivity() : BaseActivity() {

    private var mainTaskId: Long = -1

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.verification_trays_main)

        title = getString(R.string.items_in_tray)

        mainTaskId = intent.getLongExtra("mainTaskId", -1)

        if (mainTaskId < 0) {
            error("Unable to process")
            finish()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        list.layoutManager = layoutManager


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
        val it = repo.getItemsByTask(mainTaskId)
        populate(it)

    }

    private fun populate(taskItems: List<TaskItem>) {
        val filtered = taskItems.filter { it.trayId != null }

        if(filtered.isEmpty()) {
            empty_panel.visibility = View.VISIBLE
            list.visibility = View.GONE
        }else{
            empty_panel.visibility = View.GONE
            list.visibility = View.VISIBLE
        }

        val items = mutableListOf<TrayItem>()

        val trays = filtered.map { it.trayId!! }.toSortedSet()
        trays.forEach { tray ->
            val header = TrayItem(trayId = tray, name = tray, ucode = tray, packForm = tray, batch = tray, count = 0)
            items.add(header)

            val grouped = filtered.filter { it.trayId == tray }.groupBy { TrayItem(trayId = it.trayId!!, name = it.name!!, ucode = it.ucode, packForm = it.packForm ?: "", batch = it.batchNumber!!, count = it.quantity!!) }
            grouped.keys.forEach {
                /*val count = grouped[it]!!.size
                it.count = count*/
                items.add(it)
            }
        }

        list.adapter = VerificationTraysAdapter(this, items)
    }
}
