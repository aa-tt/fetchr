package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.TaskType

data class BinLock(
        val bin: String,
        val ucode: String? = null,
        val taskType: TaskType? = null,
        val status: String? = null

)