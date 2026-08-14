package org.example.schoolweb.domain.user.service

import org.example.schoolweb.domain.user.entity.Role
import org.example.schoolweb.domain.user.entity.User
import org.example.schoolweb.domain.user.repository.UserRepository
import org.example.schoolweb.global.exception.NotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userService = UserService(userRepository)
    }

    @Test
    fun `findOrCreateFromOAuth2 returns the existing user without saving when one already exists`() {
        val existing = User(id = 1L, oauthProviderId = "sso-123", name = "홍길동", email = "s1@gsm.hs.kr")
        given(userRepository.findByOauthProviderId("sso-123")).willReturn(existing)

        val result = userService.findOrCreateFromOAuth2("sso-123", "홍길동(new name)", "other@gsm.hs.kr")

        assertSame(existing, result)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `findOrCreateFromOAuth2 creates a new user when none exists for the oauth id`() {
        given(userRepository.findByOauthProviderId("sso-999")).willReturn(null)
        given(userRepository.save(any())).willAnswer { invocation ->
            val arg = invocation.getArgument<User>(0)
            User(id = 42L, oauthProviderId = arg.oauthProviderId, name = arg.name, email = arg.email)
        }

        val result = userService.findOrCreateFromOAuth2("sso-999", "김학생", "s2@gsm.hs.kr")

        assertEquals(42L, result.id)
        assertEquals("sso-999", result.oauthProviderId)
        assertEquals("김학생", result.name)
        assertEquals(Role.USER, result.role)
    }

    @Test
    fun `promoteToAdmin upgrades an existing user's role to ADMIN`() {
        val user = User(id = 7L, oauthProviderId = "sso-7", name = "관리자후보")
        given(userRepository.findById(7L)).willReturn(Optional.of(user))
        given(userRepository.save(any())).willAnswer { it.getArgument(0) }

        val result = userService.promoteToAdmin(7L)

        assertEquals(Role.ADMIN, result.role)
        assertEquals(Role.ADMIN, user.role)
    }

    @Test
    fun `promoteToAdmin throws NotFoundException when the user does not exist`() {
        given(userRepository.findById(404L)).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> { userService.promoteToAdmin(404L) }
    }

    @Test
    fun `getById throws NotFoundException when the user does not exist`() {
        given(userRepository.findById(999L)).willReturn(Optional.empty())

        assertFailsWith<NotFoundException> { userService.getById(999L) }
    }
}
