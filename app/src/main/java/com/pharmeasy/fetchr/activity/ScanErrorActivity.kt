package com.pharmeasy.fetchr.activity

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.retro.vibrate
import kotlinx.android.synthetic.main.scan_error_content.*
import kotlinx.android.synthetic.main.scan_error_main.*

class ScanErrorActivity() : ScannerBaseActivity() {

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_error_main)

        title = getString(R.string.scan_error)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val header = intent.getStringExtra("title")
        if(header != null)
            title = header

        val msg = intent.getStringExtra("message")
        if(msg != null)
            message.text = msg

        toolbar.setNavigationOnClickListener {
        }

        go.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
    }

    override fun onBarcodeScanned(barcode: String) {
    }

    override fun onResume() {
        super.onResume()

        vibrate(this)
    }
}
