package com.pharmeasy.fetchr.features.racker

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.adapter.PutawayBinAdapter
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.BinItem
import com.pharmeasy.fetchr.model.ProductItem
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.ProcessingStatus
import kotlinx.android.synthetic.main.putaway_bin_content.*
import kotlinx.android.synthetic.main.putaway_bin_main.*

class PutawayBinActivity : BaseActivity() {

    private lateinit var current: BinItem
    private lateinit var task: UserTask
    private var count: Int? = 0

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progress

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.putaway_bin_main)

        title = getString(R.string.putaway_list)

        if (intent.extras != null && intent.extras.containsKey("data")) {
            current = intent.extras.get("data") as BinItem
            count = intent.extras.getInt("count", 0)
            title = "BIN# ${current.binId}"
        } else {
            message("Unable to process")
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

        retrieve()
    }

    private fun retrieve() {
        repo.refresh()
        val it = repo.getTaskById(current.taskId)

        this.task = it.first()
        val items = repo.getItemsByTaskAndBin(current.taskId, current.binId, ProcessingStatus.SYNC)

        populate(items)

    }

    private fun populate(taskItems: List<TaskItem>) {

        val grouped = taskItems.groupBy { Triple(it.name, it.ucode, it.packForm ?: "") }
        val lots = grouped.keys.map {

            val items = grouped[it]!!
            val pending = items.any { it.processed < ProcessingStatus.SYNC.value }
            val status = if (pending) {
                if (items.size == 1)
                    translateStatus(items.first().status!!).name
                else
                    items.map { it.status }.reduce { x, y -> min(x, y) }
            } else
                ItemStatus.DONE.name

            ProductItem(current.taskId, it.first!!, it.second, it.third, current.binId, 0, items.size, ItemStatus.valueOf(status!!))
        }

        if(count == 1 && lots.size == 1)
            count = 1
        else
            count = 0

        list.adapter = PutawayBinAdapter(this, lots, count!!, task.taskId!!)

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

    private fun checkForCompletion(items: List<ProductItem>) {
        val pending = items.any { it.status == ItemStatus.PENDING || it.status == ItemStatus.IN_PROGRESS }

        if (!pending)
            finish()
    }
}
