package com.lumoslogic.test.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumoslogic.test.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostListViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostListUiState())
    val state = _state.asStateFlow()

    private var lastLoadedPage = 0

    init {
        observePosts()
        loadMore()
    }

    private fun observePosts() {
        viewModelScope.launch {
            repository.observePosts().collect { posts ->
                _state.update { currentState ->
                    // Stop paginating when data is loaded
                    val isPaginating = currentState.isPaginating && posts.size == currentState.posts.size
                    
                    // Check if we have more data (less than page size means no more)
                    val hasMoreData = posts.size % 20 == 0 && posts.size < 100
                    
                    currentState.copy(
                        posts = posts,
                        isLoading = false,
                        isRefreshing = false,
                        isPaginating = false,
                        hasMoreData = hasMoreData
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    isRefreshing = true, 
                    error = null,
                    hasMoreData = true
                ) 
            }
            lastLoadedPage = 0
            try {
                repository.refreshPosts()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        error = "Failed to refresh: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadMore() {
        val currentState = _state.value
        
        // Prevent duplicate calls
        if (currentState.isPaginating || !currentState.hasMoreData) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isPaginating = true, error = null) }

            try {
                repository.loadNextPage()
                // Success - observePosts will update state
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isPaginating = false,
                        error = "Failed to load more: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}