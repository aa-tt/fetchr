package com.pharmeasy.fetchr.model

data class JsonData(
        var barcode: String? = null,
        var taskId: Long? = null,
        var quantity: Int? = null,
        var ucode: String? = null,
        var version: String? = null)