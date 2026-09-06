package org.example.schoolweb.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

// FRONTEND_ORIGIN은 콤마로 여러 origin을 나열할 수 있다 (예: 운영 + 스테이징 도메인을 동시에
// 허용해야 할 때 "https://a.gsmsv.site,https://b.gsmsv.site"). CORS는 브라우저의 Origin 헤더와
// 완전히 문자열 일치해야 통과되므로(스킴·호스트·포트·트레일링 슬래시까지 전부), 콤마 뒤에 공백이
// 섞여도("https://a.com, https://b.com") 매칭이 깨지지 않도록 각 origin의 앞뒤 공백을 여기서
// 미리 제거한다.
@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    private val allowedOrigins: List<String> = listOf("http://localhost:3000")
) {
    val trimmedAllowedOrigins: List<String>
        get() = allowedOrigins.map { it.trim() }
}
