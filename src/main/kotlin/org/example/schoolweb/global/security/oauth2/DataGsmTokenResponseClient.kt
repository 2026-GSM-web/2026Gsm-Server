package org.example.schoolweb.global.security.oauth2

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

// DataGSM의 POST /v1/oauth/token은 RFC 6749 표준(application/x-www-form-urlencoded)이 아니라
// JSON body로 grant_type/client_id/client_secret/code/redirect_uri를 받는다
// (datagsm-server의 Oauth2TokenReqDto/Oauth2TokenResDto 참고). Spring Security 기본
// 토큰 클라이언트는 무조건 form-urlencoded + Basic 인증으로 보내므로 직접 구현한다.
@Component
class DataGsmTokenResponseClient : OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private val restClient = RestClient.create()

    override fun getTokenResponse(grantRequest: OAuth2AuthorizationCodeGrantRequest): OAuth2AccessTokenResponse {
        val clientRegistration = grantRequest.clientRegistration
        val exchange = grantRequest.authorizationExchange

        val requestBody = mapOf(
            "grant_type" to "authorization_code",
            "client_id" to clientRegistration.clientId,
            "client_secret" to clientRegistration.clientSecret,
            "code" to exchange.authorizationResponse.code,
            "redirect_uri" to exchange.authorizationRequest.redirectUri
        )

        val responseBody = try {
            restClient.post()
                .uri(clientRegistration.providerDetails.tokenUri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any?>>() {})
                ?: emptyMap()
        } catch (ex: RestClientResponseException) {
            throw OAuth2AuthorizationException(
                OAuth2Error("invalid_token_response", "DataGSM 토큰 발급 실패: ${ex.responseBodyAsString}", null),
                ex
            )
        }

        val accessToken = responseBody["access_token"]?.toString()
            ?: throw OAuth2AuthorizationException(
                OAuth2Error("invalid_token_response", "DataGSM 토큰 응답에 access_token이 없습니다.", null)
            )
        val expiresIn = (responseBody["expires_in"] as? Number)?.toLong() ?: 3600L
        val refreshToken = responseBody["refresh_token"]?.toString()
        val scopes = responseBody["scope"]?.toString()
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: clientRegistration.scopes

        val builder = OAuth2AccessTokenResponse.withToken(accessToken)
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .expiresIn(expiresIn)
            .scopes(scopes)
        if (refreshToken != null) {
            builder.refreshToken(refreshToken)
        }
        return builder.build()
    }
}
