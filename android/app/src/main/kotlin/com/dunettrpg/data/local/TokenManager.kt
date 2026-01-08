package com.dunettrpg.data.local

import android.content.SharedPreferences

class TokenManager(private val prefs: SharedPreferences) {
    
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
    
    companion object {
        private const val KEY_TOKEN = "jwt_token"
    }
}
