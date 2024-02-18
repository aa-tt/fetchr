package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.ItemStatus
import java.io.Serializable

data class ProductLotItem(
    val id: Long,
    val taskId: Long? = 0,
    val name: String,
    val ucode: String,
    val packForm: String,
    val batchNumber: String,
    val expiry: String,
    val mrp: Double,
    var status: ItemStatus,
    var bin: String? = null
) : Serializable
