package com.pharmeasy.fetchr.features.verifier

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.activity.BaseActivity
import kotlinx.android.synthetic.main.verification_tray_scan_step_main.*
import kotlinx.android.synthetic.main.verification_tray_scan_step_content.*

class VerificationTrayScanStepActivity() : BaseActivity() {

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.verification_tray_scan_step_main)

        title = getString(R.string.scan_tray_for_items)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        pending_task.setOnClickListener {
            showTrayScan()
        }
    }

    private fun showTrayScan() {

        val intent = Intent(this, VerificationFirstTrayScanActivity::class.java)

        startActivity(intent)
        finish()
    }
}
