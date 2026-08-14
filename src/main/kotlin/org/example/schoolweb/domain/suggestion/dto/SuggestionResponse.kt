package org.example.schoolweb.domain.suggestion.dto

import org.example.schoolweb.domain.suggestion.entity.Suggestion
import org.example.schoolweb.domain.suggestion.entity.SuggestionStatus
import java.time.LocalDateTime

data class SuggestionResponse(
    val id: Long,
    val title: String,
    val content: String,
    val authorId: Long,
    val authorName: String,
    val status: SuggestionStatus,
    val adminReply: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        // author는 LAZY 연관관계이므로 반드시 트랜잭션이 열려 있는 서비스 계층에서 변환할 것
        fun from(suggestion: Suggestion): SuggestionResponse = SuggestionResponse(
            id = suggestion.id!!,
            title = suggestion.title,
            content = suggestion.content,
            authorId = suggestion.author.id!!,
            authorName = suggestion.author.name,
            status = suggestion.status,
            adminReply = suggestion.adminReply,
            createdAt = suggestion.createdAt,
            updatedAt = suggestion.updatedAt
        )
    }
}
