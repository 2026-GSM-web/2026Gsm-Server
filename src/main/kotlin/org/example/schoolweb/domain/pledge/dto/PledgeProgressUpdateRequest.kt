package org.example.schoolweb.domain.pledge.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class PledgeProgressUpdateRequest(
    @field:Min(0)
    @field:Max(100)
    val percentage: Int
)
