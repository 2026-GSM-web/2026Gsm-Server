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
                        "학생회 홈페이지 백엔드 API. 로그인은 백엔드가 소유한 `GET /api/auth/dg/authorize`로 시작한다 " +
                            "(Spring Security의 기본 oauth2Login 경로인 `/oauth2/authorization/school`이 아님 - " +
                            "이 프로젝트는 oauth2Login()을 쓰지 않는다). 성공 시 JWT는 URL이 아니라 `ACCESS_TOKEN` " +
                            "httpOnly 쿠키로 전달되므로, 브라우저에서 로그인한 뒤 이 Swagger UI로 인증이 필요한 API를 " +
                            "호출하면 쿠키가 자동으로 실린다. 아래 Authorize 버튼(`Bearer <token>`)은 쿠키 대신 " +
                            "직접 발급받은 JWT를 헤더로 넣어 테스트하고 싶을 때만 쓰면 된다."
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
