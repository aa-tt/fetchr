package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.TaskStatus
import java.io.Serializable

data class TrayItem(
    val trayId: String,
    val name: String,
    val ucode: String,
    val packForm: String,
    val batch: String,
    var count: Int
) : Serializable
