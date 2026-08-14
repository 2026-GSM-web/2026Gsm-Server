package org.example.schoolweb.global.security

import org.example.schoolweb.domain.user.entity.Role

data class CustomUserPrincipal(
    val userId: Long,
    val oauthProviderId: String,
    val name: String,
    val role: Role
)
