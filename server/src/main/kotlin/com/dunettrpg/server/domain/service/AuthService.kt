package com.dunettrpg.server.domain.service

import com.dunettrpg.server.config.JwtConfig
import com.dunettrpg.server.data.repository.UserRepository
import com.dunettrpg.server.util.PasswordHasher

class AuthService(private val userRepository: UserRepository) {
    
    suspend fun login(username: String, password: String): Pair<String, String>? {
        val user = userRepository.findByUsername(username) ?: return null
        val passwordHash = userRepository.getPasswordHash(username) ?: return null
        
        if (!PasswordHasher.verify(password, passwordHash)) {
            return null
        }
        
        userRepository.updateLastActive(user.id)
        
        val token = JwtConfig.makeToken(user.id, user.username, user.role)
        return Pair(token, user.id)
    }
    
    suspend fun registerDevice(userId: String, fcmToken: String) {
        userRepository.updateFcmToken(userId, fcmToken)
    }
    
    suspend fun logout(userId: String) {
        userRepository.updateFcmToken(userId, null)
    }
}
