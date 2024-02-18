package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.ItemStatus
import java.io.Serializable

data class BinItem(
    val taskId: Long,
    val binId: String,
    val ucodes: Int,
    val total: Int,
    var status: ItemStatus
) : Serializable
