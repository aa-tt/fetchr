package com.pharmeasy.fetchr.features.verifier

import android.content.Intent
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.type.UserAction

class VerificationFirstTrayScanActivity() : ScanActivity() {

    private lateinit var trayId: String

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override fun title(): String = this.getString(R.string.scan_tray_for_items)

    override fun processScannedItem(id: String) {

        if(!id.startsWith("TR")){
            failure(getString(R.string.invalid_tray))
            return
        }

        showProgress()
        processing()

        trayId = id
        processStatus(id)

    }

    override fun next() {

        showVerificationList()
    }

    private fun processStatus(id: String) {

        repo.addEvent(eventOf(UserAction.ADD_TRAY_SCAN, id))

        hideProgress()
        success("Items Tray# $id")
    }

    private fun showVerificationList() {

        val intent = Intent(this, VerificationInvoiceScanActivity::class.java)
        intent.putExtra("trayId", trayId)
        startActivity(intent)
        finish()
    }
}
