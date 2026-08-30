package org.example.schoolweb.domain.pledge.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.example.schoolweb.domain.pledge.entity.PledgeStatus

data class PledgeCreateRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:NotBlank
    val content: String,
    @field:Size(max = 100)
    val category: String?,
    @field:NotNull
    val status: PledgeStatus,
    @field:Size(max = 255)
    val subStatus: String?,
    @field:PositiveOrZero
    val displayOrder: Int
)
