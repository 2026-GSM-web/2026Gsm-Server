package org.example.schoolweb.domain.user.service

import org.example.schoolweb.domain.user.entity.User
import org.example.schoolweb.global.config.SchoolOAuthProperties
import org.example.schoolweb.global.exception.ForbiddenException
import org.example.schoolweb.global.security.jwt.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient
import team.themoment.datagsm.sdk.oauth.exception.UnauthorizedException
import team.themoment.datagsm.sdk.oauth.model.AccountObjectType
import team.themoment.datagsm.sdk.oauth.model.Student
import team.themoment.datagsm.sdk.oauth.model.TokenResponse
import team.themoment.datagsm.sdk.oauth.model.UserInfo
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthServiceTest {

    private lateinit var dataGsmOAuthClient: DataGsmOAuthClient
    private lateinit var schoolOAuthProperties: SchoolOAuthProperties
    private lateinit var oAuthStateStore: OAuthStateStore
    private lateinit var userService: UserService
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var authService: AuthService

    private val redirectUri = "https://api.example.com/api/auth/dg/callback"

    @BeforeEach
    fun setUp() {
        dataGsmOAuthClient = mock(DataGsmOAuthClient::class.java)
        schoolOAuthProperties = SchoolOAuthProperties(
            clientId = "client-id",
            clientSecret = "client-secret",
            redirectUri = redirectUri
        )
        oAuthStateStore = mock(OAuthStateStore::class.java)
        userService = mock(UserService::class.java)
        jwtTokenProvider = mock(JwtTokenProvider::class.java)
        authService = AuthService(dataGsmOAuthClient, schoolOAuthProperties, oAuthStateStore, userService, jwtTokenProvider)
    }

    @Test
    fun `completeSchoolOAuthLogin consumes state, exchanges the code, upserts the user, and issues a JWT`() {
        val tokenResponse = TokenResponse("access-token", "Bearer", 3600L, null, "datagsm:self_read")
        val student = Student().apply {
            id = 12345L
            name = "홍길동"
            email = "s12345@gsm.hs.kr"
        }
        val userInfo = UserInfo().apply {
            id = 999L
            objectType = AccountObjectType.STUDENT
            this.student = student
        }
        val user = User(id = 1L, oauthProviderId = "999", name = "홍길동", email = "s12345@gsm.hs.kr")

        given(oAuthStateStore.consume("valid-state")).willReturn(true)
        given(dataGsmOAuthClient.exchangeCodeForToken("auth-code", redirectUri)).willReturn(tokenResponse)
        given(dataGsmOAuthClient.getUserInfo("access-token")).willReturn(userInfo)
        given(userService.findOrCreateFromOAuth2("999", "홍길동", "s12345@gsm.hs.kr")).willReturn(user)
        given(jwtTokenProvider.generateToken(user)).willReturn("jwt-token")

        val result = authService.completeSchoolOAuthLogin("auth-code", "valid-state")

        assertEquals("jwt-token", result)
    }

    @Test
    fun `completeSchoolOAuthLogin rejects an invalid or expired state without touching DG`() {
        given(oAuthStateStore.consume("bad-state")).willReturn(false)

        assertFailsWith<ForbiddenException> {
            authService.completeSchoolOAuthLogin("auth-code", "bad-state")
        }
    }

    @Test
    fun `completeSchoolOAuthLogin rejects non-student accounts`() {
        val tokenResponse = TokenResponse("access-token", "Bearer", 3600L, null, null)
        val userInfo = UserInfo().apply { objectType = AccountObjectType.TEACHER }

        given(oAuthStateStore.consume(any())).willReturn(true)
        given(dataGsmOAuthClient.exchangeCodeForToken(any(), any())).willReturn(tokenResponse)
        given(dataGsmOAuthClient.getUserInfo("access-token")).willReturn(userInfo)

        assertFailsWith<ForbiddenException> {
            authService.completeSchoolOAuthLogin("auth-code", "valid-state")
        }
    }

    @Test
    fun `completeSchoolOAuthLogin wraps SDK errors as ForbiddenException`() {
        given(oAuthStateStore.consume(any())).willReturn(true)
        given(dataGsmOAuthClient.exchangeCodeForToken(any(), any()))
            .willThrow(UnauthorizedException("invalid code"))

        assertFailsWith<ForbiddenException> {
            authService.completeSchoolOAuthLogin("bad-code", "valid-state")
        }
    }
}
