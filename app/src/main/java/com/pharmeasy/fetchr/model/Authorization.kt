package com.pharmeasy.fetchr.model

data class Authorization(
    val userId: String,
    val profile: Profile,
    val task: AssignedTask?
)
