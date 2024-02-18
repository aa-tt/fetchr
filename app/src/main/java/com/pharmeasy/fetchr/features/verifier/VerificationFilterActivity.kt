package com.pharmeasy.fetchr.features.verifier

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.adapter.VerificationAdapter
import com.pharmeasy.fetchr.constants.FILTER_DEBOUNCE
import com.pharmeasy.fetchr.event.ItemSelectedListener
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.model.NextItemTask
import com.pharmeasy.fetchr.model.ProductLotItem
import com.pharmeasy.fetchr.retro.processingStatusOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.VerifierService
import com.pharmeasy.fetchr.type.ItemStatus
import com.pharmeasy.fetchr.type.ProcessingStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.verification_filter_content.*
import kotlinx.android.synthetic.main.verification_filter_main.*
import java.io.Serializable
import java.util.concurrent.TimeUnit

class VerificationFilterActivity() : BaseActivity(), ItemSelectedListener {

    private var mainTaskId: Long = -1
    private var taskId: Long = -1
    private lateinit var subject: PublishSubject<String>
    private var lots: List<ProductLotItem> = emptyList()
    private var pageCount = 0

    private val service by lazy {
        retroWithToken(VerifierService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.verification_filter_main)

        title = getString(R.string.filter_medicines)

        mainTaskId = intent.getLongExtra("mainTaskId", -1)
        taskId = intent.getLongExtra("taskId", -1)
        if (mainTaskId < 0) {
            error("Unable to process")
            finish()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        list.layoutManager = layoutManager

        toolbar.setNavigationOnClickListener {
            finish()
        }

        subject = PublishSubject.create<String>()
        subject.debounce(FILTER_DEBOUNCE, TimeUnit.MILLISECONDS)
                .subscribe {
                    runOnUiThread {
                        filter(it)
                    }
                }

        filter_text.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null)
                    subject.onNext(s.toString().trim().toLowerCase())
            }
        })

        filter_text.setOnKeyListener { _, key, _ ->

            if (key == KeyEvent.KEYCODE_ENTER) {
                hideKeyboard()
                true
            }
            false
        }

        //raise issue
        raise_ticket_button.setOnClickListener{
            raiseTicket()
        }
    }

    override fun onResume() {
        super.onResume()

        filter_text.setText("")
        reload()
    }

    private fun reload() {
        repo.refresh()
        val items = repo.getItemsByTask(mainTaskId)
        populate(items)
    }

    private fun populate(taskItems: List<TaskItem>) {

        val grouped = taskItems.groupBy {
            ProductLotItem(id = mainTaskId, taskId = taskId, name = it.name!!, ucode = it.ucode, packForm = it.packForm
                    ?: "", batchNumber = it.batchNumber!!, expiry = it.expiryDate!!, mrp = it.mrp!!, status = ItemStatus.PENDING)
        }

        lots = grouped.keys.map {

            val items = grouped[it]!!
            val pending = items.any { it.processed < ProcessingStatus.SAVED.value }
            val status = if (pending) {
                if (items.size == 1)
                    translateStatus(items.first().status!!).name
                else
                    items.map { it.status }.reduce { x, y -> min(x, y) }
            } else
                ItemStatus.DONE.name
            ProductLotItem(id = mainTaskId, taskId = taskId, name = it.name, ucode = it.ucode, packForm = it.packForm, batchNumber = it.batchNumber, expiry = it.expiry, mrp = it.mrp, status = ItemStatus.valueOf(status!!))
        }

        val adapter = VerificationAdapter(this, lots.sortedBy { it.name },this)
        //adapter.itemSelectedListener = this
        list.adapter = adapter
    }

    private fun min(x: String? = null, y: String? = null): String {

        val p = translateStatus(x!!)
        val q = translateStatus(y!!)

        return if (p.ordinal < q.ordinal) p.name else q.name
    }

    private fun translateStatus(status: String): ItemStatus {

        return when (status) {
            ItemStatus.CREATED.name -> ItemStatus.PENDING
            ItemStatus.READY_FOR_PUTAWAY.name, ItemStatus.IN_ISSUE.name -> ItemStatus.IN_PROGRESS
            else -> ItemStatus.valueOf(status)
        }
    }

    private fun filter(query: String) {

        if (query.trim().isNotEmpty()) {
            val filtered = lots.filter { it.ucode.toLowerCase().contains(query) || it.name.toLowerCase().contains(query) }.sortedBy { it.name }
            list.adapter = VerificationAdapter(this, filtered,this)
        } else {
            list.adapter = VerificationAdapter(this, lots,this)
        }

        //(list.adapter as VerificationAdapter).itemSelectedListener = this
    }

    override fun onItemSelected(item: ProductLotItem) {
        runOnUiThread {
            val nextItem = repo.getVerifierItemsByTaskAndUCode(item.id, item.ucode, item.batchNumber)
            if (nextItem.isEmpty()) {
                showProgress()
                nextTaskItem(item)
            } else
                launchVerifyItemActivity(item)
        }
    }

    private fun launchVerifyItemActivity(item: ProductLotItem) {
        pageCount = 0
        val intent = Intent(this, VerifyItemActivity::class.java)
        intent.putExtra("data", item as Serializable)
        startActivity(intent)
        finish()
    }

    //fetching next items based on paginated
    private fun nextTaskItem(item: ProductLotItem) {

        service.nextItem(item.taskId!!, item.ucode, item.batchNumber, pageCount, 1000)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result -> close(result, item) },
                        { error -> processError(error) }
                )
    }

    private fun close(nextItem: NextItemTask, item: ProductLotItem) {
        if (nextItem.hasNext) {
            nextItem.data.forEach { it ->
                it.taskId = item.id
                it.processed = processingStatusOf(it.status!!) // processed status changes
                it.batchNumber = item.batchNumber
                repo.addVerifierItem(it)
            }
            pageCount++
            nextTaskItem(item)
            return
        } else {
            nextItem.data.forEach { it ->
                it.taskId = item.id
                it.processed = processingStatusOf(it.status!!) // processed status changes
                it.batchNumber = item.batchNumber
                repo.addVerifierItem(it)
            }
            hideProgress()
            launchVerifyItemActivity(item)
        }

    }

}
