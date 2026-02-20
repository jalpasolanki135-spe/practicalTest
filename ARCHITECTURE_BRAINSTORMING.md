# Architecture Brainstorming Document

## Current Architecture Analysis

### Existing Clean Architecture Layers
```
Presentation (UI Layer)
    ↓ StateFlow, ViewModel
Domain (Business Logic)
    ↓ Repository Interfaces
Data (Implementation)
    ↓ Room, Retrofit
```

**Strengths**:
- Clear separation of concerns
- Offline-first with Room database
- Reactive UI with StateFlow
- Dependency injection with Hilt

**Weaknesses**:
- Simple error handling (basic try-catch)
- No retry mechanisms
- No cache invalidation strategy
- Silent failures mask issues

---

## Proposed Architecture Improvements

### 1. Error Handling Strategy

#### Current State
```kotlin
// Basic try-catch with generic error
try {
    repository.loadNextPage()
} catch (e: Exception) {
    _state.update {
        it.copy(error = "Something went wrong")
    }
}
```

#### Proposed: Sealed Class Error Types
```kotlin
sealed class PostError {
    abstract val message: String
    
    data class NetworkError(override val message: String) : PostError()
    data class ServerError(val code: Int, override val message: String) : PostError()
    data class DatabaseError(override val message: String) : PostError()
    data class ValidationError(override val message: String) : PostError()
    data class UnknownError(val throwable: Throwable) : PostError() {
        override val message: String = throwable.message ?: "Unknown error"
    }
}

// Usage in ViewModel
fun loadMore() {
    viewModelScope.launch {
        _state.update { it.copy(isPaginating = true) }
        
        repository.loadNextPage()
            .fold(
                onSuccess = { /* handled by Flow */ },
                onFailure = { error ->
                    val postError = when (error) {
                        is IOException -> PostError.NetworkError("Check your connection")
                        is HttpException -> PostError.ServerError(
                            error.code(), 
                            "Server error: ${error.message()}"
                        )
                        is SQLiteException -> PostError.DatabaseError("Database error")
                        else -> PostError.UnknownError(error)
                    }
                    _state.update { it.copy(error = postError) }
                }
            )
    }
}
```

**Benefits**:
- Specific error messages for users
- Different UI for different errors (retry button only for network)
- Better logging and analytics
- Easier debugging

---

### 2. Retry Mechanisms

#### Option A: Exponential Backoff (Recommended)
```kotlin
class RetryPolicy(
    private val maxRetries: Int = 3,
    private val initialDelay: Long = 1000L,
    private val maxDelay: Long = 5000L,
    private val factor: Double = 2.0
) {
    suspend fun <T> execute(
        action: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        
        repeat(maxRetries) { attempt ->
            try {
                return Result.success(action())
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    return Result.failure(e)
                }
                
                // Only retry on network/server errors
                if (!isRetryableError(e)) {
                    return Result.failure(e)
                }
                
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        
        return Result.failure(IllegalStateException("Retry exhausted"))
    }
    
    private fun isRetryableError(e: Exception): Boolean {
        return e is IOException || 
               (e is HttpException && e.code() in 500..599)
    }
}

// Repository usage
class PostRepositoryImpl(
    private val database: AppDatabase,
    private val api: PostApiService,
    private val retryPolicy: RetryPolicy = RetryPolicy()
) : PostRepository {
    
    override suspend fun loadNextPage(): Result<Unit> = retryPolicy.execute {
        val response = api.getPosts(currentPage)
        database.postDao().insertPosts(response.map { it.toEntity() })
    }
}
```

**Why Exponential Backoff?**
- Prevents server overload during recovery
- Gives transient failures time to resolve
- Respects server resources
- Industry standard (used by AWS, Google APIs)

#### Option B: Circuit Breaker Pattern
```kotlin
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val recoveryTimeout: Long = 30000L
) {
    private enum class State { CLOSED, OPEN, HALF_OPEN }
    
    private var state = State.CLOSED
    private var failureCount = 0
    private var lastFailureTime: Long = 0
    
    suspend fun <T> execute(action: suspend () -> T): Result<T> {
        return when (state) {
            State.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > recoveryTimeout) {
                    state = State.HALF_OPEN
                    execute(action)
                } else {
                    Result.failure(IllegalStateException("Circuit breaker is OPEN"))
                }
            }
            
            State.CLOSED, State.HALF_OPEN -> {
                try {
                    val result = action()
                    onSuccess()
                    Result.success(result)
                } catch (e: Exception) {
                    onFailure()
                    Result.failure(e)
                }
            }
        }
    }
    
    private fun onSuccess() {
        failureCount = 0
        state = State.CLOSED
    }
    
    private fun onFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        
        if (failureCount >= failureThreshold) {
            state = State.OPEN
        }
    }
}
```

**When to use Circuit Breaker?**
- External APIs with frequent failures
- Cascading failure prevention
- Graceful degradation

**Recommendation**: Start with Exponential Backoff (simpler), add Circuit Breaker if API reliability is critical.

---

### 3. Cache Invalidation Strategy

#### Current State
- Cache never expires
- No way to force refresh
- Stale data persists indefinitely

