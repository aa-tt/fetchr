package com.pharmeasy.fetchr.model

data class GetBarcodes(
        var _id: Int,
        val batchId: Int,
        val batch: String,
        val barcode: String
)