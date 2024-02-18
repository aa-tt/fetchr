package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.type.Source

data class TaskTray(
    val sourceId: String,
    val sourceType: Source,
    val trayId: String,
    val referenceId: String,
    val referenceType: String?,
    val trayPickedZone: String?
)
