package org.example.schoolweb.domain.user.service

import org.example.schoolweb.domain.user.entity.Role
import org.example.schoolweb.domain.user.entity.User
import org.example.schoolweb.domain.user.repository.UserRepository
import org.example.schoolweb.global.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userRepository: UserRepository) {

    @Transactional
    fun findOrCreateFromOAuth2(oauthProviderId: String, name: String, email: String?): User {
        userRepository.findByOauthProviderId(oauthProviderId)?.let { return it }
        return userRepository.save(
            User(
                oauthProviderId = oauthProviderId,
                name = name,
                email = email
            )
        )
    }

    @Transactional
    fun promoteToAdmin(userId: Long): User {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("사용자를 찾을 수 없습니다. id=$userId") }
        user.role = Role.ADMIN
        return userRepository.save(user)
    }

    fun getById(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { NotFoundException("사용자를 찾을 수 없습니다. id=$userId") }
}
