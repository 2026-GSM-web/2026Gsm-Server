package org.example.schoolweb.domain.user.service

import org.example.schoolweb.global.config.SchoolOAuthProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

// DG OAuth CSRF 방지용 state를 Redis에 짧은 TTL로 저장한다. consume()은 조회와 삭제를 한 번의
// Redis 명령(GETDEL)으로 처리하므로, 같은 state로 콜백을 동시에 두 번 보내는 재사용/레이스
// 상황에서도 정확히 한쪽만 성공한다.
@Component
class OAuthStateStore(
    private val redisTemplate: StringRedisTemplate,
    private val schoolOAuthProperties: SchoolOAuthProperties
) {

    private fun key(state: String) = "oauth:state:$state"

    fun issue(): String {
        val state = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            key(state),
            "1",
            Duration.ofSeconds(schoolOAuthProperties.stateTtlSeconds)
        )
        return state
    }

    fun consume(state: String): Boolean =
        redisTemplate.opsForValue().getAndDelete(key(state)) != null
}
