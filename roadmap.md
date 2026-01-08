# Dune TTRPG Companion App - Software Design & Development Specification

## 1. Project Overview

### 1.1 Purpose
A private Android application for managing a Dune tabletop RPG campaign with real-time house management, economy simulation, and asynchronous voting mechanics. 

### 1.2 User Types
- **Admin Client (1 user)**: Game Master role - controls game state, initiates events, manages all houses
- **Player Client (up to 5 users)**: Players - manage their house, participate in votes, view economy

### 1.3 Core Features
- Push notifications for game events
- Real-time house economy simulation
- Asynchronous voting system with consensus requirements
- Character roster management
- Army and resource tracking
- Trade deal negotiations

---

## 2. System Architecture

### 2.1 High-Level Architecture Pattern
```
┌─────────────────────────────────────────────────────────────────┐
│                        ANDROID CLIENTS                          │
│  ┌─────────────────┐              ┌─────────────────────────┐   │
│  │   Admin App     │              │     Player App          │   │
│  │   (Single)      │              │     (Up to 5)           │   │
│  └────────┬────────┘              └───────────┬─────────────┘   │
└───────────┼────────────────────────────────────┼────────────────┘
            │                                    │
            │         HTTPS + WebSocket          │
            │                                    │
┌───────────▼────────────────────────────────────▼────────────────┐
│                         BACKEND SERVER                          │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │  REST API    │  │  WebSocket   │  │  Background Workers   │  │
│  │  Controller  │  │  Handler     │  │  (Economy Simulation) │  │
│  └──────┬───────┘  └──────┬───────┘  └───────────┬───────────┘  │
│         │                 │                      │               │
│         └─────────────────┼──────────────────────┘               │
│                           │                                      │
│                  ┌────────▼────────┐                            │
│                  │  Game State     │                            │
│                  │  Manager        │                            │
│                  └────────┬────────┘                            │
│                           │                                      │
│         ┌─────────────────┼─────────────────┐                   │
│         │                 │                 │                    │
│  ┌──────▼──────┐  ┌───────▼───────┐  ┌──────▼──────┐           │
│  │  Database   │  │  FCM Service  │  │  Event      │           │
│  │  (SQLite/   │  │  (Push Notif) │  │  Queue      │           │
│  │  PostgreSQL)│  └───────────────┘  └─────────────┘           │
│  └─────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Technology Stack Specification

#### Android Client
| Component | Technology | Justification |
|-----------|------------|---------------|
| Language | Kotlin | Modern Android standard, null safety |
| Min SDK | API 26 (Android 8.0) | Supports 95%+ devices, modern APIs |
| UI Framework | Jetpack Compose | Declarative UI, less boilerplate |
| Architecture | MVVM + Clean Architecture | Separation of concerns, testability |
| DI Framework | Hilt | Official Android DI solution |
| Networking | Retrofit + OkHttp | Industry standard, interceptor support |
| WebSocket | OkHttp WebSocket | Same client, consistent connection handling |
| Local Storage | Room Database | Type-safe SQLite abstraction |
| State Management | Kotlin Flow + StateFlow | Reactive data streams |
| Navigation | Navigation Compose | Type-safe navigation |
| Push Notifications | Firebase Cloud Messaging | Reliable, free tier sufficient |

#### Backend Server
| Component | Technology | Justification |
|-----------|------------|---------------|
| Language | Kotlin | Shared language with Android |
| Framework | Ktor | Lightweight, Kotlin-native, coroutine support |
| Database | PostgreSQL | Robust relational data, JSONB for flexibility |
| ORM | Exposed | Kotlin-native SQL DSL |
| WebSocket | Ktor WebSocket | Built-in support |
| Authentication | JWT | Stateless, simple for small user base |
| Push Service | Firebase Admin SDK | Server-side FCM integration |
| Hosting | Single VPS (DigitalOcean/Linode) | Cost-effective for 6 users |

---

## 3. Data Models & Database Schema

### 3.1 Core Entities

#### User
```
User
├── id:  UUID (primary key)
├── username: String (unique)
├── passwordHash: String
├── role:  Enum [ADMIN, PLAYER]
├── fcmToken: String (nullable, for push notifications)
├── houseId: UUID (foreign key, nullable for admin)
├── createdAt:  Timestamp
└── lastActiveAt: Timestamp
```

#### House
```
House
├���─ id: UUID (primary key)
├── name: String (e.g., "House Atreides")
├── planetaryFief: String
├── economyState: JSON
│   ├── spiceReserves:  Decimal
│   ├── solariBalance: Decimal
│   ├── incomePerCycle: Decimal
│   ├── expensesPerCycle: Decimal
│   └── tradeModifiers: Map<String, Decimal>
├── politicalStanding: Integer (-100 to 100)
├── createdAt: Timestamp
└── updatedAt: Timestamp
```

#### Character
```
Character
├── id: UUID (primary key)
├── houseId: UUID (foreign key)
├── name: String
├── role: String (e.g., "Mentat", "Swordmaster")
├── stats: JSON
│   ├── combat: Integer
│   ├── intrigue: Integer
│   ├── diplomacy: Integer
│   └── prescience: Integer
├── status:  Enum [ACTIVE, INJURED, CAPTURED, DECEASED]
├── notes: Text
└── portraitUrl: String (nullable)
```

#### Army
```
Army
├── id: UUID (primary key)
├── houseId: UUID (foreign key)
├── name: String
├── units: JSON
│   ├── infantry: Integer
│   ├── sardaukar: Integer
│   ├── fremen: Integer
│   └── ornithopters: Integer
├── location: String
├── status: Enum [STATIONED, DEPLOYED, IN_COMBAT, RETREATING]
├── maintenanceCost: Decimal
└── commanderId: UUID (foreign key to Character, nullable)
```

#### Vote
```
Vote
├── id: UUID (primary key)
├── type: Enum [TRADE_DEAL, ALLIANCE, WAR_DECLARATION, LANDSRAAD_MOTION, CUSTOM]
├── title: String
├── description: Text
├── initiatorHouseId: UUID (foreign key)
├── requiredParticipants: List<UUID> (house IDs)
├── votes: JSON
│   └── Map<houseId, {decision:  Enum, timestamp: Timestamp}>
├── consensusRequired: Boolean
├── deadline: Timestamp (nullable)
├── status: Enum [PENDING, PASSED, FAILED, EXPIRED, CANCELLED]
├── result: JSON (nullable, outcome details)
├── createdAt: Timestamp
└── resolvedAt: Timestamp (nullable)
```

#### TradeDeal
```
TradeDeal
├── id: UUID (primary key)
├── voteId: UUID (foreign key, nullable)
├── fromHouseId: UUID (foreign key)
├── toHouseId:  UUID (foreign key)
├── offering: JSON
│   ├── spice: Decimal
│   ├── solaris: Decimal
│   └── other: List<String>
├── requesting: JSON
│   ├── spice: Decimal
│   ├── solaris: Decimal
│   └── other: List<String>
├── duration: Integer (cycles, nullable for one-time)
├── status: Enum [PROPOSED, ACTIVE, COMPLETED, CANCELLED, REJECTED]
├── createdAt:  Timestamp
└── expiresAt: Timestamp (nullable)
```

#### GameEvent
```
GameEvent
├── id: UUID (primary key)
├── type: Enum [ECONOMY_UPDATE, VOTE_STARTED, VOTE_RESOLVED, TRADE_PROPOSED, 
│              ATTACK_DECLARED, CHARACTER_STATUS_CHANGE, ADMIN_ANNOUNCEMENT, CUSTOM]
├── targetHouseIds: List<UUID> (empty = all houses)
├── payload: JSON
├── requiresNotification: Boolean
├── notificationSent: Boolean
├── createdAt: Timestamp
└── createdBy: UUID (user ID)
```

### 3.2 Database Indexes
- `User.username` - Unique index for login lookup
- `User.fcmToken` - Index for notification targeting
- `Character.houseId` - Index for house roster queries
- `Army.houseId` - Index for house military queries
- `Vote.status` - Index for active vote queries
- `GameEvent.createdAt` - Index for event timeline queries
- `GameEvent.targetHouseIds` - GIN index for array containment queries

---

## 4. API Specification

### 4.1 REST Endpoints

#### Authentication
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/login` | Authenticate user, return JWT | No |
| POST | `/api/auth/refresh` | Refresh JWT token | Yes |
| POST | `/api/auth/register-device` | Register FCM token | Yes |
| DELETE | `/api/auth/logout` | Invalidate token, clear FCM | Yes |

