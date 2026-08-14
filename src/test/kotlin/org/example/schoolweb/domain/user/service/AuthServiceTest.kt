package org.example.schoolweb.domain.user.service

import org.example.schoolweb.domain.user.entity.User
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
    private lateinit var userService: UserService
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        dataGsmOAuthClient = mock(DataGsmOAuthClient::class.java)
        userService = mock(UserService::class.java)
        jwtTokenProvider = mock(JwtTokenProvider::class.java)
        authService = AuthService(dataGsmOAuthClient, userService, jwtTokenProvider)
    }

    @Test
    fun `loginWithSchoolOAuth exchanges the code, upserts the user, and issues a JWT`() {
        val tokenResponse = TokenResponse("access-token", "Bearer", 3600L, null, "datagsm:self_read")
        val student = Student().apply {
            id = 12345L
            name = "홍길동"
            email = "s12345@gsm.hs.kr"
        }
        val userInfo = UserInfo().apply {
            objectType = AccountObjectType.STUDENT
            this.student = student
        }
        val user = User(id = 1L, oauthProviderId = "12345", name = "홍길동", email = "s12345@gsm.hs.kr")

        given(dataGsmOAuthClient.exchangeCodeForToken("auth-code", "https://fe.example.com/callback"))
            .willReturn(tokenResponse)
        given(dataGsmOAuthClient.getUserInfo("access-token")).willReturn(userInfo)
        given(userService.findOrCreateFromOAuth2("12345", "홍길동", "s12345@gsm.hs.kr")).willReturn(user)
        given(jwtTokenProvider.generateToken(user)).willReturn("jwt-token")

        val result = authService.loginWithSchoolOAuth("auth-code", "https://fe.example.com/callback")

        assertEquals("jwt-token", result)
    }

    @Test
    fun `loginWithSchoolOAuth rejects non-student accounts`() {
        val tokenResponse = TokenResponse("access-token", "Bearer", 3600L, null, null)
        val userInfo = UserInfo().apply { objectType = AccountObjectType.TEACHER }

        given(dataGsmOAuthClient.exchangeCodeForToken(any(), any())).willReturn(tokenResponse)
        given(dataGsmOAuthClient.getUserInfo("access-token")).willReturn(userInfo)

        assertFailsWith<ForbiddenException> {
            authService.loginWithSchoolOAuth("auth-code", "https://fe.example.com/callback")
        }
    }

    @Test
    fun `loginWithSchoolOAuth wraps SDK errors as ForbiddenException`() {
        given(dataGsmOAuthClient.exchangeCodeForToken(any(), any()))
            .willThrow(UnauthorizedException("invalid code"))

        assertFailsWith<ForbiddenException> {
            authService.loginWithSchoolOAuth("bad-code", "https://fe.example.com/callback")
        }
    }
}
