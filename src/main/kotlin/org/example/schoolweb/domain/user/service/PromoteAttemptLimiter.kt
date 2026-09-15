package org.example.schoolweb.domain.user.service

import org.example.schoolweb.global.exception.TooManyRequestsException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

// POST /api/auth/promote는 ADMIN_MASTER_CODE라는 단일 공유 비밀값 하나로만 보호되므로, 시도
// 횟수 제한이 없으면 코드를 무차별 대입(brute force)해 관리자 권한을 탈취당할 수 있다. 로그인된
// 사용자별로 window 동안 maxAttempts번까지만 시도를 허용하고, 그 다음 호출부터는 TTL 없이
// 영구적으로 잠근다 - 시간이 지나도 자동으로 풀리지 않으므로, 다시 시도하게 하려면 Redis에서
// 잠금 키(auth:promote:locked:*)를 직접 지워야 한다. Redis INCR는 원자적이라 동시에 여러
// 요청이 몰려도 카운트가 정확하다.
@Component
class PromoteAttemptLimiter(
    private val redisTemplate: StringRedisTemplate
) {
    private val maxAttempts = 5L
    private val window = Duration.ofMinutes(10)

    private fun attemptsKey(userId: Long) = "auth:promote:attempts:$userId"
    private fun lockedKey(userId: Long) = "auth:promote:locked:$userId"

    private fun lockedMessage() =
        "관리자 코드를 ${maxAttempts}회 잘못 입력하여 더 이상 시도할 수 없습니다. 학생회 서버 관리자에게 문의해 주세요."

    // 성공/실패 여부와 무관하게 호출 자체를 카운트한다 - 성공하면 이미 관리자가 되어 더 이상
    // 의미가 없고, 실패만 세면 "정답 직전까지 계속 틀려보기"를 막지 못하기 때문이다.
    fun checkAndRecord(userId: Long) {
        if (redisTemplate.hasKey(lockedKey(userId)) == true) {
            throw TooManyRequestsException(lockedMessage())
        }

        val redisKey = attemptsKey(userId)
        val attempts = redisTemplate.opsForValue().increment(redisKey) ?: 1L
        if (attempts == 1L) {
            redisTemplate.expire(redisKey, window)
        }
        if (attempts > maxAttempts) {
            // 영구 잠금 - TTL을 걸지 않아 시간이 지나도 자동 해제되지 않는다
            redisTemplate.opsForValue().set(lockedKey(userId), "true")
            throw TooManyRequestsException(lockedMessage())
        }
    }
}