#### Houses
| Method | Endpoint | Description | Auth Required | Role |
|--------|----------|-------------|---------------|------|
| GET | `/api/houses` | List all houses (summary) | Yes | Any |
| GET | `/api/houses/{id}` | Get house details | Yes | Owner/Admin |
| PUT | `/api/houses/{id}` | Update house | Yes | Admin |
| GET | `/api/houses/{id}/economy` | Get detailed economy state | Yes | Owner/Admin |
| POST | `/api/houses/{id}/economy/adjust` | Manual economy adjustment | Yes | Admin |

#### Characters
| Method | Endpoint | Description | Auth Required | Role |
|--------|----------|-------------|---------------|------|
| GET | `/api/houses/{houseId}/characters` | List house characters | Yes | Owner/Admin |
| POST | `/api/houses/{houseId}/characters` | Create character | Yes | Admin |
| GET | `/api/characters/{id}` | Get character details | Yes | Owner/Admin |
| PUT | `/api/characters/{id}` | Update character | Yes | Admin |
| DELETE | `/api/characters/{id}` | Remove character | Yes | Admin |

#### Armies
| Method | Endpoint | Description | Auth Required | Role |
|--------|----------|-------------|---------------|------|
| GET | `/api/houses/{houseId}/armies` | List house armies | Yes | Owner/Admin |
| POST | `/api/houses/{houseId}/armies` | Create army | Yes | Admin |
| PUT | `/api/armies/{id}` | Update army | Yes | Admin |
| POST | `/api/armies/{id}/deploy` | Change army deployment | Yes | Owner/Admin |

#### Votes
| Method | Endpoint | Description | Auth Required | Role |
|--------|----------|-------------|---------------|------|
| GET | `/api/votes` | List votes (filtered by status) | Yes | Any |
| GET | `/api/votes/pending` | Get pending votes for user | Yes | Any |
| POST | `/api/votes` | Create new vote | Yes | Admin/Owner |
| GET | `/api/votes/{id}` | Get vote details | Yes | Participant/Admin |
| POST | `/api/votes/{id}/cast` | Cast vote | Yes | Participant |
| POST | `/api/votes/{id}/cancel` | Cancel vote | Yes | Admin/Initiator |

#### Trade Deals
| Method | Endpoint | Description | Auth Required | Role |
|--------|----------|-------------|---------------|------|
| GET | `/api/trades` | List all trades | Yes | Any |
| POST | `/api/trades/propose` | Propose new trade | Yes | Owner |
| GET | `/api/trades/{id}` | Get trade details | Yes | Party/Admin |
| POST | `/api/trades/{id}/counter` | Counter-propose | Yes | Target Owner |
| POST | `/api/trades/{id}/cancel` | Cancel trade | Yes | Proposer/Admin |

#### Game Events
| Method | Endpoint | Description | Auth Required | Role |
|--------|----------|-------------|---------------|------|
| GET | `/api/events` | Get event feed (paginated) | Yes | Any |
| POST | `/api/events/announce` | Create announcement | Yes | Admin |
| GET | `/api/events/unread` | Get unread events count | Yes | Any |
| POST | `/api/events/mark-read` | Mark events as read | Yes | Any |

#### Admin
| Method | Endpoint | Description | Auth Required | Role |
|--------|----------|-------------|---------------|------|
| GET | `/api/admin/game-state` | Get full game state snapshot | Yes | Admin |
| POST | `/api/admin/economy/cycle` | Trigger economy cycle manually | Yes | Admin |
| POST | `/api/admin/reset` | Reset game state | Yes | Admin |
| GET | `/api/admin/users` | List all users | Yes | Admin |
| POST | `/api/admin/users` | Create user account | Yes | Admin |

