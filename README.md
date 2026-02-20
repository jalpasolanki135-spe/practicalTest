# Posts App

A sample Android application demonstrating Clean Architecture with MVVM pattern using Jetpack Compose. The app displays a paginated list of posts fetched from a remote API with offline-first caching using Room database.

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or higher
- Android SDK with minimum API 24 (Android 7.0)

### Build & Run
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd practicalTest
   ```

2. Open the project in Android Studio

3. Sync Gradle files (Android Studio will prompt automatically)

4. Build and run the app:
   - Select a device/emulator (API 24+, minSdk is 24)
   - Click "Run" (Ctrl+R) or use: `./gradlew installDebug`

### Project Structure
```
app/src/main/java/com/lumoslogic/test/
├── data/                    # Data layer (Repository implementations)
│   ├── local/              # Room database, DAOs, Entities
│   ├── remote/             # Retrofit API service, DTOs
│   ├── mapper/             # Data transformation between layers
│   └── repository/         # Repository implementations
├── domain/                 # Domain layer (Business logic)
│   ├── model/              # Domain models (Post)
│   └── repository/          # Repository interfaces
├── presentation/           # UI layer (Jetpack Compose)
│   ├── list/               # Post list screen with ViewModel
│   ├── detail/             # Post detail screen
│   └── navigation/         # Navigation graph
├── di/                     # Dependency Injection (Hilt modules)
└── MainActivity.kt         # Entry point
```

## Architecture Overview

### Clean Architecture with MVVM
The app follows **Clean Architecture** principles with three distinct layers:

```
Presentation Layer (UI)
    ↓
Domain Layer (Business Logic)
    ↓
Data Layer (Sources)
```

#### Layer Details

**1. Presentation Layer** (`presentation/`)
- **UI**: Jetpack Compose screens (`PostListScreen`, `PostDetailScreen`)
- **State Management**: MVI pattern using `StateFlow` in `ViewModel`
- **Navigation**: Jetpack Navigation Compose with type-safe navigation

**2. Domain Layer** (`domain/`)
- **Models**: Domain entities (`Post`) - pure Kotlin data classes
- **Repository Interfaces**: Contract definitions for data operations
- **No dependencies** on Android framework or external libraries

**3. Data Layer** (`data/`)
- **Repository Implementations**: `PostRepositoryImpl` implements domain contracts
- **Remote**: Retrofit API service consuming JSONPlaceholder API
- **Local**: Room database for offline caching with `PostDao` and `PostEntity`
- **Mappers**: Data transformation between DTOs, Entities, and Domain models

### Key Libraries
- **UI**: Jetpack Compose (Material3)
- **DI**: Dagger Hilt
- **Networking**: Retrofit2 + Gson
- **Database**: Room
- **Navigation**: Jetpack Navigation Compose
- **Async**: Kotlin Coroutines + StateFlow

## Key Decisions and Trade-offs

### 1. Offline-First Architecture
**Decision**: Room database as single source of truth, API updates local cache.

**Pros**:
- Users see cached data immediately (better UX)
- Works offline after initial load
- Consistent data across app sessions

**Cons**:
- Increased complexity (sync logic, database management)
- Slightly higher storage usage
- Stale data if not refreshed properly

### 2. Pagination with Simple Page Counter
**Decision**: Page-based pagination using `_page` and `_limit` query parameters.

**Pros**:
- Simple implementation
- Efficient for linear, append-only data
- Easy to integrate with LazyColumn

**Cons**:
- No support for cursor-based pagination (better for dynamic data)
- Race conditions possible if user scrolls rapidly
- No retry mechanism for failed page loads

### 3. Repository Pattern with Flow
**Decision**: Repository exposes `Flow<List<Post>>` for reactive UI updates.

**Pros**:
- Automatic UI updates when database changes
- Lifecycle-aware data observation
- Decoupled UI from data source

**Cons**:
- More complex than simple callback/list approach
- Requires understanding of Flow/StateFlow concepts

### 4. Error Handling Strategy
**Decision**: Silent failure for network errors (offline case), surface errors via UI state.

**Pros**:
- App remains usable offline
- Graceful degradation

**Cons**:
- Users may not realize data is stale
- No explicit retry mechanisms visible to user

### 5. Hilt for Dependency Injection
**Decision**: Dagger Hilt for compile-time DI.

**Pros**:
- Compile-time safety
- Boilerplate reduction
- Easy ViewModel injection
- Android framework integration

**Cons**:
- Build time slightly increased
- Learning curve for annotation-based DI
- Kapt/KSP configuration complexity

### 6. Jetpack Compose for UI
**Decision**: 100% Compose UI with Material3 components.

**Pros**:
- Modern declarative UI
- Less boilerplate than XML
- Easy theming and customization
- State-driven UI updates

**Cons**:
- Runtime performance considerations (recomposition)
- Smaller ecosystem than View system
- Tooling still evolving

## Known Limitations

### 1. **No Pull-to-Refresh**
- Users cannot manually refresh data
- Only loads more on scroll to bottom
- **Workaround**: Restart the app

### 2. **No Error Retry UI**
- Failed network requests fail silently
- No visible retry button for users
- **Impact**: Users may see infinite loading spinner

### 3. **No Cache Invalidation**
- Data never expires from local cache
- No mechanism to force refresh old data
- **Impact**: Stale data may persist indefinitely

### 4. **Simple Error Messages**
- Generic "Something went wrong" message
- No specific error types (network, server, parsing)
- **Impact**: Poor debugging experience for users

### 5. **No Loading Indicators for Pagination**
- No footer progress indicator when loading more
- Users can't tell if more data is coming
- **Impact**: UX confusion during infinite scroll

### 6. **No Data Validation**
- No validation of API response data
- Empty or malformed posts can be saved
- **Impact**: Potential crashes or UI glitches

### 7. **Single Theme Support**
- Only light theme implemented
- No dark mode support
- **Impact**: Eye strain in low-light conditions

### 8. **No Accessibility Features**
- Missing content descriptions
- No semantic markup for screen readers
- **Impact**: App unusable for visually impaired users

### 9. **Navigation URL Encoding Issues**
- Post title/body passed in navigation route without encoding
- Special characters may break navigation
- **Impact**: Crashes or failed navigation with certain content

### 10. **No Unit/Integration Tests**
- No test coverage for ViewModels, Repository, or UI
- Manual testing only
- **Impact**: Regressions possible with future changes

## Future Improvements

1. Add pull-to-refresh mechanism
2. Implement proper error retry with Snackbar/Dialog
3. Add cache expiration policy (e.g., 1 hour TTL)
4. Implement loading footer for pagination
5. Add comprehensive error handling with specific messages
6. Support dark mode theme
7. Add accessibility labels and semantic descriptions
8. Fix navigation argument encoding (use JSON or proper URL encoding)
9. Add unit tests for ViewModels and Repository
10. Add UI tests with Compose testing framework

## API Reference

This app uses [JSONPlaceholder](https://jsonplaceholder.typicode.com/) - a free fake REST API for testing and prototyping.

**Endpoint**: `GET https://jsonplaceholder.typicode.com/posts`

**Query Parameters**:
- `_page`: Page number (starting from 1)
- `_limit`: Items per page (default: 20)

## License

This project is for demonstration purposes.
