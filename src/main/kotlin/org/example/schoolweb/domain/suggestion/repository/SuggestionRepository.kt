package org.example.schoolweb.domain.suggestion.repository

import org.example.schoolweb.domain.suggestion.entity.Suggestion
import org.example.schoolweb.domain.suggestion.entity.SuggestionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SuggestionRepository : JpaRepository<Suggestion, Long> {
    fun findAllByAuthorId(authorId: Long, pageable: Pageable): Page<Suggestion>
    fun findAllByStatus(status: SuggestionStatus, pageable: Pageable): Page<Suggestion>
}