### 4.2 WebSocket Events

#### Connection
- Endpoint: `wss://server/ws/game`
- Authentication: JWT token as query parameter or first message

#### Server → Client Events
```
ECONOMY_UPDATE
├── houseId: UUID
├── previousState: EconomyState
├── newState: EconomyState
└── changeReason: String

VOTE_CREATED
├── voteId: UUID
├── type: String
├── title: String
├── initiator: String
└── deadline: Timestamp? 

VOTE_CAST
├── voteId: UUID
├── houseId: UUID
├── decision: String (without revealing to others if secret ballot)
└── remainingVoters: Integer

VOTE_RESOLVED
├── voteId: UUID
├── result: String
├── details: JSON
└── effects: List<String>

TRADE_PROPOSED
├── tradeId:  UUID
├── fromHouse: String
├── toHouse: String
└── summary: String

TRADE_STATUS_CHANGED
├── tradeId: UUID
├── newStatus: String
└── effects: JSON? 

GAME_ANNOUNCEMENT
├── title: String
├── message: String
├── priority:  Enum [LOW, NORMAL, HIGH, URGENT]
└── fromAdmin: Boolean

SYNC_REQUIRED
├── reason: String
└── lastKnownVersion: Long
```

#### Client → Server Events
```
PING
└── timestamp: Long

SUBSCRIBE_HOUSE
└── houseId: UUID

TYPING_INDICATOR (for trade negotiation)
├── context: String
└── targetUserId: UUID
```

### 4.3 API Response Format
```
Success Response:
{
  "success": true,
  "data": { ...  },
  "timestamp": "2026-01-08T12:00:00Z"
}

Error Response:
{
  "success": false,
  "error": {
    "code": "VOTE_ALREADY_CAST",
    "message": "You have already cast your vote on this matter",
    "details": { ... }
  },
  "timestamp": "2026-01-08T12:00:00Z"
}
```

---

## 5. Module Breakdown

### 5.1 Android Client Modules

```
app/
├── build.gradle.kts (app module configuration)
├── src/main/
│   ├── AndroidManifest.xml
│   └── kotlin/com/dunettrpg/
│       ├── DuneApplication.kt (Application class, Hilt setup)
│       ├── MainActivity.kt (Single activity, Compose host)
│       │
│       ├── di/ (Dependency Injection)
│       │   ├── AppModule.kt (Application-scoped dependencies)
│       │   ├── NetworkModule.kt (Retrofit, OkHttp, WebSocket)
│       │   ├── DatabaseModule.kt (Room database)
│       │   └── RepositoryModule.kt (Repository bindings)
│       │
│       ├── data/
│       │   ├── local/
│       │   │   ├── AppDatabase.kt (Room database definition)
│       │   │   ├── dao/ (Data Access Objects)
│       │   │   │   ├── HouseDao.kt
│       │   │   │   ├── CharacterDao.kt
│       │   │   │   ├── VoteDao.kt
│       │   │   │   └── EventDao.kt
│       │   │   └── entity/ (Room entities, mirrors server models)
│       │   │
│       │   ├── remote/
│       │   │   ├── api/
│       │   │   │   ├── AuthApi.kt (Retrofit interface)
│       │   │   │   ├── HouseApi.kt
│       │   │   │   ├── VoteApi.kt
│       │   │   │   ├── TradeApi.kt
│       │   │   │   └── AdminApi.kt
│       │   │   ├── websocket/
│       │   │   │   ├── GameWebSocket.kt (Connection manager)
│       │   │   │   ├── WebSocketEvent.kt (Sealed class for events)
│       │   │   │   └── WebSocketEventParser.kt
│       │   │   └── dto/ (Data Transfer Objects)
│       │   │       ├── request/ (Request bodies)
│       │   │       └── response/ (Response bodies)
│       │   │
│       │   └── repository/
│       │       ├── AuthRepository.kt
│       │       ├── HouseRepository.kt
│       │       ├── CharacterRepository.kt
│       │       ├── VoteRepository. kt
│       │       ├── TradeRepository.kt
│       │       └── EventRepository.kt
│       │
│       ├── domain/
│       │   ├── model/ (Domain models, UI-friendly)
│       │   │   ├── User.kt
│       │   │   ├── House.kt
│       │   │   ├── Character.kt
│       │   │   ├── Army. kt
│       │   │   ├── Vote.kt
│       │   │   ├── TradeDeal.kt
│       │   │   └── GameEvent.kt
│       │   │
│       │   └── usecase/
│       │       ├── auth/
│       │       │   ├── LoginUseCase.kt
│       │       │   └── LogoutUseCase.kt
│       │       ├── house/
│       │       │   ├── GetHouseDetailsUseCase.kt
│       │       │   └── ObserveEconomyUseCase.kt
│       │       ├── vote/
│       │       │   ├── GetPendingVotesUseCase.kt
│       │       │   ├── CastVoteUseCase.kt
│       │       │   └── CreateVoteUseCase.kt
│       │       └── trade/
│       │           ├── ProposeTradeUseCase.kt
│       │           └── GetActiveTradesUseCase.kt
│       │
│       ├── ui/
│       │   ├── theme/
│       │   │   ├── Theme.kt (Dune-inspired dark theme)
│       │   │   ├── Color.kt
│       │   │   ├── Typography.kt
│       │   │   └── Shape.kt
│       │   │
│       │   ├── navigation/
│       │   │   ├── NavGraph.kt (Navigation definition)
│       │   │   ├── Screen.kt (Sealed class for routes)
│       │   │   └── BottomNavBar.kt
│       │   │
│       │   ├── components/ (Reusable composables)
│       │   │   ├── DuneCard.kt
│       │   │   ├── ResourceDisplay.kt
│       │   │   ├── VoteCard.kt
│       │   │   ├── CharacterAvatar.kt
│       │   │   ├── ArmyUnitCounter.kt
│       │   │   ├── LoadingIndicator.kt
│       │   │   └── ErrorDisplay.kt
│       │   │
│       │   ├── screens/
│       │   │   ├── auth/
│       │   │   │   ├── LoginScreen. kt
│       │   │   │   └── LoginViewModel.kt
│       │   │   │
│       │   │   ├── dashboard/
│       │   │   │   ├── DashboardScreen.kt (Home overview)
│       │   │   │   └── DashboardViewModel.kt
│       │   │   │
│       │   │   ├── house/
│       │   │   │   ├── HouseOverviewScreen.kt
│       │   │   │   ├── HouseOverviewViewModel. kt
│       │   │   │   ├── EconomyDetailScreen.kt
│       │   │   │   └── EconomyDetailViewModel.kt
│       │   │   │
│       │   │   ├── characters/
│       │   │   │   ├── CharacterListScreen.kt
│       │   │   │   ├── CharacterListViewModel.kt
│       │   │   │   ├── CharacterDetailScreen.kt
│       │   │   │   └── CharacterDetailViewModel.kt
│       │   │   │
│       │   │   ├── armies/
│       │   │   │   ├── ArmyListScreen.kt
│       │   │   │   ├── ArmyListViewModel.kt
│       │   │   │   └── ArmyDetailScreen.kt
│       │   │   │
│       │   │   ├── votes/
│       │   │   │   ├── VoteListScreen.kt
│       │   │   │   ├── VoteListViewModel.kt
│       │   │   │   ├── VoteDetailScreen.kt
│       │   │   │   ├── VoteDetailViewModel.kt
│       │   │   │   ├── CreateVoteScreen.kt (Admin/Initiator)
│       │   │   │   └── CreateVoteViewModel.kt
│       │   │   │
│       │   │   ├── trades/
│       │   │   │   ├── TradeListScreen. kt
│       │   │   │   ├── TradeListViewModel.kt
│       │   │   │   ├── ProposeTradeScreen.kt
│       │   │   │   └── ProposeTradeViewModel.kt
│       │   │   │
│       │   │   ├── events/
│       │   │   │   ├── EventFeedScreen.kt
│       │   │   │   └── EventFeedViewModel.kt
│       │   │   │
│       │   │   └── admin/ (Admin-only screens)
│       │   │       ├── AdminDashboardScreen.kt
│       │   │       ├── AdminDashboardViewModel. kt
│       │   │       ├── ManageHouseScreen.kt
│       │   │       ├── ManageCharacterScreen.kt
│       │   │       ├── ManageArmyScreen.kt
│       │   │       ├── TriggerEconomyCycleScreen.kt
│       │   │       └── BroadcastAnnouncementScreen.kt
│       │   │
│       │   └── state/ (UI state classes)
│       │       ├── UiState.kt (Generic sealed class)
│       │       └── SnackbarState.kt
│       │
│       ├── service/
│       │   ├── DuneFirebaseMessagingService.kt (FCM handler)
│       │   └── NotificationHelper.kt (Notification channel setup)
│       │
│       └── util/
│           ├── Constants.kt
│           ├── Extensions.kt
│           ├── DateTimeFormatter.kt
│           └── NetworkMonitor.kt
```

