package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.ItemStatus
import java.io.Serializable

data class ProductItem(
    val taskId: Long,
    val name: String,
    val ucode: String,
    val packForm: String,
    val binId: String,
    val batches: Int,
    val total: Int,
    var status: ItemStatus
) : Serializable
