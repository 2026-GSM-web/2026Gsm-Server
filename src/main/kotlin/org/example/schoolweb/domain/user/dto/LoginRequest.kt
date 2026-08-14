package org.example.schoolweb.domain.user.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    val authCode: String,

    @field:NotBlank
    val redirectUri: String
)
