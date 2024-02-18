package com.pharmeasy.fetchr.features.barcoder.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import com.pharmeasy.fetchr.features.barcoder.presenter.BarcoderMedicineDetailsPresenterImpl
import com.pharmeasy.fetchr.features.barcoder.view.BarcoderMedicineDetailsView
import com.pharmeasy.fetchr.fragments.BottomSheetWrongMedicineFragment
import kotlinx.android.synthetic.main.activity_barcoder_medicine_details.*
import kotlinx.android.synthetic.main.bin_tray_scan_text.*
import kotlinx.android.synthetic.main.button_layout.*
import kotlinx.android.synthetic.main.layout_enter_detail.*
import kotlinx.android.synthetic.main.medicine_barcoder_details_layout.*

class BarcoderMedicineDetailsActivity : BaseActivity(), BarcoderMedicineDetailsView {

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = null

    private lateinit var mPresenter: BarcoderMedicineDetailsPresenterImpl

    private var trayId: String? = null

    private var mainTaskId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcoder_medicine_details)

        if(intent.extras != null && intent.getStringExtra("trayId") != null) {
            trayId = intent.getStringExtra("trayId")
            mainTaskId = intent.getLongExtra("mainTaskId", 0)
        }else
            finish()

        mPresenter = BarcoderMedicineDetailsPresenterImpl(this, mainTaskId!!)
        mPresenter.attachView(this)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        mPresenter.tasksOnViewResumed()
    }

    private fun initViews(){

        bin_scan_text.text = getString(R.string.tray)
        bin_scan_name_text.text = trayId

        submit_button.setOnClickListener {
            valid()
        }
    }

    override fun valid() {

        if (et_batch_no.text.isEmpty() || et_mrp.text.isEmpty() || !mPresenter.batchValidation(et_batch_no.text.toString())){
            et_batch_no.setBackgroundResource(R.drawable.red_border)
            et_mrp.setBackgroundResource(R.drawable.red_border)
            tv_validation.visibility = View.VISIBLE
            tv_validation_mrp.visibility = View.VISIBLE
            return
        }



        if (!mPresenter.validateWrongBarcode(et_batch_no.text.toString(), et_mrp.text.toString())){
            return
        }

        if(mPresenter.validateNearExpiry()){
            mPresenter.markNearExpiry()
            return
        }

        /*
        * Call status API to set status IN-PROGRESS
        * */
        mPresenter.markStatus()
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

    override fun launchPasteBarcodeActivity() {
        hideProgress()

        val intent = Intent(this, BarcoderCompleteTaskActivity::class.java)
        intent.putExtra("mainTaskId", mainTaskId!!)
        intent.putExtra("trayId", trayId!!)
        startActivity(intent)
        finish()
    }

    override fun launchBarcodeAdminActivity() {
        hideProgress()

        val intent = Intent(this, BarcoderAdminActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun bottomSheetWrongMedicine(){
        val bottomSheetWrongMedicineFragment = BottomSheetWrongMedicineFragment()
        val args = Bundle()
        args.putSerializable("mainTaskId", mainTaskId)
        bottomSheetWrongMedicineFragment.arguments = args
        bottomSheetWrongMedicineFragment.isCancelable = false
        bottomSheetWrongMedicineFragment.show(supportFragmentManager, bottomSheetWrongMedicineFragment.tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter.detachView()
    }
}
