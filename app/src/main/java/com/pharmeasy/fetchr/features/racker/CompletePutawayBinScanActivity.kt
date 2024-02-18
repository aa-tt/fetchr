package com.pharmeasy.fetchr.features.racker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew

class CompletePutawayBinScanActivity() : ScanActivity() {

    private var binId: String? = null

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        binId = intent.getStringExtra("binId")
        if (binId == null) {
            message("Unable to process")
            finish()
        }
        title = "${getString(R.string.scan)}  ${binId}"

    }

    override fun title(): String = this.getString(R.string.scan)

    override fun processScannedItem(id: String) {

        if (binId!!.trim() != id) {
            failure("Wrong bin scanned")
            return
        }

        success("")
    }

    override fun next() {
        setResult(Activity.RESULT_OK, Intent())
        finish()
    }
}
