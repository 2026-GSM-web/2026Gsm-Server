package org.example.schoolweb.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

// datagsm-oauth-sdk-java(GSM SSO 운영팀이 배포하는 공식 OAuth SDK)에 넘길 클라이언트 자격증명 및
// backend-owned redirect 흐름(OAuthController 참고)에 필요한 설정.
// SDK가 인증/유저정보 엔드포인트 기본값을 이미 GSM 실제 서버(oauth.authorization.datagsm.kr,
// oauth.resource.datagsm.kr)로 잡고 있어서 별도 base URL 설정은 필요 없다.
@ConfigurationProperties(prefix = "app.oauth2.school")
data class SchoolOAuthProperties(
    val clientId: String,
    val clientSecret: String,
    // 백엔드 자신의 콜백 URL. DG 클라이언트 콘솔에 redirect_uri로 등록된 값과 정확히 일치해야 한다.
    val redirectUri: String,
    // 로그인 완료(쿠키 세팅) 후 사용자를 보낼 프론트 URL. 값이 확정되기 전에는 빈 문자열로 두고,
    // OAuthController가 이 값이 비어 있으면 임의로 채우지 않고 그대로 실패시킨다.
    val webCallbackRedirectUrl: String = "",
    val stateTtlSeconds: Long = 300,
    // Secure 쿠키는 https에서만 브라우저가 전송하므로, 순수 http 로컬 환경에서만 false로 둔다.
    val cookieSecure: Boolean = true
)
