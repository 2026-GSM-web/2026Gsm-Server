package org.example.schoolweb.domain.pledge.dto

import org.example.schoolweb.domain.pledge.entity.Pledge
import java.time.LocalDateTime

data class PledgeResponse(
    val id: Long,
    val title: String,
    val content: String,
    val category: String?,
    val completed: Boolean,
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
            completed = pledge.completed,
            displayOrder = pledge.displayOrder,
            createdAt = pledge.createdAt,
            updatedAt = pledge.updatedAt
        )
    }
}
