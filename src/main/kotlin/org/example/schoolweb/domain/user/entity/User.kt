package org.example.schoolweb.domain.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // DataGSM SSO 계정 고유 식별자(/userinfo 응답의 최상위 id, Student.id 아님)를 문자열로
    // 저장한다 (AuthService 참고)
    @Column(nullable = false, unique = true, length = 100)
    val oauthProviderId: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 200)
    var email: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: Role = Role.USER,

    @CreationTimestamp
    @Column(updatable = false)
    val createdAt: LocalDateTime? = null
)
