package org.example.schoolweb.global.security.oauth2

import org.example.schoolweb.domain.user.service.UserService
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

// TODO: 학교 SSO가 OIDC(id_token 발급)로 확인되면 이 클래스 대신 OidcUserService를 상속한
// 동등한 서비스를 만들어 SecurityConfig의 oauth2Login { userInfoEndpoint { oidcUserService = ... } }로 교체.
// attribute 추출 로직(OAuth2AttributeExtractor)은 그대로 재사용 가능.
@Service
class CustomOAuth2UserService(
    private val attributeExtractor: OAuth2AttributeExtractor,
    private val userService: UserService
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val attributes = oAuth2User.attributes

        val user = userService.findOrCreateFromOAuth2(
            attributeExtractor.extractProviderId(attributes),
            attributeExtractor.extractName(attributes),
            attributeExtractor.extractEmail(attributes)
        )

        val nameAttributeKey = userRequest.clientRegistration.providerDetails
            .userInfoEndpoint.userNameAttributeName
            .takeIf { !it.isNullOrBlank() } ?: "sub"

        val mergedAttributes: Map<String, Any> = attributes + mapOf("internalUserId" to user.id!!)

        return DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_${user.role}")),
            mergedAttributes,
            nameAttributeKey
        )
    }
}