### 5.2 Backend Server Modules

```
server/
├── build.gradle.kts
├── src/main/
│   ├── resources/
│   │   ├── application.conf (Ktor configuration)
│   │   └── logback.xml
│   │
│   └── kotlin/com/dunettrpg/server/
│       ├── Application.kt (Entry point)
│       │
│       ├── config/
│       │   ├── DatabaseConfig.kt (PostgreSQL connection)
│       │   ├── FirebaseConfig.kt (FCM initialization)
│       │   └── JwtConfig.kt (JWT settings)
│       │
│       ├── plugins/
│       │   ├── Routing.kt (Route installation)
│       │   ├── Security.kt (JWT authentication)
│       │   ├── Serialization.kt (JSON config)
│       │   ├── WebSockets.kt (WebSocket config)
│       │   └── StatusPages.kt (Error handling)
│       │
│       ├── data/
│       │   ├── tables/ (Exposed table definitions)
│       │   │   ├── UsersTable.kt
│       │   │   ├── HousesTable.kt
│       │   │   ├── CharactersTable.kt
│       │   │   ├── ArmiesTable.kt
│       │   │   ├── VotesTable.kt
│       │   │   ├── TradeDealsTable.kt
│       │   │   └── GameEventsTable.kt
│       │   │
│       │   └── repository/
│       │       ├── UserRepository.kt
│       │       ├── HouseRepository.kt
│       │       ├── CharacterRepository.kt
│       │       ├── ArmyRepository.kt
│       │       ├── VoteRepository.kt
│       │       ├── TradeRepository.kt
│       │       └── EventRepository.kt
│       │
│       ├── domain/
│       │   ├── model/ (Shared domain models)
│       │   └── service/
│       │       ├── AuthService.kt
│       │       ├── HouseService.kt
│       │       ├── VoteService.kt
│       │       ├── TradeService. kt
│       │       ├── EconomyService.kt (Simulation logic)
│       │       └── NotificationService.kt
│       │
│       ├── routes/
│       │   ├── AuthRoutes.kt
│       │   ├── HouseRoutes.kt
│       │   ├── CharacterRoutes.kt
│       │   ├── ArmyRoutes.kt
│       │   ├── VoteRoutes.kt
│       │   ├── TradeRoutes.kt
│       │   ├── EventRoutes.kt
│       │   └── AdminRoutes.kt
│       │
│       ├── websocket/
│       │   ├── GameWebSocketHandler.kt
│       │   ├── ConnectionManager.kt (Track active connections)
│       │   ├── WebSocketEvent.kt
│       │   └── EventBroadcaster.kt
│       │
│       ├── worker/
│       │   ├── EconomyCycleWorker.kt (Scheduled economy updates)
│       │   ├── VoteDeadlineWorker.kt (Check vote expirations)
│       │   └── WorkerScheduler.kt
│       │
│       ├── dto/
│       │   ├── request/
│       │   └── response/
│       │
│       └── util/
│           ├── PasswordHasher.kt
│           └── Extensions.kt
```

