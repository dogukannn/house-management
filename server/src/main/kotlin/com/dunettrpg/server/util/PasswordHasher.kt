package com.dunettrpg.server.util

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    private const val BCRYPT_ROUNDS = 12
    
    fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS))
    
    fun verify(password: String, hash: String): Boolean = BCrypt.checkpw(password, hash)
}
