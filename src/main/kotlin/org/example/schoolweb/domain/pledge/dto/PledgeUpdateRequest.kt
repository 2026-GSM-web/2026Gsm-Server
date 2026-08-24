package org.example.schoolweb.domain.pledge.dto

import jakarta.validation.constraints.NotBlank

data class PledgeUpdateRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String,
    val category: String?,
    val completed: Boolean,
    val displayOrder: Int
)
