package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.TaskType

data class BinParam(
        val binId: String,
        val ucode: String? = null,
        val taskType: TaskType? = null,
        val status: String? = null,
        val trayId: String? = null

)