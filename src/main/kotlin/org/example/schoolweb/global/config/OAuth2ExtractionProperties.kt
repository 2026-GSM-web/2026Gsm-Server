package org.example.schoolweb.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

// DataGSM(https://docs.datagsm.kr) userinfo 응답 스키마(AccountInfoResDto) 기준 기본값.
// id/email은 최상위 필드, name은 student.name처럼 중첩되어 있어 "." 경로로 표현한다.
// teacher 계정을 지원해야 하면 name-attribute를 teacher.name으로 바꿔야 한다.
@ConfigurationProperties(prefix = "app.oauth2.school")
data class OAuth2ExtractionProperties(
    val idAttribute: String = "id",
    val nameAttribute: String = "student.name",
    val emailAttribute: String = "email"
)
