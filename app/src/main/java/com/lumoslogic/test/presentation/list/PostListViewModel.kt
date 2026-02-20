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

    init {
        observePosts()
        loadMore()
    }

    private fun observePosts() {

        viewModelScope.launch {

            repository.observePosts().collect { posts ->

                _state.update {
                    it.copy(
                        posts = posts,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadMore() {

        viewModelScope.launch {

            _state.update { it.copy(isLoading = true) }

            try {
                repository.loadNextPage()
            } catch (e: Exception) {

                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Something went wrong"
                    )
                }
            }
        }
    }
}