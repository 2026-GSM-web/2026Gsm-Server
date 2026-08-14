package org.example.schoolweb.domain.suggestion.service

import org.example.schoolweb.domain.suggestion.dto.SuggestionCreateRequest
import org.example.schoolweb.domain.suggestion.dto.SuggestionUpdateRequest
import org.example.schoolweb.domain.suggestion.dto.SuggestionUpdateStatusRequest
import org.example.schoolweb.domain.suggestion.entity.Suggestion
import org.example.schoolweb.domain.suggestion.entity.SuggestionStatus
import org.example.schoolweb.domain.suggestion.repository.SuggestionRepository
import org.example.schoolweb.domain.user.entity.Role
import org.example.schoolweb.domain.user.entity.User
import org.example.schoolweb.domain.user.service.UserService
import org.example.schoolweb.global.exception.ForbiddenException
import org.example.schoolweb.global.exception.NotFoundException
import org.example.schoolweb.global.security.CustomUserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SuggestionServiceTest {

    private lateinit var suggestionRepository: SuggestionRepository
    private lateinit var userService: UserService
    private lateinit var suggestionService: SuggestionService

    private val author = User(id = 1L, oauthProviderId = "sso-1", name = "작성자")
    private val otherUser = User(id = 2L, oauthProviderId = "sso-2", name = "다른학생")
    private val authorPrincipal = CustomUserPrincipal(userId = 1L, oauthProviderId = "sso-1", name = "작성자", role = Role.USER)
    private val otherPrincipal = CustomUserPrincipal(userId = 2L, oauthProviderId = "sso-2", name = "다른학생", role = Role.USER)
    private val adminPrincipal = CustomUserPrincipal(userId = 99L, oauthProviderId = "sso-99", name = "관리자", role = Role.ADMIN)

    @BeforeEach
    fun setUp() {
        suggestionRepository = mock(SuggestionRepository::class.java)
        userService = mock(UserService::class.java)
        suggestionService = SuggestionService(suggestionRepository, userService)
    }

    private fun suggestion(
        id: Long = 10L,
        author: User = this.author,
        status: SuggestionStatus = SuggestionStatus.RECEIVED
    ) = Suggestion(id = id, title = "제목", content = "내용", author = author, status = status)

    @Test
    fun `create saves a suggestion authored by the requesting user`() {
        given(userService.getById(1L)).willReturn(author)
        given(suggestionRepository.save(any())).willAnswer { invocation ->
            val arg = invocation.getArgument<Suggestion>(0)
            Suggestion(id = 10L, title = arg.title, content = arg.content, author = arg.author, status = arg.status)
        }

        val result = suggestionService.create(authorPrincipal, SuggestionCreateRequest(title = "건의합니다", content = "내용입니다"))

        assertEquals(10L, result.id)
        assertEquals("건의합니다", result.title)
        assertEquals(1L, result.authorId)
        assertEquals(SuggestionStatus.RECEIVED, result.status)
    }

    @Test
    fun `getAllForAdmin without a status filter queries findAll`() {
        val pageable: Pageable = PageRequest.of(0, 10)
        given(suggestionRepository.findAll(pageable)).willReturn(PageImpl(listOf(suggestion())))

        val result = suggestionService.getAllForAdmin(null, pageable)

        assertEquals(1, result.totalElements)
        verify(suggestionRepository, never()).findAllByStatus(any(), any())
    }

    @Test
    fun `getAllForAdmin with a status filter queries findAllByStatus instead of findAll`() {
        val pageable: Pageable = PageRequest.of(0, 10)
        given(suggestionRepository.findAllByStatus(eq(SuggestionStatus.RESOLVED), eq(pageable)))
            .willReturn(PageImpl(listOf(suggestion(status = SuggestionStatus.RESOLVED))))

        val result = suggestionService.getAllForAdmin(SuggestionStatus.RESOLVED, pageable)

        assertEquals(1, result.totalElements)
        verify(suggestionRepository, never()).findAll(any<Pageable>())
    }

    @Test
    fun `getById allows the author to read their own suggestion`() {
        given(suggestionRepository.findById(10L)).willReturn(Optional.of(suggestion()))

        val result = suggestionService.getById(authorPrincipal, 10L)

        assertEquals(10L, result.id)
    }

    @Test
    fun `getById allows an admin to read a suggestion they did not author`() {
        given(suggestionRepository.findById(10L)).willReturn(Optional.of(suggestion()))

        val result = suggestionService.getById(adminPrincipal, 10L)

        assertEquals(10L, result.id)
    }

    @Test
    fun `getById rejects a user who is neither the author nor an admin`() {
        given(suggestionRepository.findById(10L)).willReturn(Optional.of(suggestion()))

        assertFailsWith<ForbiddenException> { suggestionService.getById(otherPrincipal, 10L) }
    }

    @Test
    fun `getById throws NotFoundException for a missing suggestion`() {
        given(suggestionRepository.findById(404L)).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> { suggestionService.getById(authorPrincipal, 404L) }
    }

    @Test
    fun `update allows the author to edit their own suggestion`() {
        val target = suggestion()
        given(suggestionRepository.findById(10L)).willReturn(Optional.of(target))

        val result = suggestionService.update(authorPrincipal, 10L, SuggestionUpdateRequest(title = "수정된 제목", content = "수정된 내용"))

        assertEquals("수정된 제목", result.title)
        assertEquals("수정된 내용", result.content)
    }

    @Test
    fun `update allows an admin to edit a suggestion they did not author`() {
        val target = suggestion()
        given(suggestionRepository.findById(10L)).willReturn(Optional.of(target))

        val result = suggestionService.update(adminPrincipal, 10L, SuggestionUpdateRequest(title = "관리자 수정", content = "관리자 내용"))

        assertEquals("관리자 수정", result.title)
    }

    @Test
    fun `update rejects a caller who is neither the author nor an admin`() {
        val target = suggestion()
        given(suggestionRepository.findById(10L)).willReturn(Optional.of(target))

        assertFailsWith<ForbiddenException> {
            suggestionService.update(otherPrincipal, 10L, SuggestionUpdateRequest(title = "무단 수정", content = "무단 내용"))
        }
    }

    @Test
    fun `update throws NotFoundException for a missing suggestion`() {
        given(suggestionRepository.findById(404L)).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> {
            suggestionService.update(authorPrincipal, 404L, SuggestionUpdateRequest(title = "제목", content = "내용"))
        }
    }

    @Test
    fun `updateStatus overwrites status and admin reply regardless of who calls it`() {
        val target = suggestion()
        given(suggestionRepository.findById(10L)).willReturn(Optional.of(target))

        val result = suggestionService.updateStatus(10L, SuggestionUpdateStatusRequest(SuggestionStatus.RESOLVED, "처리했습니다"))

        assertEquals(SuggestionStatus.RESOLVED, result.status)
        assertEquals("처리했습니다", result.adminReply)
    }

    @Test
    fun `delete removes the suggestion when the caller is the author`() {
        val target = suggestion()
        given(suggestionRepository.findById(10L)).willReturn(Optional.of(target))

        suggestionService.delete(authorPrincipal, 10L)

        verify(suggestionRepository, times(1)).delete(target)
    }

    @Test
    fun `delete rejects a caller who is neither the author nor an admin`() {
        val target = suggestion()
        given(suggestionRepository.findById(10L)).willReturn(Optional.of(target))

        assertFailsWith<ForbiddenException> { suggestionService.delete(otherPrincipal, 10L) }
        verify(suggestionRepository, never()).delete(any())
    }
}
