package com.pharmeasy.fetchr.model

import java.time.LocalDateTime

data class Profile(
    val id: String,
    val token: String,
    val email: String,
    val name: String,
    val displayName: String,
    val mobile: String,
    val status: String,
    val userId: String,
    val roles: List<Role> = mutableListOf(),
    val lastBreakTime: String? = null,
    val jitPickingEnabled: Boolean? = false
)
