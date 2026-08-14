package org.example.schoolweb.domain.user.service

import org.example.schoolweb.global.exception.ForbiddenException
import org.example.schoolweb.global.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient
import team.themoment.datagsm.sdk.oauth.exception.DataGsmException
import team.themoment.datagsm.sdk.oauth.model.AccountObjectType

// 학교 SSO(DataGSM) 로그인 흐름 전담. Spring Security의 oauth2Login() 표준 파이프라인은 쓰지 않는다.
// 프론트가 별도 origin의 SPA이므로, 프론트가 직접 DataGSM 인증 URL로 리다이렉트시켜 authCode를
// 받아오고, 그 code(+ 프론트가 실제 사용한 redirectUri)를 이 서비스로 그대로 넘긴다.
// code->token->userinfo 교환은 GSM SSO 운영팀이 공식 배포하는 datagsm-oauth-sdk-java로 처리하므로
// 예전의 CustomOAuth2UserService/OAuth2AttributeExtractor 같은 attribute-name 매핑 인프라가
// 필요 없다 - SDK가 이미 타입 세이프한 UserInfo/Student 모델을 준다.
@Service
class AuthService(
    private val dataGsmOAuthClient: DataGsmOAuthClient,
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider
) {

    fun loginWithSchoolOAuth(authCode: String, redirectUri: String): String {
        val userInfo = try {
            val tokenResponse = dataGsmOAuthClient.exchangeCodeForToken(authCode, redirectUri)
            dataGsmOAuthClient.getUserInfo(tokenResponse.accessToken)
        } catch (ex: DataGsmException) {
            throw ForbiddenException("학교 SSO 인증에 실패했습니다: ${ex.message}")
        }

        if (userInfo.objectType != AccountObjectType.STUDENT) {
            throw ForbiddenException("학생 계정만 로그인할 수 있습니다.")
        }
        val student = userInfo.student
            ?: throw ForbiddenException("학교 SSO 응답에 학생 정보가 없습니다.")

        val user = userService.findOrCreateFromOAuth2(
            oauthProviderId = student.id.toString(),
            name = student.name,
            email = student.email ?: userInfo.email
        )
        return jwtTokenProvider.generateToken(user)
    }
}
