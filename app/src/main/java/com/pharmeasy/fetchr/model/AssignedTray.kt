package com.pharmeasy.fetchr.model

data class AssignedTray(
    val trayId: String,
    val rackerId: String? = null,
    val verifierTaskId: Long? = null,
    val referenceId: String,
    val referenceType: String?
)
