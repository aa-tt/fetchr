package com.pharmeasy.fetchr.model

import com.pharmeasy.fetchr.service.SessionService

data class UserStatus(
    val userId: String,
    val action: String,
    val role: String = SessionService.role
)
