package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.ItemStatus
import java.io.Serializable

data class AuditorItem(
    val taskId: Long,
    val name: String,
    val ucode: String,
    val packForm: String? = null,
    val packQuantity: Int? = null,
    val binId: String,
    var availableQuantity: Long = 0,
    var status: ItemStatus? = null,
    var trayId: String? = null
) : Serializable
