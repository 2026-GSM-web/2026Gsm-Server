package org.example.schoolweb.domain.user.dto

import org.example.schoolweb.domain.user.entity.Role

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String?,
    val role: Role
)
