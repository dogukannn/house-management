package com.dunettrpg.server.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object FirebaseConfig {
    fun init() {
        val credentialsPath = System.getenv("FIREBASE_CREDENTIALS_PATH") 
            ?: "/etc/dune/firebase-credentials.json"
        
        try {
            val serviceAccount = FileInputStream(credentialsPath)
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()
            
            FirebaseApp.initializeApp(options)
        } catch (e: Exception) {
            println("Warning: Firebase initialization failed. Push notifications will not work.")
            println("Error: ${e.message}")
        }
    }
}
