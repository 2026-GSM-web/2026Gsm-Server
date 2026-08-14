package org.example.schoolweb.domain.suggestion.dto

import jakarta.validation.constraints.NotBlank

data class SuggestionCreateRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String
)
