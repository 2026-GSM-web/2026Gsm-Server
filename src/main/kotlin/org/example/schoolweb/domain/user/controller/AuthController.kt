package org.example.schoolweb.domain.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.example.schoolweb.global.config.AdminProperties
import org.example.schoolweb.domain.user.dto.LoginRequest
import org.example.schoolweb.domain.user.dto.LoginResponse
import org.example.schoolweb.domain.user.dto.PromoteRequest
import org.example.schoolweb.domain.user.dto.PromoteResponse
import org.example.schoolweb.domain.user.dto.UserResponse
import org.example.schoolweb.global.exception.ForbiddenException
import org.example.schoolweb.global.security.CustomUserPrincipal
import org.example.schoolweb.domain.user.service.AuthService
import org.example.schoolweb.domain.user.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "학교 SSO 로그인, 내 정보 조회, 관리자 승격")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
    private val adminProperties: AdminProperties
) {

    @PostMapping("/login")
    @Operation(
        summary = "학교 SSO 로그인",
        description = "프론트가 학교 SSO 인증 후 받은 authCode와, 인증 요청 시 사용한 redirectUri를 넘기면 " +
            "학생 계정인지 검증한 뒤 최초 로그인 시 자동 회원가입하고 JWT를 발급한다."
    )
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
        LoginResponse(authService.loginWithSchoolOAuth(request.authCode, request.redirectUri))

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "JWT로 인증된 현재 로그인 사용자의 정보를 반환한다.")
    fun me(@AuthenticationPrincipal principal: CustomUserPrincipal): UserResponse {
        val user = userService.getById(principal.userId)
        return UserResponse(user.id!!, user.name, user.email, user.role)
    }

    @PostMapping("/promote")
    @Operation(
        summary = "관리자로 승격",
        description = "서버 관리자가 미리 알려준 마스터 코드(`ADMIN_MASTER_CODE`)를 제출하면 본인 계정을 ADMIN으로 승격한다."
    )
    fun promote(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: PromoteRequest
    ): PromoteResponse {
        if (request.code != adminProperties.masterCode) {
            throw ForbiddenException("관리자 코드가 올바르지 않습니다.")
        }
        val user = userService.promoteToAdmin(principal.userId)
        return PromoteResponse(user.role, "관리자로 승격되었습니다.")
    }
}