#### Proposed: Time-Based Invalidation (TTL)
```kotlin
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val body: String,
    val cachedAt: Long = System.currentTimeMillis() // New field
)

class CacheManager(
    private val defaultTtl: Long = TimeUnit.HOURS.toMillis(1)
) {
    fun isCacheValid(cachedAt: Long, ttl: Long = defaultTtl): Boolean {
        return System.currentTimeMillis() - cachedAt < ttl
    }
}

// Repository with TTL
class PostRepositoryImpl(
    private val database: AppDatabase,
    private val api: PostApiService,
    private val cacheManager: CacheManager = CacheManager()
) : PostRepository {
    
    override fun observePosts(): Flow<List<Post>> {
        return database.postDao()
            .observePosts()
            .map { entities ->
                // Check cache validity
                val validPosts = entities.filter {
                    cacheManager.isCacheValid(it.cachedAt)
                }
                
                // If cache is stale, trigger background refresh
                if (validPosts.size < entities.size) {
                    CoroutineScope(Dispatchers.IO).launch {
                        refreshPosts()
                    }
                }
                
                validPosts.map { it.toDomain() }
            }
    }
}
```

#### Alternative: Version-Based Invalidation
```kotlin
// For APIs that support ETag or versioning
interface CacheableResponse {
    val version: String
    val etag: String?
}

class VersionBasedCache<T : CacheableResponse> {
    private val versionStore: MutableMap<String, String> = mutableMapOf()
    
    suspend fun fetchWithVersionCheck(
        key: String,
        fetch: suspend (currentVersion: String?) -> T
    ): T {
        val currentVersion = versionStore[key]
        val response = fetch(currentVersion)
        
        // If version changed, update cache
        if (response.version != currentVersion) {
            versionStore[key] = response.version
        }
        
        return response
    }
}
```

**Recommendation**: Use Time-Based Invalidation (TTL) for this app because:
- JSONPlaceholder API doesn't support versioning
- Simple to implement and understand
- 1-hour TTL is reasonable for blog posts
- Can be combined with pull-to-refresh for manual refresh

---

### 4. Resilience Patterns

#### A. Fallback Strategy
```kotlin
class PostRepositoryWithFallback(
    private val primaryRepository: PostRepository,
    private val fallbackRepository: PostRepository // Local JSON cache
) : PostRepository {
    
    override suspend fun loadNextPage() {
        try {
            primaryRepository.loadNextPage()
        } catch (e: Exception) {
            // Log error
            fallbackRepository.loadNextPage()
        }
    }
}
```

#### B. Bulkhead Pattern (Isolation)
```kotlin
// Separate thread pools for different operations
class BulkheadExecutor {
    private val ioDispatcher = Dispatchers.IO.limitedParallelism(10)
    private val databaseDispatcher = Dispatchers.IO.limitedParallelism(4)
    
    suspend fun <T> executeNetwork(block: suspend () -> T): T {
        return withContext(ioDispatcher) { block() }
    }
    
    suspend fun <T> executeDatabase(block: suspend () -> T): T {
        return withContext(databaseDispatcher) { block() }
    }
}
```

#### C. Timeout Pattern
```kotlin
class TimeoutConfig(
    val networkTimeout: Long = 10000L,
    val databaseTimeout: Long = 5000L
)

suspend fun <T> withTimeout(
    timeoutMs: Long,
    block: suspend () -> T
): T {
    return withTimeout(timeoutMs) { block() }
}

// Usage
override suspend fun loadNextPage() {
    withTimeout(config.networkTimeout) {
        api.getPosts(currentPage)
    }
}
```

---

## Implementation Priority

### Phase 1: Error Handling (High Impact, Low Effort)
1. Create `PostError` sealed class
2. Update UI to show specific error messages
3. Add retry button only for retryable errors

**Estimated Time**: 2-3 hours  
**Impact**: Better UX, easier debugging

### Phase 2: Retry Mechanisms (High Impact, Medium Effort)
1. Implement `RetryPolicy` with exponential backoff
2. Integrate into Repository
3. Add unit tests for retry logic

**Estimated Time**: 4-5 hours  
**Impact**: Improved reliability, better offline handling

### Phase 3: Cache Invalidation (Medium Impact, Medium Effort)
1. Add `cachedAt` field to `PostEntity`
2. Implement `CacheManager`
3. Add background refresh logic
4. Update UI to show cache staleness

**Estimated Time**: 3-4 hours  
**Impact**: Fresh data, reduced API calls

### Phase 4: Advanced Patterns (Low Priority)
- Circuit Breaker (if API reliability issues)
- Bulkhead (if high concurrency needed)
- Fallback (if offline-first is critical)

**Estimated Time**: 6-8 hours  
**Impact**: Enterprise-grade resilience

---

## Trade-offs Summary

| Improvement | Complexity | Performance Impact | UX Impact | Priority |
|-------------|------------|-------------------|-----------|----------|
| Typed Errors | Low | None | High | P0 |
| Retry with Backoff | Medium | Better | High | P1 |
| TTL Cache | Medium | Better | Medium | P1 |
| Circuit Breaker | High | Neutral | Low | P2 |
| Bulkhead | High | Neutral | None | P3 |

---

## Recommended Next Steps

1. **Immediate**: Implement typed error handling (sealed class)
2. **This Week**: Add exponential backoff retry
3. **Next Sprint**: Implement TTL-based cache invalidation
4. **Future**: Consider Circuit Breaker if API issues persist

---

*Document Generated By: Firebender AI (Architecture Brainstorming)*  
*Date: February 20, 2026*
