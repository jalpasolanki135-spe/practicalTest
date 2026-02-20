package com.lumoslogic.test.presentation.list

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.lumoslogic.test.domain.model.Post
import com.lumoslogic.test.domain.repository.PostRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostListViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var repository: PostRepository

    private lateinit var viewModel: PostListViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Default mock behavior
        coEvery { repository.observePosts() } returns flowOf(emptyList())
        coEvery { repository.loadNextPage() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load posts and start observing`() = runTest {
        // Given
        val posts = listOf(
            Post(1, "Title 1", "Body 1"),
            Post(2, "Title 2", "Body 2")
        )
        coEvery { repository.observePosts() } returns flowOf(posts)

        // When
        viewModel = PostListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(posts, state.posts)
        assertFalse(state.isLoading)
    }

    @Test
    fun `refresh should clear error and set refreshing state`() = runTest {
        // Given
        coEvery { repository.refreshPosts() } returns Unit
        viewModel = PostListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isRefreshing)
        assertEquals(null, state.error)
        coVerify { repository.refreshPosts() }
    }

    @Test
    fun `loadMore should not load when already paginating`() = runTest {
        // Given
        viewModel = PostListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // First call starts pagination
        viewModel.loadMore()

        // When - Second call should be ignored
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.loadNextPage() }
    }

    @Test
    fun `loadMore should not load when no more data`() = runTest {
        // Given
        viewModel = PostListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate no more data
        val currentState = viewModel.state.value
        viewModel.state.value = currentState.copy(hasMoreData = false)

        // When
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { repository.loadNextPage() }
    }

    @Test
    fun `loadMore should handle error and update state`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { repository.loadNextPage() } throws Exception(errorMessage)
        viewModel = PostListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isPaginating)
        assertTrue(state.error?.contains(errorMessage) == true)
    }

    @Test
    fun `clearError should remove error from state`() = runTest {
        // Given
        coEvery { repository.loadNextPage() } throws Exception("Error")
        viewModel = PostListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.error != null)

        // When
        viewModel.clearError()

        // Then
        assertEquals(null, viewModel.state.value.error)
    }
}