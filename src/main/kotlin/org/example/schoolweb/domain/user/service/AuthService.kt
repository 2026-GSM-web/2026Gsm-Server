package org.example.schoolweb.domain.user.service

import org.example.schoolweb.global.config.SchoolOAuthProperties
import org.example.schoolweb.global.exception.ForbiddenException
import org.example.schoolweb.global.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient
import team.themoment.datagsm.sdk.oauth.exception.DataGsmException
import team.themoment.datagsm.sdk.oauth.model.AccountObjectType

// 학교 SSO(DataGSM) 로그인 흐름 전담. Spring Security의 oauth2Login() 표준 파이프라인은 쓰지 않고
// 대신 OAuthController가 /authorize -> DG 동의 -> /callback 리다이렉트 체인을 직접 소유한다
// (backend-owned redirect). code->token->userinfo 교환은 GSM SSO 운영팀이 공식 배포하는
// datagsm-oauth-sdk-java로 처리하므로 예전의 CustomOAuth2UserService/OAuth2AttributeExtractor
// 같은 attribute-name 매핑 인프라가 필요 없다 - SDK가 이미 타입 세이프한 UserInfo/Student
// 모델을 준다. CSRF 방지용 state는 OAuthStateStore(Redis)에 짧은 TTL로 저장한다.
@Service
class AuthService(
    private val dataGsmOAuthClient: DataGsmOAuthClient,
    private val schoolOAuthProperties: SchoolOAuthProperties,
    private val oAuthStateStore: OAuthStateStore,
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider
) {

    fun buildAuthorizationUrl(): String {
        val state = oAuthStateStore.issue()
        return dataGsmOAuthClient.createAuthorizationUrl(schoolOAuthProperties.redirectUri)
            .state(state)
            .build()
    }

    fun completeSchoolOAuthLogin(code: String, state: String): String {
        if (!oAuthStateStore.consume(state)) {
            throw ForbiddenException("state가 유효하지 않거나 만료되었습니다.")
        }

        val userInfo = try {
            val tokenResponse = dataGsmOAuthClient.exchangeCodeForToken(code, schoolOAuthProperties.redirectUri)
            dataGsmOAuthClient.getUserInfo(tokenResponse.accessToken)
        } catch (ex: DataGsmException) {
            throw ForbiddenException("학교 SSO 인증에 실패했습니다: ${ex.message}")
        }

        if (userInfo.objectType != AccountObjectType.STUDENT) {
            throw ForbiddenException("학생 계정만 로그인할 수 있습니다.")
        }
        val student = userInfo.student
            ?: throw ForbiddenException("학교 SSO 응답에 학생 정보가 없습니다.")

        // oauthProviderId는 student.id가 아니라 /userinfo 응답의 최상위 id를 쓴다 - 둘은 다른 값이다.
        val user = userService.findOrCreateFromOAuth2(
            oauthProviderId = userInfo.id.toString(),
            name = student.name,
            email = student.email ?: userInfo.email
        )
        return jwtTokenProvider.generateToken(user)
    }
}
