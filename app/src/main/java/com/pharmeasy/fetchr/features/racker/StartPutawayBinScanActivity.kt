package com.pharmeasy.fetchr.features.racker

import android.content.Intent
import android.os.Bundle
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.ScanActivity
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.model.ProductItem
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.retro.retroWithToken
import com.pharmeasy.fetchr.service.RackerService
import com.pharmeasy.fetchr.type.UserAction
import java.io.Serializable

class StartPutawayBinScanActivity() : ScanActivity() {

    private lateinit var current: ProductItem
    private var count: Int? = 0
    private var taskId: Long = -1

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    private val service by lazy {
        retroWithToken(RackerService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        if (intent.extras != null && intent.extras.containsKey("data")) {
            current = intent.extras.get("data") as ProductItem
            count = intent.extras.getInt("count",0)
            taskId = intent.getLongExtra("taskId", -1)
        } else {
            message("Unable to process")
            finish()
        }

        title = "${getString(R.string.scan)}  ${current.binId}"
    }

    override fun title(): String = this.getString(R.string.scan)

    override fun processScannedItem(id: String) {

        if (current.binId.trim() != id) {
            failure("Wrong bin scanned")
            return
        }
        processing()
        binScanned(id)
    }

    private fun binScanned(id: String) {

        repo.addEvent(eventOf(UserAction.BIN_SCAN, id))
        success("")
    }

    override fun next() {

        showPutawayBin()
    }

    private fun showPutawayBin() {

        val intent = Intent(this, PutawayItemActivity::class.java)
        intent.putExtra("data", current as Serializable)
        intent.putExtra("count", count )
        intent.putExtra("binStatus", true)
        intent.putExtra("ucount", true)
        startActivity(intent)
        finish()
    }
}
