package org.example.schoolweb.global.security.oauth2

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.schoolweb.global.config.FrontendProperties
import org.example.schoolweb.domain.user.repository.UserRepository
import org.example.schoolweb.global.security.jwt.JwtTokenProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// 프론트가 완전히 별도 origin이라 세션 쿠키 대신 JWT를 발급해 리다이렉트로 전달한다.
// 토큰은 쿼리스트링이 아닌 URL 프래그먼트(#token=...)에 실어 서버 로그/Referer에 남지 않게 한다.
@Component
class OAuth2LoginSuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val frontendProperties: FrontendProperties
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oAuth2User = authentication.principal as OAuth2User
        val internalUserId = oAuth2User.attributes["internalUserId"] as Long
        val user = userRepository.findById(internalUserId)
            .orElseThrow { IllegalStateException("OAuth2 로그인 직후 사용자를 찾을 수 없습니다. id=$internalUserId") }

        val token = jwtTokenProvider.generateToken(user)
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8)
        response.sendRedirect("${frontendProperties.oauth2RedirectUri}#token=$encodedToken")
    }
}