---

## 6. UI Layout Specification

### 6.1 Screen Flow Diagram

```
┌─────────────┐
│   Splash    │
│   Screen    │
└──────┬──────┘
       │
       ▼
┌─────────────┐     ┌─────────────────────────────────────────────┐
│   Login     │────▶│              Main App Shell                 │
│   Screen    │     │  ┌─────────────────────────────────────┐    │
└─────────────┘     │  │         Bottom Navigation           │    │
                    │  │  [Dashboard][House][Votes][Events]  │    │
                    │  └─────────────────────────────────────┘    │
                    │                    │                        │
                    │    ┌───────────────┼───────────────┐        │
                    │    ▼               ▼               ▼        │
                    │ Dashboard      House Tab       Votes Tab    │
                    │    │               │               │        │
                    │    │          ┌────┴────┐     ┌────┴────┐   │
                    │    │          ▼         ▼     ▼         ▼   │
                    │    │       Overview  Characters  List   Detail│
                    │    │          │         │                   │
                    │    │          ▼         ▼                   │
                    │    │       Economy   Detail                 │
                    │    │       Detail                           │
                    └────┴────────────────────────────────────────┘
```

### 6.2 Screen Layouts

#### Login Screen
```
┌────────────────────────────────────┐
│                                    │
│         [DUNE LOGO/TITLE]          │
│        "House Management"          │
│                                    │
│   ┌────────────────────────────┐   │
│   │  Username                  │   │
│   └────────────────────────────┘   │
│                                    │
│   ┌────────────────────────────┐   │
│   │  Password                  │   │
│   └────────────────────────────┘   │
│                                    │
│   ┌────────────────────────────┐   │
│   │         LOGIN              │   │
│   └────────────────────────────┘   │
│                                    │
│         [Error message area]       │
│                                    │
└────────────────────────────────────┘
```

#### Dashboard Screen (Player View)
```
┌────────────────────────────────────┐
│  House Atreides          [⚙️]     │
├────────────────────────────────────┤
│  ┌──────────────────────────────┐  │
│  │  ECONOMY OVERVIEW            │  │
│  │  ────────────────────────    │  │
│  │  Spice:  ████████░░ 1,240    │  │
│  │  Solaris: ███████░░░ 45,000 │  │
│  │  Income: +2,500/cycle        │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │  ⚠️ PENDING ACTIONS (2)      │  │
│  │  ────────────────────────    │  │
│  │  • Vote:  Trade Agreement     │  │
│  │  • Vote: Landsraad Motion    │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │  RECENT EVENTS               │  │
│  │  ────────────────────────    │  │
│  │  • Economy cycle completed   │  │
│  │  • Trade with Harkonnen...    │  │
│  │  • New character added       │  │
│  └──────────────────────────────┘  │
│                                    │
├────────────────────────────────────┤
│ [🏠Home] [🏰House] [🗳️Vote] [📜Events]│
└────────────────────────────────────┘
```

#### House Overview Screen
```
┌────────────────────────────────────┐
│  ← House Overview                  │
├────────────────────────────────────┤
│  ┌──────────────────────────────┐  │
│  │  🏰 HOUSE ATREIDES           │  │
│  │  Fief:  Caladan               │  │
│  │  Standing: +45 (Favorable)   │  │
│  └──────────────────────────────┘  │
│                                    │
│  [Economy] [Characters] [Armies]   │
│  ─────────────────────────────────│
│                                    │
│  When "Economy" tab selected:      │
│  ┌──────────────────────────────┐  │
│  │  RESOURCES                   │  │
│  │  Spice Reserves: 1,240 tons  │  │
│  │  Solaris:  45,000             │  │
│  │                              │  │
│  │  INCOME/EXPENSES             │  │
│  │  Base Income:     +3,000      │  │
│  │  Trade Income:   +500        │  │
│  │  Army Upkeep:    -800        │  │
│  │  Other:           -200        │  │
│  │  ─────────────────────       │  │
│  │  Net per Cycle:   +2,500      │  │
│  └──────────────────────────────┘  │
│                                    │
├────────────────────────────────────┤
│ [🏠Home] [🏰House] [🗳️Vote] [📜Events]│
└────────────────────────────────────┘
```

#### Character List Screen
```
┌────────────────────────────────────┐
│  ← Characters (5)                  │
├────────────────────────────────────┤
│  🔍 Search characters...            │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ [👤] Duke Leto Atreides      │  │
│  │      Role: Duke              │  │
│  │      Status: ● Active        │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ [👤] Duncan Idaho            │  │
│  │      Role: Swordmaster       │  │
│  │      Status: ● Active        │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ [👤] Thufir Hawat            │  │
│  │      Role: Mentat            │  │
│  │      Status: ● Active        │  │
│  └──────────────────────────────┘  │
│                                    │
│  ...  (scrollable)                  │
│                                    │
├────────────────────────────────────┤
│ [🏠Home] [🏰House] [🗳️Vote] [📜Events]│
└────────────────────────────────────┘
```

#### Vote Detail Screen
```
┌────────────────────────────────────┐
│  ← Vote Details                    │
├────────────────────────────────────┤
│  ┌──────────────────────────────┐  │
│  │  🗳️ TRADE AGREEMENT          │  │
│  │  Proposed by: House Harkonnen│  │
│  │  Type: Trade Deal            │  │
│  │  Status: ⏳ Awaiting Votes   │  │
│  └──────────────────────────────┘  │
│                                    │
│  DESCRIPTION                       │
│  ───────────────────────────────   │
│  House Harkonnen proposes a        │
│  mutual non-aggression pact and    │
│  spice trading agreement for       │
│  the next 3 cycles.                │
│                                    │
│  VOTE STATUS                       │
│  ───────────────────────────────   │
│  ✅ Atreides - Voted               │
│  ⏳ Harkonnen - Pending            │
│  ✅ Corrino - Voted                │
│  ⏳ Ordos - Pending                │
│                                    │
│  YOUR VOTE                         │
│  ┌─────────┐  ┌─────────┐         │
│  │  APPROVE │  │  REJECT │         │
│  └─────────┘  └─────────┘         │
│                                    │
├────────────────────────────────────┤
│ [🏠Home] [🏰House] [🗳️Vote] [📜Events]│
└────────────────────────────────────┘
```

