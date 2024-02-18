package com.pharmeasy.fetchr.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.features.barcoder.presenter.FragmentWrongMedicinePresenterImpl
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderAdminActivity
import com.pharmeasy.fetchr.features.barcoder.view.FragmentView
import kotlinx.android.synthetic.main.bottom_sheet_wrong_medicine.view.*
import kotlinx.android.synthetic.main.button_layout.*
import kotlinx.android.synthetic.main.button_layout.view.*

class BottomSheetWrongMedicineFragment : BaseBottomSheetFragment(), View.OnClickListener , FragmentView {

    private lateinit var fPresenter: FragmentWrongMedicinePresenterImpl

    private var mainTaskId: Long? = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainTaskId = arguments?.getLong("mainTaskId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.bottom_sheet_wrong_medicine, container, false)

        view.button_raise_issue.visibility = View.VISIBLE
        view.button_proceed.visibility = View.GONE

        view.btnHideDialog.setOnClickListener(this)
        view.button_raise_issue.setOnClickListener(this)

        fPresenter = FragmentWrongMedicinePresenterImpl(activity!!, mainTaskId!!)
        fPresenter.attachView(this)
        return view
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.btnHideDialog -> dismiss()

            R.id.button_raise_issue -> fPresenter.markWrongMedicine()
        }
    }

    override fun openActivity() {
        dismiss()

        val intent = Intent(activity, BarcoderAdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    override fun showProgressBar() {
        showProgress()    }

    override fun hideProgressBar() {
        hideProgress()    }

    override fun showError(error: Throwable) {
        processError(error)
    }

    override fun onResume() {
        super.onResume()
        fPresenter.tasksOnViewResumed()
    }

    override fun openMainActivity() {

    }

    override fun onDestroy() {
        super.onDestroy()
        fPresenter.detachView()
    }
}