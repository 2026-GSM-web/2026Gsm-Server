package org.example.schoolweb.global.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.example.schoolweb.global.config.JwtProperties
import org.example.schoolweb.domain.user.entity.User
import org.springframework.stereotype.Component
import java.util.Date

// JWT에는 user id만 담고 role은 넣지 않는다 - 매 요청마다 DB에서 현재 role을 다시 조회하므로
// 관리자 마스터 코드 승격이 토큰 재발급 없이 즉시 반영된다.
@Component
class JwtTokenProvider(jwtProperties: JwtProperties) {

    private val key = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    private val expirationMs = jwtProperties.expirationMs

    fun generateToken(user: User): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)
        return Jwts.builder()
            .subject(user.id.toString())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun getUserId(token: String): Long =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
            .toLong()

    fun validateToken(token: String): Boolean =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            true
        } catch (ex: JwtException) {
            false
        } catch (ex: IllegalArgumentException) {
            false
        }
}
