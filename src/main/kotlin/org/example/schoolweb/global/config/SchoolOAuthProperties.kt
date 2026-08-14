package org.example.schoolweb.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

// datagsm-oauth-sdk-java(GSM SSO 운영팀이 배포하는 공식 OAuth SDK)에 넘길 클라이언트 자격증명.
// SDK가 인증/유저정보 엔드포인트 기본값을 이미 GSM 실제 서버(oauth.authorization.datagsm.kr,
// oauth.resource.datagsm.kr)로 잡고 있어서 별도 base URL 설정은 필요 없다.
@ConfigurationProperties(prefix = "app.oauth2.school")
data class SchoolOAuthProperties(
    val clientId: String,
    val clientSecret: String
)
