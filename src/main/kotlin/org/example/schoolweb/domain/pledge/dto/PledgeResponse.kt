package org.example.schoolweb.domain.pledge.dto

import org.example.schoolweb.domain.pledge.entity.Pledge
import org.example.schoolweb.domain.pledge.entity.PledgeStatus
import java.time.LocalDateTime

data class PledgeResponse(
    val id: Long,
    val title: String,
    val content: String,
    val category: String?,
    val status: PledgeStatus,
    val subStatus: String?,
    val displayOrder: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(pledge: Pledge): PledgeResponse = PledgeResponse(
            id = pledge.id!!,
            title = pledge.title,
            content = pledge.content,
            category = pledge.category,
            status = pledge.status,
            subStatus = pledge.subStatus,
            displayOrder = pledge.displayOrder,
            createdAt = pledge.createdAt,
            updatedAt = pledge.updatedAt
        )
    }
}