#### Admin Dashboard Screen
```
┌────────────────────────────────────┐
│  Admin Control Panel      [🔔 3]  │
├────────────────────────────────────┤
│  QUICK ACTIONS                     │
│  ┌─────────┐ ┌─────────┐          │
│  │ Trigger │ │ Send    │          │
│  │ Economy │ │ Announce│          │
│  │ Cycle   │ │ ment    │          │
│  └─────────┘ └─────────┘          │
│  ┌─────────┐ ┌─────────┐          │
│  │ Create  │ │ Manage  │          │
│  │ Vote    │ │ Users   │          │
│  └─────────┘ └─────────┘          │
│                                    │
│  HOUSE OVERVIEW                    │
│  ───────────────────────────────   │
│  ┌──────────────────────────────┐  │
│  │ Atreides  💰45,000  🏜️1,240 │  │
│  │ [Edit] [Economy] [Characters]│  │
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │ Harkonnen 💰62,000  🏜️980  │  │
│  │ [Edit] [Economy] [Characters]│  │
│  └──────────────────────────────┘  │
│  ...  (all houses)                  │
│                                    │
│  PENDING VOTES (2)                 │
│  ───────────────────────────────   │
│  • Trade Agreement - 2/4 voted     │
│  • Landsraad Motion - 1/5 voted    │
│                                    │
├────────────────────────────────────┤
│ [📊Dash] [🏰Houses] [🗳️Votes] [⚙️]  │
└────────────────────────────────────┘
```

### 6.3 Design System Specifications

#### Color Palette (Dune-Inspired)
| Name | Hex | Usage |
|------|-----|-------|
| Desert Sand | `#D4A574` | Primary accent |
| Spice Orange | `#FF8C42` | Spice-related, highlights |
| Deep Blue | `#1A365D` | Primary background |
| Midnight | `#0D1B2A` | Card backgrounds |
| Atreides Green | `#2D6A4F` | Success, Atreides house |
| Harkonnen Red | `#9B2226` | Danger, Harkonnen house |
| Corrino Gold | `#FFD700` | Imperial, Corrino house |
| Sand White | `#F5F0E8` | Primary text |
| Muted Sand | `#A0937D` | Secondary text |

#### Typography
| Element | Font | Size | Weight |
|---------|------|------|--------|
| H1 | System Default | 28sp | Bold |
| H2 | System Default | 24sp | SemiBold |
| H3 | System Default | 20sp | SemiBold |
| Body | System Default | 16sp | Regular |
| Caption | System Default | 14sp | Regular |
| Button | System Default | 16sp | Medium |

#### Spacing System
- Base unit: 8dp
- Margins: 16dp (2 units)
- Card padding: 16dp
- Item spacing: 8dp
- Section spacing: 24dp (3 units)

---

## 7. Business Logic Specification

### 7.1 Economy Simulation System

#### Economy Cycle Process
```
1.  TRIGGER (Admin manual OR scheduled every X hours)
   │
2. FOR EACH House:
   │
   ├─► Calculate Base Income
   │   - Planetary fief production
   │   - Political standing modifier
   │
   ├─► Calculate Trade Income
   │   - Sum of active trade deal incomes
   │   - Apply trade modifiers
   │
   ├─► Calculate Expenses
   │   - Army maintenance (units × cost per unit)
   │   - Character salaries (optional)
   │   - Fixed house expenses
   │
   ├─► Apply Net Change
   │   - newBalance = oldBalance + income - expenses
   │   - Clamp to minimum 0
   │
   ├─► Check Bankruptcy Conditions
   │   - If balance < 0: Trigger bankruptcy event
   ���   - Notify admin, apply penalties
   │
   └─► Generate Economy Event
       - Store previous and new state
       - Broadcast via WebSocket
       - Send push notification if significant change

3. PROCESS Active Trade Deals
   - Execute resource transfers
   - Check for deal completions
   - Update deal statuses

4. LOG Cycle Completion
   - Store cycle timestamp
   - Store snapshot of all house economies
```

#### Army Maintenance Costs
| Unit Type | Cost per Unit per Cycle |
|-----------|-------------------------|
| Infantry | 10 Solaris |
| Sardaukar | 50 Solaris |
| Fremen | 25 Solaris |
| Ornithopters | 100 Solaris |

### 7.2 Voting System Logic

#### Vote Creation Flow
```
1. Initiator creates vote
   │
2. Validate vote parameters
   │ - Required participants exist
   │ - Initiator has permission
   │ - No duplicate active vote of same type
   │
3. Store vote with PENDING status
   │
4. Determine notification targets
   │ - All required participants
   │ - Admin always notified
   │
5. Send notifications
   │ - Push notification via FCM
   │ - WebSocket event VOTE_CREATED
   │
6. Start deadline timer (if deadline set)
```

#### Vote Resolution Logic
```
WHEN vote cast OR deadline reached:
│
├─► Check if all required participants voted
│   │
│   ├─► YES: Proceed to resolution
│   │
│   └─► NO: Check deadline
│       │
│       ├─► Deadline passed: 
│       │   - Mark as EXPIRED or FAILED
│       │   - Notify all parties
│       │
│       └─► Deadline not passed: 
│           - Continue waiting
│           - Send reminder if configured

RESOLUTION LOGIC:
│
├─► Consensus Required (consensusRequired = true):
│   │
│   ├─► All APPROVE:  PASSED
│   └─► Any REJECT: FAILED
│
└─► Majority Vote (consensusRequired = false):
    │
    ├─► APPROVE > REJECT: PASSED
    ├─► REJECT > APPROVE: FAILED
    └─► TIE:  Initiator's vote breaks tie OR FAILED

AFTER RESOLUTION:
│
├─► Execute vote effects (if any)
│   - Trade deals activated
│   - Alliances formed
│   - Resources transferred
│
├─► Update vote status
│
├─► Broadcast VOTE_RESOLVED event
│
└─► Send push notifications
```

