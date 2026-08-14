package org.example.schoolweb.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        val bearerScheme = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("schoolweb API")
                    .description(
                        "학생회 홈페이지 백엔드 API. 로그인은 학교 SSO(OAuth2, `/oauth2/authorization/school`)로 진행하고, " +
                            "성공 시 프론트엔드 리다이렉트 URL의 `#token=...`으로 JWT가 전달된다. " +
                            "이 JWT를 아래 Authorize 버튼에 `Bearer <token>` 형식으로 입력하면 인증이 필요한 API를 직접 호출해볼 수 있다."
                    )
                    .version("v1")
            )
            .addSecurityItem(SecurityRequirement().addList(bearerScheme))
            .components(
                Components().addSecuritySchemes(
                    bearerScheme,
                    SecurityScheme()
                        .name(bearerScheme)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
    }
}
