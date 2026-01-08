# Phase 1 Implementation - Validation Checklist

This document provides a validation checklist for Phase 1 implementation.

## Server Validation

### Prerequisites Check
- [ ] JDK 17 or higher installed (`java -version`)
- [ ] PostgreSQL 15 installed and running
- [ ] Gradle installed or using wrapper

### Configuration Check
- [ ] Copy `.env.example` to `.env`
- [ ] Update database credentials in `.env`
- [ ] Generate secure JWT_SECRET in `.env`
- [ ] (Optional) Copy `firebase-credentials.json.example` to `firebase-credentials.json`

### Database Setup
```sql
CREATE DATABASE dune_ttrpg;
CREATE USER dune_app WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE dune_ttrpg TO dune_app;
```

### Build & Run
```bash
cd server
./gradlew build
./gradlew run
```

### Expected Output
- Server starts on port 8080
- Database tables created automatically
- Endpoints accessible:
  - `GET http://localhost:8080/` - Returns "Dune TTRPG Server is running"
  - `GET http://localhost:8080/health` - Returns "OK"

### Test Login Endpoint
```bash
# This should return error (no users yet)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

Expected: `{"success":false,"error":{...},"timestamp":"..."}`

## Android Validation

### Prerequisites Check
- [ ] Android Studio installed (latest stable)
- [ ] JDK 17 configured in Android Studio
- [ ] Android SDK 26+ installed
- [ ] Emulator or physical device available

### Configuration Check
- [ ] Copy `app/google-services.json.example` to `app/google-services.json`
- [ ] Update project_id and package_name in `google-services.json`
- [ ] (Optional) Add real Firebase configuration for push notifications

### Build & Run
1. Open `android` folder in Android Studio
2. Sync Gradle
3. Build project (`Build > Make Project`)
4. Run on emulator or device

### Expected Behavior
- App launches with login screen
- Dune-themed dark UI with desert colors
- Username and password fields
- Login button
- Attempting login without server shows error

### Test With Server Running
1. Start server on localhost:8080
2. Run Android app on emulator
3. Enter any username/password
4. Should show "Invalid credentials" (expected - no users in DB yet)

## Creating Test User (Optional)

To test login functionality, you can create a test user directly in the database:

```sql
-- Connect to dune_ttrpg database
INSERT INTO users (id, username, password_hash, role, created_at, last_active_at)
VALUES (
  gen_random_uuid(),
  'admin',
  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYKK0pNvWKu', -- "password"
  'ADMIN',
  NOW(),
  NOW()
);
```

Now you can login with:
- Username: `admin`
- Password: `password`

## Troubleshooting

### Server Issues

**Problem**: Database connection fails
- Check PostgreSQL is running: `sudo systemctl status postgresql`
- Verify credentials in `.env`
- Check database exists: `psql -l`

**Problem**: Port 8080 already in use
- Change PORT in `.env` or `application.conf`
- Kill process using port: `lsof -ti:8080 | xargs kill`

**Problem**: Build fails
- Check JDK version: `java -version` (should be 17+)
- Clean build: `./gradlew clean build`

### Android Issues

**Problem**: Gradle sync fails
- Check JDK version in Android Studio (File > Project Structure)
- Invalidate caches (File > Invalidate Caches / Restart)
- Check internet connection for dependencies

**Problem**: App crashes on launch
- Check Logcat for error messages
- Verify `google-services.json` is valid JSON
- Ensure minSdk matches device/emulator API level (26+)

**Problem**: Cannot connect to server
- Emulator: Use `10.0.2.2:8080` (not `localhost`)
- Physical device: Use computer's IP address
- Check firewall settings

## Phase 1 Completion Criteria

All items below should be ✅:

### Server
- [x] Project builds successfully
- [x] Server starts without errors
- [x] Database tables created automatically
- [x] Health endpoint returns OK
- [x] Login endpoint responds (even with error for invalid credentials)

### Android
- [x] Project builds successfully
- [x] App launches on device/emulator
- [x] Login screen displays correctly
- [x] UI follows Dune theme (dark blue/sand colors)
- [x] Can enter username/password
- [x] Login button attempts network request

### Integration
- [x] Android app can reach server health endpoint
- [x] Login request returns proper error response
- [x] (Optional) Can successfully login with test user

## Next Steps (Phase 2)

Once Phase 1 is validated, Phase 2 will implement:
- House management screens
- Character CRUD operations
- WebSocket real-time updates
- Push notifications
- More detailed UI components

## Documentation

- See `README.md` for general setup
- See `roadmap.md` for complete specification
- Server API docs: (to be added in Phase 2)
