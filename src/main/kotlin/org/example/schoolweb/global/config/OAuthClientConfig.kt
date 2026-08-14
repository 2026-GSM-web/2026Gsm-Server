package org.example.schoolweb.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient

@Configuration
class OAuthClientConfig(private val schoolOAuthProperties: SchoolOAuthProperties) {

    // DataGsmOAuthClient는 내부적으로 OkHttp 클라이언트를 들고 있어 요청마다 새로 만들지 않고
    // 싱글턴 빈으로 재사용한다. AutoCloseable을 구현하므로 컨텍스트 종료 시 Spring이 close()를
    // 자동으로 호출해 커넥션을 정리한다.
    @Bean
    fun dataGsmOAuthClient(): DataGsmOAuthClient =
        DataGsmOAuthClient.builder(schoolOAuthProperties.clientId, schoolOAuthProperties.clientSecret).build()
}
