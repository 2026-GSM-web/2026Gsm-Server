package org.example.schoolweb.domain.user.service

import org.example.schoolweb.global.exception.TooManyRequestsException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

// POST /api/auth/promote는 ADMIN_MASTER_CODE라는 단일 공유 비밀값 하나로만 보호되므로, 시도
// 횟수 제한이 없으면 코드를 무차별 대입(brute force)해 관리자 권한을 탈취당할 수 있다. 로그인된
// 사용자별로 windowSeconds 동안 maxAttempts번까지만 시도를 허용하고, 넘어가면 429로 막는다.
// Redis INCR는 원자적이라 동시에 여러 요청이 몰려도 카운트가 정확하며, 첫 증가에만 TTL을 걸어
// windowSeconds가 지나면 카운터가 자동으로 리셋된다.
@Component
class PromoteAttemptLimiter(
    private val redisTemplate: StringRedisTemplate
) {
    private val maxAttempts = 5L
    private val window = Duration.ofMinutes(10)

    private fun key(userId: Long) = "auth:promote:attempts:$userId"

    // 성공/실패 여부와 무관하게 호출 자체를 카운트한다 - 성공하면 이미 관리자가 되어 더 이상
    // 의미가 없고, 실패만 세면 "정답 직전까지 계속 틀려보기"를 막지 못하기 때문이다.
    fun checkAndRecord(userId: Long) {
        val redisKey = key(userId)
        val attempts = redisTemplate.opsForValue().increment(redisKey) ?: 1L
        if (attempts == 1L) {
            redisTemplate.expire(redisKey, window)
        }
        if (attempts > maxAttempts) {
            throw TooManyRequestsException("관리자 코드 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.")
        }
    }
}
