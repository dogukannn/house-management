# Dune TTRPG Companion App

A private Android application for managing a Dune tabletop RPG campaign with real-time house management, economy simulation, and asynchronous voting mechanics.

## Project Structure

This project consists of two main components:

### 1. Android Client (`/android`)
- **Technology**: Kotlin, Jetpack Compose, MVVM Architecture
- **Min SDK**: API 26 (Android 8.0)
- **Features**: Login, Dashboard, House Management UI

### 2. Backend Server (`/server`)
- **Technology**: Kotlin, Ktor Framework
- **Database**: PostgreSQL with Exposed ORM
- **Features**: REST API, JWT Authentication, WebSocket support

## Phase 1 Implementation Status ✅

Phase 1 (Foundation) has been completed with the following features:

- ✅ Android project structure setup
- ✅ Server project structure setup
- ✅ Database schema implementation (all 7 tables)
- ✅ JWT-based authentication (login/logout)
- ✅ Basic API connectivity
- ✅ Firebase integration templates

## Setup Instructions

### Prerequisites
- JDK 17 or higher
- Android Studio (latest stable version)
- PostgreSQL 15
- Gradle 8.x

### Server Setup

1. Navigate to the server directory:
   ```bash
   cd server
   ```

2. Copy the environment configuration:
   ```bash
   cp .env.example .env
   ```

3. Edit `.env` with your database credentials and JWT secret

4. Set up PostgreSQL database:
   ```sql
   CREATE DATABASE dune_ttrpg;
   CREATE USER dune_app WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE dune_ttrpg TO dune_app;
   ```

5. Run the server:
   ```bash
   ./gradlew run
   ```

   Or build a fat JAR:
   ```bash
   ./gradlew shadowJar
   java -jar build/libs/server-all.jar
   ```

The server will start on `http://localhost:8080`

### Android Setup

1. Navigate to the android directory:
   ```bash
   cd android
   ```

2. Copy the Firebase configuration template:
   ```bash
   cp app/google-services.json.example app/google-services.json
   ```

3. Edit `app/google-services.json` with your Firebase project details

4. Open the project in Android Studio

5. Sync Gradle and build the project

6. Run on an emulator or device

**Note**: The debug build is configured to connect to `http://10.0.2.2:8080` (Android emulator localhost)

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register-device` - Register FCM token (authenticated)
- `DELETE /api/auth/logout` - Logout (authenticated)

### Health Check
- `GET /health` - Server health check

## Database Schema

The following tables are implemented:

1. **users** - User accounts (admin/player)
2. **houses** - Great Houses with economy state
3. **characters** - House characters/roster
4. **armies** - Military forces
5. **votes** - Voting system
6. **trade_deals** - Trade agreements
7. **game_events** - Event log/feed

## Technology Stack

### Android
- Kotlin 1.9.23
- Jetpack Compose (Material 3)
- Hilt (Dependency Injection)
- Retrofit + OkHttp (Networking)
- Room (Local Database)
- Firebase Cloud Messaging

### Backend
- Kotlin 1.9.23
- Ktor 2.3.8 (Server Framework)
- Exposed ORM (Database)
- PostgreSQL (Database)
- JWT Authentication
- Firebase Admin SDK

## Development Roadmap

See `roadmap.md` for the complete development specification.

### ✅ Phase 1: Foundation (COMPLETED)
- Set up project structure
- Implement database schema
- Basic authentication
- API connectivity

### 📋 Phase 2: Core Features (Next)
- House management screens
- Character CRUD operations
- Army management
- WebSocket connection
- Push notifications

### 📋 Phase 3: Game Mechanics
- Voting system
- Trade deals
- Economy simulation

### 📋 Phase 4: Admin Features
- Admin dashboard
- Game management tools

### 📋 Phase 5: Polish & Testing
- UI improvements
- Testing
- Performance optimization

## Configuration Files

### Server Configuration
- `.env` - Environment variables (copy from `.env.example`)
- `firebase-credentials.json` - Firebase Admin SDK credentials (copy from `.example`)
- `src/main/resources/application.conf` - Ktor configuration

### Android Configuration
- `app/google-services.json` - Firebase configuration (copy from `.example`)
- `app/build.gradle.kts` - Server URL configuration (debug/release)

## Security Notes

- JWT tokens expire after 1 hour
- Passwords are hashed using BCrypt (cost factor 12)
- Android uses EncryptedSharedPreferences for token storage
- HTTPS should be used in production
- Never commit `google-services.json` or `firebase-credentials.json`

## Testing

### Server Tests
```bash
cd server
./gradlew test
```

### Android Tests
```bash
cd android
./gradlew test              # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

## License

Private project - All rights reserved

## Contact

This is a private project for a Dune TTRPG campaign.
