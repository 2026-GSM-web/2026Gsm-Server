package org.example.schoolweb.domain.pledge.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.example.schoolweb.domain.pledge.entity.PledgeStatus

data class PledgeUpdateRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String,
    val category: String?,
    @field:NotNull
    val status: PledgeStatus,
    val displayOrder: Int
)
