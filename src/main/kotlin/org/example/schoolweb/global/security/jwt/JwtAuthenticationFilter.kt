package org.example.schoolweb.global.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.schoolweb.domain.user.repository.UserRepository
import org.example.schoolweb.global.security.CustomUserPrincipal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        val bearerToken = header?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        // OAuthController가 로그인 성공 시 심는 httpOnly 쿠키(ACCESS_TOKEN)로도 인증할 수 있게
        // 헤더가 없을 때 쿠키로 폴백한다.
        val cookieToken = request.cookies?.firstOrNull { it.name == "ACCESS_TOKEN" }?.value
        val token = bearerToken ?: cookieToken

        if (token != null && jwtTokenProvider.validateToken(token)) {
            userRepository.findById(jwtTokenProvider.getUserId(token)).ifPresent { user ->
                val principal = CustomUserPrincipal(
                    userId = user.id!!,
                    oauthProviderId = user.oauthProviderId,
                    name = user.name,
                    role = user.role
                )
                val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role}"))
                val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
