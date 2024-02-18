package com.pharmeasy.fetchr.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.view.inputmethod.InputMethodManager
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.retro.httpErrorMessage
import org.jetbrains.anko.indeterminateProgressDialog
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

abstract class BaseBottomSheetFragment : BottomSheetDialogFragment(){

    private var progressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initProgress()
    }

    private fun initProgress() {
        progressDialog = activity?.indeterminateProgressDialog(resources.getString(R.string.waiting))
        progressDialog?.setCanceledOnTouchOutside(false)
        progressDialog?.setCancelable(false)
        progressDialog?.dismiss()
    }

    protected fun showProgress() {
        progressDialog?.show()
    }

    protected fun hideProgress() {
        progressDialog?.dismiss()
    }


    protected fun processError(error: Throwable?) {
        hideProgress()

        when (error) {
            is HttpException -> handleHttpError(error)
            is SocketTimeoutException -> error(getString(R.string.request_timeout))
            is IOException -> error(getString(R.string.network_error))
            else -> error(error!!.message!!)
        }
    }

    private fun handleHttpError(error: Throwable) {

        val message = httpErrorMessage(error) ?: getString(R.string.unable_to_process)
        error(message)
    }

    protected open fun error(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        errorMessage(message)
    }

    private fun errorMessage( message: String) {

        if(activity != null) {
            val builder = AlertDialog.Builder(activity!!)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, null)

            builder.create().show()
        }
    }
}
