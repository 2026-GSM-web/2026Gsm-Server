package org.example.schoolweb.global.config

import org.example.schoolweb.global.security.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
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

// 프론트가 별도 origin의 SPA이므로 세션 쿠키 대신 stateless JWT 인증을 사용한다. 이제 JWT를
// httpOnly 쿠키(ACCESS_TOKEN, SameSite=None)로도 전달하므로 브라우저가 상태 변경 요청에 이
// 쿠키를 자동으로 실어 보낼 수 있다는 점에서 예전(Bearer 헤더뿐이던 시절)과 CSRF 전제가
// 달라졌다. 그래도 CSRF를 계속 비활성화해두는 건, 모든 상태 변경 엔드포인트가 @RequestBody로
// application/json만 받고(GET으로 상태를 바꾸는 엔드포인트가 없음) JSON Content-Type과
// PUT/DELETE 메서드는 브라우저가 CORS preflight 없이 보낼 수 없는 non-simple 요청이라, CORS
// 설정(명시적 origin만 허용, 아래 참고)을 통과하지 못하는 타 origin에서는 실제 요청 자체가
// 브라우저에서 막히기 때문이다 - 즉 순수 HTML form/img 기반 CSRF는 JSON body를 만들 수 없어
// 애초에 발동하지 않는다. 단, 새 상태 변경 엔드포인트를 추가할 때 GET이나 form-urlencoded로
// 상태를 바꾸게 만들면 이 전제가 깨지므로 유의할 것.
// 학교 SSO 로그인은 Spring Security의 oauth2Login()이 아니라 OAuthController가 소유하는
// GET /api/auth/dg/authorize -> /callback 리다이렉트로 처리한다 - 그래서 oauth2Login 관련
// 설정이 없다.
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
                authorize("/api/auth/dg/authorize", permitAll)
                authorize("/api/auth/dg/callback", permitAll)
                // 공약 이행률 조회는 프론트의 공개 페이지(비로그인 방문자도 봄)에서 쓰이므로
                // GET만 permitAll. 수정(PUT)은 anyRequest.authenticated() + 컨트롤러의
                // @PreAuthorize("hasRole('ADMIN')")로 계속 보호된다.
                authorize(HttpMethod.GET, "/api/pledge-progress", permitAll)
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
            // ACCESS_TOKEN이 httpOnly 쿠키로 전달되므로 브라우저가 쿠키를 실어 보내려면
            // credentials 허용이 필요하다 (allowedOrigins가 와일드카드가 아니라 명시적 origin
            // 목록이라 credentials=true와 함께 써도 안전하다).
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
