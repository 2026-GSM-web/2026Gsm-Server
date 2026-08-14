package org.example.schoolweb.domain.suggestion.service

import org.example.schoolweb.domain.suggestion.entity.Suggestion
import org.example.schoolweb.domain.suggestion.repository.SuggestionRepository
import org.example.schoolweb.domain.suggestion.entity.SuggestionStatus
import org.example.schoolweb.domain.user.entity.Role
import org.example.schoolweb.domain.user.service.UserService
import org.example.schoolweb.domain.suggestion.dto.SuggestionCreateRequest
import org.example.schoolweb.domain.suggestion.dto.SuggestionResponse
import org.example.schoolweb.domain.suggestion.dto.SuggestionUpdateRequest
import org.example.schoolweb.domain.suggestion.dto.SuggestionUpdateStatusRequest
import org.example.schoolweb.global.exception.ForbiddenException
import org.example.schoolweb.global.exception.NotFoundException
import org.example.schoolweb.global.security.CustomUserPrincipal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SuggestionService(
    private val suggestionRepository: SuggestionRepository,
    private val userService: UserService
) {

    @Transactional
    fun create(principal: CustomUserPrincipal, request: SuggestionCreateRequest): SuggestionResponse {
        val author = userService.getById(principal.userId)
        val suggestion = suggestionRepository.save(
            Suggestion(title = request.title, content = request.content, author = author)
        )
        return SuggestionResponse.from(suggestion)
    }

    @Transactional(readOnly = true)
    fun getMine(principal: CustomUserPrincipal, pageable: Pageable): Page<SuggestionResponse> =
        suggestionRepository.findAllByAuthorId(principal.userId, pageable).map { SuggestionResponse.from(it) }

    @Transactional(readOnly = true)
    fun getAllForAdmin(status: SuggestionStatus?, pageable: Pageable): Page<SuggestionResponse> {
        val page = if (status != null) {
            suggestionRepository.findAllByStatus(status, pageable)
        } else {
            suggestionRepository.findAll(pageable)
        }
        return page.map { SuggestionResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getById(principal: CustomUserPrincipal, id: Long): SuggestionResponse {
        val suggestion = findOrThrow(id)
        checkAccess(principal, suggestion)
        return SuggestionResponse.from(suggestion)
    }

    @Transactional
    fun update(principal: CustomUserPrincipal, id: Long, request: SuggestionUpdateRequest): SuggestionResponse {
        val suggestion = findOrThrow(id)
        checkAccess(principal, suggestion)
        suggestion.title = request.title
        suggestion.content = request.content
        return SuggestionResponse.from(suggestion)
    }

    @Transactional
    fun updateStatus(id: Long, request: SuggestionUpdateStatusRequest): SuggestionResponse {
        val suggestion = findOrThrow(id)
        suggestion.status = request.status
        suggestion.adminReply = request.adminReply
        return SuggestionResponse.from(suggestion)
    }

    @Transactional
    fun delete(principal: CustomUserPrincipal, id: Long) {
        val suggestion = findOrThrow(id)
        checkAccess(principal, suggestion)
        suggestionRepository.delete(suggestion)
    }

    private fun findOrThrow(id: Long): Suggestion =
        suggestionRepository.findById(id)
            .orElseThrow { NotFoundException("건의사항을 찾을 수 없습니다. id=$id") }

    private fun checkAccess(principal: CustomUserPrincipal, suggestion: Suggestion) {
        val isAuthor = suggestion.author.id == principal.userId
        val isAdmin = principal.role == Role.ADMIN
        if (!isAuthor && !isAdmin) {
            throw ForbiddenException("본인이 작성한 건의사항만 접근할 수 있습니다.")
        }
    }
}
