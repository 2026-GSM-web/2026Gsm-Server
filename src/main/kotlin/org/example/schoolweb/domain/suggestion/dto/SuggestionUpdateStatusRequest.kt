package org.example.schoolweb.domain.suggestion.dto

import org.example.schoolweb.domain.suggestion.entity.SuggestionStatus

data class SuggestionUpdateStatusRequest(
    val status: SuggestionStatus,
    val adminReply: String? = null
)
