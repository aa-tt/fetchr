package com.pharmeasy.fetchr.features.barcoder.ui

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.adapter.BarcoderCompleteTaskIssueListAdapter
import com.pharmeasy.fetchr.features.barcoder.presenter.BarcoderCompleteTaskPresenterImpl
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderCompleteTaskView
import com.pharmeasy.fetchr.fragments.BottomSheetIssueTypeFragment
import com.pharmeasy.fetchr.fragments.BottomSheetTaskCompletionFragment
import com.pharmeasy.fetchr.greendao.model.BarcoderItem
import com.pharmeasy.fetchr.greendao.model.TaskItem
import com.pharmeasy.fetchr.model.BarcoderTasks
import kotlinx.android.synthetic.main.activity_barcoder_paste_barcode.*
import kotlinx.android.synthetic.main.bin_tray_scan_text.*
import kotlinx.android.synthetic.main.button_layout.*
import kotlinx.android.synthetic.main.issue_layout.*
import kotlinx.android.synthetic.main.layout_barcoder_item_details.*
import kotlinx.android.synthetic.main.medicine_barcoder_item_layout.*

class BarcoderCompleteTaskActivity : BaseActivity(), BarcoderCompleteTaskView, View.OnClickListener {

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = null

    private lateinit var mPresenter: BarcoderCompleteTaskPresenterImpl

    private var trayId: String? = null
    private var mainTaskId: Long? = null
    private var showIssueItems: Boolean? = false
    private var isIssue: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcoder_paste_barcode)

        if(intent.extras != null && intent.getStringExtra("trayId") != null) {
            trayId = intent.getStringExtra("trayId")
            mainTaskId = intent.getLongExtra("mainTaskId", 0)
            showIssueItems = intent.getBooleanExtra("showIssueItems", false)

        }else
            finish()

        initViews()

        mPresenter = BarcoderCompleteTaskPresenterImpl(this, mainTaskId!!, showIssueItems!!)
        mPresenter.attachView(this)
    }

    private fun initViews(){
        iv_raise_issue_arrow.visibility = View.VISIBLE
        iv_raise_issue_arrow.isEnabled = true

        button_complete_task.visibility = View.VISIBLE
        button_proceed.visibility = View.GONE

        button_complete_task.setOnClickListener(this)
        iv_raise_issue_arrow.setOnClickListener(this)


        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        rv_issue_type.layoutManager = layoutManager
    }

    override fun onResume() {
        super.onResume()
        mPresenter.tasksOnViewResumed()
    }

    override fun getTaskItem(item: TaskItem, items: List<TaskItem>, barcode: List<BarcoderItem>, trayId: String) {

        txt_medicine_name_text.text = item.name
        txt_medicine_batch_no.text = item.batchNumber
        txt_value_mrp.text = item.mrp.toString()
        txt_value_packform.text = item.packForm
        txt_value_expiry.text = item.expiryDate
        bin_scan_text.text = getString(R.string.tray)
        bin_scan_name_text.text = trayId


        if(barcode.isEmpty())
            txt_value_quantity.text = items.size.toString()
        else
            txt_value_quantity.text = barcode.size.toString()
    }

    override fun getIssueItemByList(items: List<BarcoderTasks>, taskItem: List<TaskItem>, barcoderItems: List<BarcoderItem>) {
        hideProgress()

        isIssue = true

        issue_layout.visibility = View.VISIBLE
        rv_issue_type.visibility = View.VISIBLE
        show_issue.visibility = View.GONE

        val adapter = BarcoderCompleteTaskIssueListAdapter(this, items)
        rv_issue_type.adapter = adapter

        if(taskItem.size == barcoderItems.size){
            iv_raise_issue_arrow.isEnabled = false
            message("All issues are raised.")
        }

    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.button_complete_task -> showTaskCompletionBottomSheet()

            R.id.iv_raise_issue_arrow -> {
                showRaiseIssueBottomSheet()
            }
        }
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

    override fun onFinish() {
        finish()
    }


    private fun showTaskCompletionBottomSheet() {
        val bottomSheetTaskCompletionFragment = BottomSheetTaskCompletionFragment()
        val args = Bundle()
        args.putLong("mainTaskId", mainTaskId!!)
        args.putBoolean("isIssue", isIssue!!)
        bottomSheetTaskCompletionFragment.arguments = args
        bottomSheetTaskCompletionFragment.isCancelable = false
        bottomSheetTaskCompletionFragment.show(supportFragmentManager, bottomSheetTaskCompletionFragment.tag)
    }

    private fun showRaiseIssueBottomSheet() {
        val bottomSheetIssueTypeFragment = BottomSheetIssueTypeFragment()
        val args = Bundle()
        args.putLong("mainTaskId", mainTaskId!!)
        bottomSheetIssueTypeFragment.arguments = args
        bottomSheetIssueTypeFragment.isCancelable = false
        bottomSheetIssueTypeFragment.show(supportFragmentManager, bottomSheetIssueTypeFragment.tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter.detachView()
    }
}
