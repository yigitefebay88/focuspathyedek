package com.focuspath.backend.service

import com.focuspath.backend.dto.UserSyncRequest
import com.focuspath.backend.model.User
import com.focuspath.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userRepository: UserRepository) {

    @Transactional
    fun syncUser(request: UserSyncRequest): User {
        val user = userRepository.findByEmail(request.email).orElseGet {
            User(email = request.email)
        }
        
        return userRepository.save(user.copy(
            username = request.username,
            xp = request.xp,
            coins = request.coins,
            level = request.level,
            photoUrl = request.photoUrl
        ))
    }

    fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }

    fun getLeaderboard(): List<User> {
        return userRepository.findAllByOrderByXpDesc()
    }
}
