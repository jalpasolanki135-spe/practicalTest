package com.lumoslogic.test.presentation.list

import com.lumoslogic.test.domain.model.Post

data class PostListUiState(
    val isLoading: Boolean = false,
    val posts: List<Post> = emptyList(),
    val error: String? = null
)