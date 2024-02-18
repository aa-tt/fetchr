package com.pharmeasy.fetchr.scanner

interface ScannerActionListener {

    fun onConnected()

    fun onConnecting()

    fun onDisconnected()

    fun onData(barcode: String)
}
