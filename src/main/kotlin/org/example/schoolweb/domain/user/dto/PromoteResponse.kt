package org.example.schoolweb.domain.user.dto

import org.example.schoolweb.domain.user.entity.Role

data class PromoteResponse(
    val role: Role,
    val message: String
)
