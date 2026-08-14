package org.example.schoolweb.domain.user.dto

import jakarta.validation.constraints.NotBlank

data class PromoteRequest(
    @field:NotBlank
    val code: String
)
