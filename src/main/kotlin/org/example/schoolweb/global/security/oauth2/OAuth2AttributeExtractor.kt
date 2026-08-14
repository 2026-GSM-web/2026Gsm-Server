package org.example.schoolweb.global.security.oauth2

import org.example.schoolweb.global.config.OAuth2ExtractionProperties
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.stereotype.Component

// 학교 SSO의 실제 userinfo 응답 스키마(클레임 이름)가 확정되면
// application.yml의 app.oauth2.school.* 값만 바꾸면 되도록 매핑 로직을 이 클래스 하나에 격리해둔다.
// DataGSM의 userinfo 응답은 id/email은 최상위 필드지만 이름은 student.name처럼 중첩되어 있어서,
// "." 구분 경로(dot path)로 중첩 맵까지 내려가서 찾도록 지원한다.
@Component
class OAuth2AttributeExtractor(private val props: OAuth2ExtractionProperties) {

    fun extractProviderId(attributes: Map<String, Any?>): String =
        resolveAttribute(attributes, props.idAttribute)?.toString()
            ?: attributes["sub"]?.toString()
            ?: attributes["id"]?.toString()
            ?: throw OAuth2AuthenticationException(
                OAuth2Error("invalid_user_info"),
                "학교 SSO 응답에서 사용자 식별자를 찾을 수 없습니다."
            )

    fun extractName(attributes: Map<String, Any?>): String =
        resolveAttribute(attributes, props.nameAttribute)?.toString() ?: "Unknown"

    fun extractEmail(attributes: Map<String, Any?>): String? =
        resolveAttribute(attributes, props.emailAttribute)?.toString()

    private fun resolveAttribute(attributes: Map<String, Any?>, dotPath: String): Any? {
        var current: Any? = attributes
        for (key in dotPath.split(".")) {
            current = (current as? Map<*, *>)?.get(key)
        }
        return current
    }
}
