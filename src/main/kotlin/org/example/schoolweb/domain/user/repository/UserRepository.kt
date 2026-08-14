package org.example.schoolweb.domain.user.repository

import org.example.schoolweb.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByOauthProviderId(oauthProviderId: String): User?
}
