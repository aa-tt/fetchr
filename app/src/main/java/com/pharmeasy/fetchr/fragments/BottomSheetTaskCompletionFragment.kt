package com.pharmeasy.fetchr.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.features.barcoder.presenter.FragmentCompleteTaskPresenterImpl
import com.pharmeasy.fetchr.features.barcoder.ui.BarcoderAdminActivity
import com.pharmeasy.fetchr.features.barcoder.view.FragmentView
import com.pharmeasy.fetchr.main.ui.MainActivity
import kotlinx.android.synthetic.main.bottom_sheet_confirmation.view.*

class BottomSheetTaskCompletionFragment : BaseBottomSheetFragment(), View.OnClickListener, FragmentView {

    private lateinit var fPresenter: FragmentCompleteTaskPresenterImpl

    private var mainTaskId: Long? = -1
    private var isIssue: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainTaskId = arguments?.getLong("mainTaskId")
        isIssue = arguments?.getBoolean("isIssue")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_confirmation, container, false)

        view.btn_dismiss_button.setOnClickListener(this)
        view.btn_confirm_button.setOnClickListener(this)

        (view.findViewById(R.id.confirm_task_completion_text) as TextView).text = getString(R.string.confirm_task_completion)

        (view.findViewById(R.id.confirm_task_completion_text_detail) as TextView).text = getString(R.string.no_back_text)

        fPresenter = FragmentCompleteTaskPresenterImpl(activity!!, mainTaskId!!, isIssue!!)
        fPresenter.attachView(this)

        return view
    }

    override fun onResume() {
        super.onResume()
        fPresenter.tasksOnViewResumed()
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_dismiss_button -> dismiss()
            R.id.btn_confirm_button -> fPresenter.markCompleteTask()
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

    override fun openActivity() {
        dismiss()

        val intent = Intent(activity, BarcoderAdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    override fun openMainActivity() {
        dismiss()

        Toast.makeText(activity,"Task has been completed!", Toast.LENGTH_SHORT).show()

        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    override fun onStop() {
        super.onStop()
        fPresenter.detachView()
    }
}