### 7.3 Trade Deal Processing

#### Trade Proposal Flow
```
1. Player proposes trade
   │
2. Validate trade
   │ - Proposer has sufficient resources
   │ - Target house exists
   │ - No conflicting active trade
   │
3. IF requires vote (multi-party OR configured):
   │ - Create associated Vote
   │ - Trade status = PENDING_VOTE
   │
4. ELSE (bilateral, no vote needed):
   │ - Notify target house
   │ - Trade status = PROPOSED
   │
5. Target house responds
   │
   ├─► ACCEPT: 
   │   - Execute trade
   │   - Transfer resources
   │   - Status = ACTIVE (recurring) or COMPLETED (one-time)
   │
   ├─► REJECT: 
   │   - Status = REJECTED
   │   - Notify proposer
   │
   └─► COUNTER: 
       - Create new trade with modified terms
       - Original trade = CANCELLED
       - New trade references original
```

---

## 8. Push Notification Specification

### 8.1 Notification Types

| Event | Title | Body Template | Priority |
|-------|-------|---------------|----------|
| VOTE_CREATED | "New Vote Required" | "{initiator} has initiated a vote:  {title}" | HIGH |
| VOTE_REMINDER | "Vote Pending" | "You haven't voted on:  {title}.  Deadline: {time}" | NORMAL |
| VOTE_RESOLVED | "Vote Concluded" | "Vote '{title}' has {result}" | NORMAL |
| TRADE_PROPOSED | "Trade Offer" | "{house} has proposed a trade deal" | NORMAL |
| TRADE_ACCEPTED | "Trade Accepted" | "Your trade with {house} has been accepted" | NORMAL |
| ECONOMY_UPDATE | "Economy Cycle" | "New cycle complete.  Balance: {balance}" | LOW |
| ADMIN_ANNOUNCEMENT | "{title}" | "{message}" | HIGH |
| CHARACTER_STATUS | "Character Update" | "{name} status changed to {status}" | NORMAL |

### 8.2 FCM Implementation Requirements

#### Server-Side
1. Initialize Firebase Admin SDK with service account credentials
2. Store FCM tokens per user in database
3. Implement token refresh handling
4. Create notification payload builder
5. Handle token invalidation (remove stale tokens)
6. Support topic-based messaging for broadcast

#### Client-Side
1. Implement `FirebaseMessagingService` subclass
2. Handle foreground notifications (show in-app banner)
3. Handle background notifications (system tray)
4. Implement deep linking from notification tap
5. Request notification permission (Android 13+)
6. Send token to server on login and refresh

### 8.3 Notification Channels (Android)

| Channel ID | Name | Importance | Description |
|------------|------|------------|-------------|
| `votes` | "Votes & Decisions" | HIGH | Vote requests and results |
| `trades` | "Trade Deals" | DEFAULT | Trade proposals and updates |
| `economy` | "Economy Updates" | LOW | Cycle updates, balance changes |
| `announcements` | "Game Master" | HIGH | Admin announcements |

---

## 9. Build & Deployment Specification

### 9.1 Android Build Configuration

#### Gradle Configuration Requirements
```
Module:  app
├── compileSdk:  35
├── minSdk: 26
├── targetSdk: 35
├── Kotlin version: 1.9.x
├── Compose Compiler:  Compatible with Kotlin version
├── Build features: 
│   ├── compose: true
│   └── buildConfig: true
├── Signing configs:
│   ├── debug (auto-generated)
│   └── release (keystore required)
└── Build types:
    ├── debug
    │   ├── debuggable:  true
    │   ├── minifyEnabled: false
    │   └── Server URL: http://10.0.2.2:8080 (emulator localhost)
    └── release
        ├── debuggable: false
        ├── minifyEnabled: true
        ├── proguardFiles:  proguard-rules.pro
        └── Server URL: https://your-server.com
```

#### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate signed APK
./gradlew bundleRelease
```

### 9.2 Server Build & Deployment

#### Build Configuration
```
Build tool: Gradle with Kotlin DSL
├── Ktor version: 2.3.x
├── Kotlin version: 1.9.x (match Android)
├── Exposed version: Latest stable
├── PostgreSQL driver: Latest
└── Firebase Admin SDK: Latest
```

#### Build Commands
```bash
# Build fat JAR
./gradlew shadowJar

# Run locally
./gradlew run

# Run tests
./gradlew test

