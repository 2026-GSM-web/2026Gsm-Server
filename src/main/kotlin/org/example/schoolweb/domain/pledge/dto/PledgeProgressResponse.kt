package org.example.schoolweb.domain.pledge.dto

import org.example.schoolweb.domain.pledge.entity.PledgeProgress
import java.time.LocalDateTime

data class PledgeProgressResponse(
    val percentage: Int,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(pledgeProgress: PledgeProgress): PledgeProgressResponse = PledgeProgressResponse(
            percentage = pledgeProgress.percentage,
            updatedAt = pledgeProgress.updatedAt
        )
    }
}
