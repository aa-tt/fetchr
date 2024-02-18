package com.pharmeasy.fetchr.model

data class GetInventory(
        val elements: Int,
        val pages: Int,
        val available: Long
        //val data: List<BarcodeInfo> = mutableListOf()
)