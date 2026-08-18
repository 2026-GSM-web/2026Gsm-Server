package org.example.schoolweb.domain.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.schoolweb.domain.user.service.AuthService
import org.example.schoolweb.global.config.JwtProperties
import org.example.schoolweb.global.config.SchoolOAuthProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Duration

// 백엔드가 DG(SSO) 리다이렉트 체인을 직접 소유한다: 프론트는 /authorize로 사용자를 보내기만 하면
// 되고, code/state를 다루거나 토큰을 저장하는 책임이 없다. 로그인 성공 시 발급한 JWT는 JSON
// 바디가 아니라 httpOnly 쿠키로 내려주고, 프론트의 다음 화면으로 302 리다이렉트한다.
@RestController
@RequestMapping("/api/auth/dg")
@Tag(name = "DG OAuth", description = "DG(DataGSM) SSO backend-owned 리다이렉트 로그인")
class OAuthController(
    private val authService: AuthService,
    private val schoolOAuthProperties: SchoolOAuthProperties,
    private val jwtProperties: JwtProperties
) {

    @GetMapping("/authorize")
    @Operation(
        summary = "DG 인가 페이지로 리다이렉트",
        description = "state를 발급해 Redis에 저장한 뒤 DG 로그인 동의 화면으로 302 리다이렉트한다."
    )
    fun authorize(): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(authService.buildAuthorizationUrl()))
            .build()

    @GetMapping("/callback")
    @Operation(
        summary = "DG 콜백 처리",
        description = "DG가 code/state를 붙여 리다이렉트하는 엔드포인트. state 검증 후 code->token->" +
            "userinfo 교환을 거쳐 JWT를 httpOnly 쿠키로 세팅하고, 프론트의 다음 화면으로 리다이렉트한다."
    )
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String
    ): ResponseEntity<Void> {
        val token = authService.completeSchoolOAuthLogin(code, state)

        val redirectTarget = schoolOAuthProperties.webCallbackRedirectUrl.ifBlank {
            // 값이 아직 확정되지 않았다 - 임의로 채우지 않고 명확히 실패시킨다.
            throw IllegalStateException(
                "APP_WEB_OAUTH_CALLBACK_REDIRECT_URL이 설정되지 않았습니다."
            )
        }

        val cookie = ResponseCookie.from("ACCESS_TOKEN", token)
            .httpOnly(true)
            .secure(schoolOAuthProperties.cookieSecure)
            .sameSite("None")
            .path("/")
            .maxAge(Duration.ofMillis(jwtProperties.expirationMs))
            .build()

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .location(URI.create(redirectTarget))
            .build()
    }
}
