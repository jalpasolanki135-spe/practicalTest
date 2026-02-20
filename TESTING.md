# Testing Guide

## Overview

This project includes comprehensive unit tests for ViewModels and business logic. Tests use MockK for mocking, Turbine for Flow testing, and Coroutines Test for async operations.

## Test Structure

```
app/src/test/java/com/lumoslogic/test/
├── presentation/
│   └── list/
│       └── PostListViewModelTest.kt
└── [Additional test files as needed]
```

## Running Tests

### From Android Studio
1. Right-click on the `test` folder
2. Select "Run Tests"

### From Command Line
```bash
./gradlew test
```

### Specific Test Class
```bash
./gradlew test --tests "com.lumoslogic.test.presentation.list.PostListViewModelTest"
```

## Test Dependencies

```kotlin
// In app/build.gradle.kts
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("com.google.dagger:hilt-android-testing:2.51")
kaptTest("com.google.dagger:hilt-android-compiler:2.51")
```

## Writing Unit Tests

### ViewModel Test Template

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var repository: MyRepository

    private lateinit var viewModel: MyViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test description`() = runTest {
        // Given - Setup mocks
        coEvery { repository.someMethod() } returns expectedResult

        // When - Execute action
        viewModel.someAction()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Verify results
        assertEquals(expectedState, viewModel.state.value)
        coVerify { repository.someMethod() }
    }
}
```

## Key Testing Patterns

### 1. StateFlow Testing with Turbine
```kotlin
@Test
fun `state should update correctly`() = runTest {
    viewModel.state.test {
        assertEquals(InitialState, awaitItem())
        
        viewModel.loadData()
        assertEquals(LoadingState, awaitItem())
        assertEquals(SuccessState, awaitItem())
    }
}
```

### 2. Mocking Suspended Functions
```kotlin
coEvery { repository.loadNextPage() } returns Unit
coEvery { repository.loadNextPage() } throws NetworkException()
```

### 3. Testing Error Handling
```kotlin
@Test
fun `should handle error and update state`() = runTest {
    // Given
    coEvery { repository.loadNextPage() } throws Exception("Network error")
    
    // When
    viewModel.loadMore()
    testDispatcher.scheduler.advanceUntilIdle()
    
    // Then
    val state = viewModel.state.value
    assertTrue(state.error != null)
    assertFalse(state.isLoading)
}
```

## Coverage Goals

- **ViewModels**: 90%+ coverage
- **Repository**: 80%+ coverage
- **Mappers**: 100% coverage
- **Validation**: 100% coverage

## Future Test Additions

### Integration Tests
- Repository integration with database
- API service with mock web server
- End-to-end navigation tests

### UI Tests
- Compose UI interactions
- Screenshot tests
- Accessibility tests

---

*Last Updated: February 20, 2026*
