package org.example.schoolweb.global.config

import org.example.schoolweb.global.security.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

// 프론트가 별도 origin의 SPA이므로 세션 쿠키 대신 stateless JWT 인증을 사용한다.
// CSRF는 쿠키 기반 인증이 아니라 위조할 대상 자체가 없으므로 비활성화한다.
// 학교 SSO 로그인은 Spring Security의 oauth2Login()이 아니라 POST /api/auth/login 하나로 처리한다
// (datagsm-oauth-sdk-java 기반 AuthService 참고) - 그래서 oauth2Login 관련 설정이 없다.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsProperties: CorsProperties
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { configurationSource = corsConfigurationSource() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/api/auth/login", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize(anyRequest, authenticated)
            }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = corsProperties.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type")
            allowCredentials = false
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
