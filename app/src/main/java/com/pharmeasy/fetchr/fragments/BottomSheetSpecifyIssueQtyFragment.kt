package com.pharmeasy.fetchr.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.adapter.BarcoderIssueQtyAdapter
import com.pharmeasy.fetchr.event.OnItemCheckListener
import com.pharmeasy.fetchr.event.OnSpinnerItemSelected
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderIssueItemActivity
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderScanIssueMedicineActivity
import com.pharmeasy.fetchr.model.Task

class BottomSheetSpecifyIssueQtyFragment  : BaseBottomSheetFragment(), OnItemCheckListener, View.OnClickListener, OnSpinnerItemSelected {

    private val TAG: String = BottomSheetIssueTypeFragment::class.java.simpleName
    private var returnReasonFragment: BarcoderIssueQtyAdapter? = null

    private var itemTasks: Task? = null
    private var tripId: Int? = 0

    private var  reason: ArrayList<String> = ArrayList()
    private val issueTypeSelection = ArrayList<String>()

    // private var items : ArrayList<Item>? = null
    private var position : Int? = 0



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_specify_issue_qty, container, false)
        //(view.findViewById(R.id.btnHideDialog) as ImageButton).setOnClickListener(this)

        /* itemTasks = arguments?.getSerializable(Constant.taskItem) as Task
         tripId = arguments?.getInt(Constant.tripId)
         items = arguments?.getSerializable(Constant.item) as ArrayList<ClipData.Item>*/

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        issueTypeSelection.add("Missing Barcode")
        issueTypeSelection.add("Damaged Barcode")

        addIssueQty(issueTypeSelection)


    }

    private fun addIssueQty(issueTypeSelections: ArrayList<String>) {
        returnReasonFragment = BarcoderIssueQtyAdapter(this, activity!!, issueTypeSelections, false)

        reason.add(issueTypeSelections.get(0))
    }


    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btnHideDialog -> {
                dismiss()
            }
            R.id.button_raise_issue -> {
                showBarcoderIssueItemActivity()
            }
        }
    }

    private fun showBarcoderIssueItemActivity(){
        if (reason.size >= 0 ) {


            launchActivitiesFromButton()
            /*intent.putExtra(Constant.taskItem, itemTasks)
            intent.putExtra(Constant.tripId, tripId)
            intent.putExtra(Constant.item, items)
            intent.putExtra(Constant.reason, reason)*/
            dismiss()
        }/*else{
            Toast.makeText(activity,"Please Select atleast One Reason",Toast.LENGTH_SHORT).show()
        }*/
    }

    override fun onItemCheck(pos: Int) {
        reason.add(issueTypeSelection[pos])
        position = pos
        //raiseIssueItem.adapter!!.notifyDataSetChanged()

        showBarcoderIssueItemActivity()

    }

    override fun onItemUncheck(position: Int) {
        Log.d("TAG","dismiss")
        reason.remove(issueTypeSelection[position])
    }

    override fun onItemSelected(position: Int, spinnerPosition: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun launchActivitiesFromButton(){
        when (position){
            0 -> {
                val intent = Intent(activity, BarcoderIssueItemActivity::class.java)
                startActivity(intent)
            }
            1 -> {
                val intent = Intent(activity, BarcoderScanIssueMedicineActivity::class.java)
                startActivity(intent)
            }

        }
    }


}