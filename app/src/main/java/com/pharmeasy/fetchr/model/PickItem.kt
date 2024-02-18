package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.ItemStatus
import java.io.Serializable

data class PickItem(
    val taskId: Long,
    val name: String,
    val ucode: String,
    val packForm: String,
    val packQuantity: Int,
    val binId: String,
    var orderedQuantity: Int,
    var status: ItemStatus,
    var refrigerated: String? = null,
    var trayId: String? = null
) : Serializable
