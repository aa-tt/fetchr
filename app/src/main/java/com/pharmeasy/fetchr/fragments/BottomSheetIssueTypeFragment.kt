package com.pharmeasy.fetchr.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.event.OnItemCheckListener
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderIssueItemActivity
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderScanIssueMedicineActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.greendao.model.UserTask
import com.pharmeasy.fetchr.model.Task
import kotlinx.android.synthetic.main.bottom_sheet_fragment.*
import kotlinx.android.synthetic.main.button_layout.*

class BottomSheetIssueTypeFragment : BaseBottomSheetFragment(), OnItemCheckListener, View.OnClickListener {

    private val TAG: String = BottomSheetIssueTypeFragment::class.java.simpleName

    private lateinit var task: UserTask
    lateinit var mActivity: Activity

    private var issueReason: String? = null
    private var mainTaskId: Long? = null

    private val repo by lazy {
        TaskRepoNew(context = activity!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_fragment, container, false)
        mainTaskId = arguments?.getLong("mainTaskId")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deliveryOptions = setRadioButtonList()
        addRadioButtons(view, deliveryOptions.size, deliveryOptions)

        button_raise_issue.visibility = View.VISIBLE
        button_proceed.visibility = View.GONE
        iv_raise_issue_arrow.visibility = View.GONE

        button_raise_issue.setOnClickListener(this)
        btnHideDialog.setOnClickListener(this)

        button_raise_issue.isEnabled = false

    }

    override fun onResume() {
        super.onResume()
        task = repo.getTaskById(mainTaskId!!).first()
    }

    private fun setRadioButtonList(): ArrayList<String> {
        val deliveryOptions = ArrayList<String>()

        deliveryOptions.add("Missing / Damaged Barcode")
        deliveryOptions.add("Extra Barcode")
        deliveryOptions.add("Damaged Medicine")
        deliveryOptions.add("Incorrect Medicine")

        return deliveryOptions

    }

    private fun addRadioButtons(view: View?, number: Int, deliveryOptions: ArrayList<String>) {
        val radioGroup = RadioGroup(activity!!)
        radioGroup.orientation = LinearLayout.VERTICAL

        for (i in 0 until number) {

            val radioOptionButton = RadioButton(context)
            radioOptionButton.id = View.generateViewId()
            radioOptionButton.text = deliveryOptions[i]
            radioOptionButton.textSize = 16f
            radioOptionButton.setPadding(56, 0, 0, 0)
            radioOptionButton.setTextColor(resources.getColor(R.color.txt_color))

            val params = RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(16, 16, 16, 16)
            radioOptionButton.layoutParams = params

            (view!!.findViewById(R.id.radioGroup) as RadioGroup).addView(radioOptionButton)
        }

        (view!!.findViewById(R.id.radioGroup) as RadioGroup).setOnCheckedChangeListener { group, checkedId ->

            val selected = group.findViewById<RadioButton>(checkedId)
            when (selected.text) {
                "Missing / Damaged Barcode" -> {
                    issueReason = selected.text.toString()
                    mActivity = BarcoderIssueItemActivity()
                    button_raise_issue.isEnabled = true
                }
                "Extra Barcode" -> {
                    issueReason = selected.text.toString()
                    mActivity = BarcoderScanIssueMedicineActivity()
                    button_raise_issue.isEnabled = true
                }
                "Damaged Medicine" -> {
                    issueReason = selected.text.toString()
                    mActivity = BarcoderScanIssueMedicineActivity()
                    button_raise_issue.isEnabled = true
                }
                "Incorrect Medicine" -> {
                    issueReason = selected.text.toString()
                    mActivity = BarcoderScanIssueMedicineActivity()
                    button_raise_issue.isEnabled = true
                }
            }
        }

    }

    override fun onClick(v: View?) {
        when (v!!.id) {

            R.id.btnHideDialog -> dismiss()

            R.id.button_raise_issue -> {
                dismiss()
                showActivity(mActivity::class.java)
            }
        }
    }

    private fun <T> showActivity(mActivity: Class<T>){
        Log.d(TAG, "$mActivity $issueReason")

        val intent = Intent(activity, mActivity)
        intent.putExtra("issueReason", issueReason)
        intent.putExtra("mainTaskId", mainTaskId)
        intent.putExtra("taskId", task.taskId)
        startActivity(intent)
    }

    override fun onItemCheck(position: Int) {


    }

    override fun onItemUncheck(position: Int) {
    }


}