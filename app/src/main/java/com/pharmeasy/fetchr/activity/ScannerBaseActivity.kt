package com.pharmeasy.fetchr.activity

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.scanner.ScannerActionListener
import com.pharmeasy.fetchr.scanner.ScannerService
import com.pharmeasy.fetchr.service.SessionService

abstract class ScannerBaseActivity : BaseActivity(), ScannerActionListener {

    private val ENABLE_BT = 1000

    public override fun onResume() {
        super.onResume()

        ScannerService.register(this)

        if (BluetoothAdapter.getDefaultAdapter() != null && !BluetoothAdapter.getDefaultAdapter().isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, ENABLE_BT)
        }else
            checkConnectedDevice()
    }

    public override fun onPause() {
        super.onPause()

        ScannerService.deregister(this)
    }

    protected abstract fun onBarcodeScanned(barcode: String)

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                checkConnectedDevice()
            } else {
                message(getString(R.string.bt_not_enabled))
                finish()
            }
        }else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkConnectedDevice(){

        if(ScannerService.currentDevice != null)
            return

        if(!SessionService.scannerEnabled)
            return

        val intent = Intent(this, DevicesActivity::class.java)
        intent.putExtra("mode", "select scanner")
        startActivity(intent)
    }

    override fun onConnecting() {
        showProgress()
    }

    override fun onConnected() {
        hideProgress()
    }

    override fun onDisconnected() {
        hideProgress()
    }

    override fun onData(barcode: String) {
        onBarcodeScanned(barcode)
    }
}
