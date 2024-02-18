package com.pharmeasy.fetchr.activity

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.adapter.DevicesAdapter
import com.pharmeasy.fetchr.scanner.ScannerActionListener
import com.pharmeasy.fetchr.scanner.ScannerService
import com.pharmeasy.fetchr.service.SessionService
import kotlinx.android.synthetic.main.devices_content.*
import kotlinx.android.synthetic.main.devices_main.*



class DevicesActivity() : BaseActivity(), ScannerActionListener {

    private val TAG = "DA"
    private val ENABLE_BT = 1000

    private var mode: String? = null

    override val rootView: View?
        get() = root_panel

    override val progressIndicator: ProgressBar?
        get() = progress

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(com.pharmeasy.fetchr.R.layout.devices_main)

        title = getString(com.pharmeasy.fetchr.R.string.devices)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        list.layoutManager = layoutManager

        mode = intent.getStringExtra("mode")
        if(mode != null){
            title = getString(com.pharmeasy.fetchr.R.string.select_scanner)
        }

        toolbar.setNavigationOnClickListener {
            if(mode == null || !SessionService.scannerEnabled)
                finish()
        }

        enable_scanner.setOnClickListener {
            SessionService.scannerEnabled = true

            reload()
        }

        disable_scanner.setOnClickListener {

            ScannerService.disconnect()
            SessionService.scannerEnabled = false


            if(mode != null)
                finish()
            else
                reload()
        }

        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = mBluetoothAdapter.bondedDevices

        val s = ArrayList<String>()
        for (bt in pairedDevices)
            s.add(bt.name)

        //setListAdapter(ArrayAdapter(this, R.layout.list, s))
    }

    override fun onBackPressed() {
        if(mode == null || !SessionService.scannerEnabled)
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.refresh, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> reload()
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onResume() {
        super.onResume()

        ScannerService.register(this)

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            startActivityForResult(intent, ENABLE_BT)
        }else
            reload()
    }

    public override fun onPause() {
        super.onPause()

        ScannerService.deregister(this)
    }

    private fun reload(): Boolean {

        val devices = ScannerService.getPairedDevices()
        list.adapter = DevicesAdapter(this, devices, SessionService.scannerEnabled)

        if(devices.isEmpty()) {
            empty_panel.visibility = View.VISIBLE
            list.visibility = View.GONE
        }else{
            empty_panel.visibility = View.GONE
            list.visibility = View.VISIBLE
        }

        if(SessionService.scannerEnabled){
            enable_scanner.visibility = View.GONE
            disable_scanner.visibility = View.VISIBLE
        }else{
            disable_scanner.visibility = View.GONE

            if(devices.isEmpty())
                enable_scanner.visibility = View.GONE
            else
                enable_scanner.visibility = View.VISIBLE
        }

        return true
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                reload()
            } else {
                message(getString(R.string.bt_not_enabled))
                finish()
            }
        }else
            super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onConnected() {
        hideProgress()

        if(mode != null)
            finish()
        else
            reload()
    }

    override fun onConnecting() {
        showProgress()
    }

    override fun onDisconnected() {
        hideProgress()

        reload()
    }

    override fun onData(barcode: String) {}
}
