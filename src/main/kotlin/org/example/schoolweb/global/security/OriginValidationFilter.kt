package org.example.schoolweb.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.schoolweb.global.config.CorsProperties
import org.example.schoolweb.global.exception.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.net.URI

// CORS(corsConfigurationSource, SecurityConfig 참고)가 이미 명시적 origin 화이트리스트 +
// credentials 조합으로 실제 크로스 오리진 요청 자체를 브라우저 단에서 막아주지만, 이건
// "CORS 설정 실수"(예: 화이트리스트에 잘못된 값이 들어가는 경우)나 "새 상태 변경 엔드포인트를
// 추가하면서 실수로 GET/form-urlencoded처럼 preflight를 우회하는 조건으로 만드는 경우"까지
// 막아주진 못한다. 이 필터는 그런 실수들에 대한 defense-in-depth로, Origin/Referer를 서버가
// 직접 한 번 더 검증한다.
//
// 검사 대상은 "상태 변경 메서드 + ACCESS_TOKEN 쿠키가 실린 요청"으로 한정한다. CSRF는 브라우저가
// 조건 없이 자동으로 실어 보내는 ambient credential(쿠키)을 악용하는 공격이라, 쿠키가 없는
// 요청(Authorization: Bearer 헤더만으로 인증하는 요청)은 애초에 CSRF 대상이 아니다 - 공격자
// 페이지는 커스텀 헤더를 실을 수 없고, 커스텀 헤더가 실린 요청은 그 자체로 non-simple이라 이미
// CORS preflight를 통과해야 하기 때문이다. 이 범위를 넘어 Bearer 전용 요청까지 막으면 curl/모바일
// 앱처럼 Origin/Referer가 없는 정상 비브라우저 클라이언트가 불필요하게 깨진다.
// ObjectMapper는 반드시 tools.jackson.databind(Jackson 3)에서 import해야 한다 - Spring Boot
// 4.1부터 spring-boot-starter-web이 끌어오는 자동설정(spring-boot-starter-jackson ->
// spring-boot-jackson의 JacksonAutoConfiguration)이 tools.jackson.databind.json.JsonMapper
// 빈만 등록하고, 예전 Jackson 2 패키지(com.fasterxml.jackson.databind.ObjectMapper)는 빈으로
// 등록해주지 않는다. jackson-databind 2.x 자체는 jjwt-jackson/springdoc 같은 다른 라이브러리가
// 끌어오는 전이 의존성으로 클래스패스에 여전히 존재하지만, 그 라이브러리들이 내부적으로 직접
// 쓸 뿐 Spring 빈으로 노출되진 않는다 - com.fasterxml.jackson.databind.ObjectMapper를 생성자
// 주입하면 NoSuchBeanDefinitionException으로 기동 자체가 실패한다(실제 운영 배포에서 재현됨).
@Component
class OriginValidationFilter(
    private val corsProperties: CorsProperties,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val isStateChanging = request.method.uppercase() in STATE_CHANGING_METHODS
        val hasAccessTokenCookie = request.cookies?.any { it.name == "ACCESS_TOKEN" } == true

        if (!isStateChanging || !hasAccessTokenCookie) {
            filterChain.doFilter(request, response)
            return
        }

        val allowedOrigins = corsProperties.trimmedAllowedOrigins
        val effectiveOrigin = request.getHeader("Origin") ?: originOf(request.getHeader("Referer"))

        if (effectiveOrigin == null || effectiveOrigin !in allowedOrigins) {
            respondForbidden(response)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun originOf(referer: String?): String? {
        if (referer == null) return null
        val uri = runCatching { URI(referer) }.getOrNull() ?: return null
        val scheme = uri.scheme ?: return null
        val host = uri.host ?: return null
        return buildString {
            append(scheme).append("://").append(host)
            if (uri.port != -1) append(":").append(uri.port)
        }
    }

    private fun respondForbidden(response: HttpServletResponse) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(
            response.writer,
            ErrorResponse(HttpStatus.FORBIDDEN.value(), "허용되지 않은 origin에서의 요청입니다.")
        )
    }

    companion object {
        private val STATE_CHANGING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