# Output:  build/libs/server-all.jar
```

#### Deployment Requirements
1. **VPS Specifications**
   - OS: Ubuntu 22.04 LTS
   - RAM: 1GB minimum (2GB recommended)
   - Storage: 20GB SSD
   - CPU: 1 vCPU

2. **Software Requirements**
   - Java 17 JRE
   - PostgreSQL 15
   - Nginx (reverse proxy, SSL termination)
   - Certbot (Let's Encrypt SSL)
   - systemd (service management)

3. **Environment Variables**
   ```
   DATABASE_URL=jdbc:postgresql://localhost:5432/dune_ttrpg
   DATABASE_USER=dune_app
   DATABASE_PASSWORD=<secure_password>
   JWT_SECRET=<secure_random_string>
   JWT_ISSUER=dune-ttrpg
   JWT_AUDIENCE=dune-ttrpg-clients
   FIREBASE_CREDENTIALS_PATH=/etc/dune/firebase-credentials.json
   ```

4. **Systemd Service File**
   ```
   Location: /etc/systemd/system/dune-server.service
   - ExecStart:  java -jar /opt/dune/server-all.jar
   - Restart: always
   - User: dune (non-root)
   ```

5. **Nginx Configuration**
   - Reverse proxy to localhost:8080
   - SSL certificate via Let's Encrypt
   - WebSocket upgrade headers
   - Rate limiting (optional for your use case)

### 9.3 Firebase Project Setup

1. Create Firebase project in Firebase Console
2. Add Android app with package name
3. Download `google-services.json` → place in `app/` folder
4. Enable Cloud Messaging
5. Generate server key for Admin SDK
6. Download service account JSON for server

---

## 10. Testing Specification

### 10.1 Android Testing Strategy

#### Unit Tests (JVM)
| Component | Test Focus | Tools |
|-----------|------------|-------|
| ViewModels | State management, UI logic | JUnit5, Mockk, Turbine (Flow testing) |
| Use Cases | Business logic, repository calls | JUnit5, Mockk |
| Repositories | Data transformation, caching | JUnit5, Mockk |
| Mappers | DTO ↔ Domain model conversion | JUnit5 |

#### Integration Tests (Instrumented)
| Component | Test Focus | Tools |
|-----------|------------|-------|
| Room Database | DAO operations, migrations | AndroidJUnit, Room testing |
| API Client | Request/response handling | MockWebServer |
| Navigation | Screen transitions | Compose Testing |

#### UI Tests
| Component | Test Focus | Tools |
|-----------|------------|-------|
| Screens | UI rendering, interactions | Compose Testing |
| End-to-End | Full user flows | Compose Testing |

### 10.2 Server Testing Strategy

#### Unit Tests
| Component | Test Focus | Tools |
|-----------|------------|-------|
| Services | Business logic | JUnit5, Mockk |
| Repositories | Database queries | JUnit5, Testcontainers (PostgreSQL) |
| Route handlers | Request handling | Ktor Test Host |

#### Integration Tests
| Component | Test Focus | Tools |
|-----------|------------|-------|
| API Endpoints | Full request cycle | Ktor Test Host |
| WebSocket | Connection, events | Ktor Test Host |
| Database | Schema, queries | Testcontainers |

### 10.3 Manual Testing Checklist

#### Authentication Flow
- [ ] Login with valid credentials
- [ ] Login with invalid credentials (error shown)
- [ ] Token refresh after expiration
- [ ] Logout clears local data
- [ ] FCM token registered after login

#### House Management
- [ ] View house overview
- [ ] View economy details
- [ ] View character list
- [ ] View character details
- [ ] View army list
- [ ] View army details

#### Voting System
- [ ] View pending votes
- [ ] Cast vote (approve)
- [ ] Cast vote (reject)
- [ ] Cannot vote twice
- [ ] Vote resolution triggers correctly
- [ ] Consensus vote works correctly
- [ ] Majority vote works correctly

#### Trade System
- [ ] Propose trade
- [ ] Accept trade
- [ ] Reject trade
- [ ] Counter-offer trade
- [ ] Trade execution transfers resources

#### Notifications
- [ ] Receive notification when vote created
- [ ] Receive notification when trade proposed
- [ ] Notification tap opens correct screen
- [ ] Foreground notification displays banner

#### Admin Functions
- [ ] Trigger economy cycle
- [ ] Edit house details
- [ ] Create/edit characters
- [ ] Create/edit armies
- [ ] Send announcement
- [ ] All players receive announcement

---

## 11. Security Considerations

### 11.1 Authentication & Authorization
- JWT tokens with expiration (1 hour access, 7 day refresh)
- Password hashing with BCrypt (cost factor 12)
- Role-based access control on all endpoints
- Admin-only endpoints protected by role check

### 11.2 Network Security
- HTTPS only in production
- Certificate pinning (optional, recommended)
- API rate limiting on server
- Input validation on all endpoints

### 11.3 Data Security
- No sensitive data in logs
- FCM tokens encrypted at rest
- Database credentials in environment variables
- Firebase credentials not in repository

### 11.4 Client Security
- ProGuard obfuscation in release builds
- No hardcoded secrets in app
- Server URL configurable via build config
- Token stored in EncryptedSharedPreferences

---

## 12. Development Milestones

### Phase 1: Foundation
- [ ] Set up Android project structure
- [ ] Set up server project structure
- [ ] Configure Firebase project
- [ ] Implement database schema
- [ ] Implement authentication (login/logout)
- [ ] Basic API connectivity

### Phase 2: Core Features
- [ ] House management screens
- [ ] Character CRUD operations
- [ ] Army management
- [ ] WebSocket connection
- [ ] Push notification integration

### Phase 3: Game Mechanics
- [ ] Voting system (create, cast, resolve)
- [ ] Trade deal system
- [ ] Economy simulation engine
- [ ] Event feed

### Phase 4: Admin Features
- [ ] Admin dashboard
- [ ] Manual economy triggers
- [ ] Announcement system
- [ ] User management

### Phase 5: Polish & Testing
- [ ] UI polish and animations
- [ ] Error handling improvements
- [ ] Comprehensive testing
- [ ] Performance optimization
- [ ] Deployment setup

---

## 13. File Deliverables Checklist

### Android Project
- [ ] `settings.gradle. kts` - Project settings
- [ ] `build.gradle.kts` (project) - Root build config
- [ ] `app/build.gradle.kts` - App module config
- [ ] `gradle. properties` - Gradle properties
- [ ] `local.properties` - Local SDK paths (not committed)
- [ ] `proguard-rules.pro` - ProGuard configuration
- [ ] `google-services.json` - Firebase config (not committed)
- [ ] All source files per module structure

### Server Project
- [ ] `settings.gradle.kts` - Project settings
- [ ] `build.gradle.kts` - Build config with Shadow plugin
- [ ] `application. conf` - Ktor configuration
- [ ] `logback.xml` - Logging configuration
- [ ] All source files per module structure
- [ ] `Dockerfile` (optional, for containerized deployment)

### Documentation
- [ ] README.md - Project overview, setup instructions
- [ ] API. md - API documentation
- [ ] DEPLOYMENT.md - Deployment guide

### Configuration Files (Templates)
- [ ] `.env. example` - Environment variable template
- [ ] `firebase-credentials.json. example` - Firebase config template
- [ ] `nginx.conf.example` - Nginx configuration template
- [ ] `dune-server.service.example` - Systemd service template

---

This specification provides a complete blueprint for building the Dune TTRPG companion application.  Each section can be referenced independently when implementing specific features.  The modular architecture ensures components can be developed in parallel and tested in isolation. 
