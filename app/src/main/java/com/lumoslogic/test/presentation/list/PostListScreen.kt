package com.lumoslogic.test.presentation.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lumoslogic.test.presentation.components.EmptyStateMessage
import com.lumoslogic.test.presentation.components.ErrorMessage
import com.lumoslogic.test.presentation.components.LoadingIndicator
import com.lumoslogic.test.presentation.components.PostCard
import com.lumoslogic.test.presentation.navigation.NavigationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListScreen(
    navController: NavHostController,
    viewModel: PostListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(state.isRefreshing) {
        if (!state.isRefreshing) {
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Posts") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            when {
                state.isLoading && state.posts.isEmpty() -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.error != null && state.posts.isEmpty() -> {
                    ErrorMessage(
                        error = state.error!!,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.posts.isEmpty() -> {
                    EmptyStateMessage(
                        message = "No Posts Found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    PostListContent(
                        state = state,
                        onPostClick = { post ->
                            val safeRoute = NavigationUtils.createRoute(
                                "detail",
                                post.title,
                                post.body
                            )
                            navController.navigate(safeRoute)
                        },
                        onLoadMore = { viewModel.loadMore() }
                    )
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun PostListContent(
    state: PostListUiState,
    onPostClick: (com.lumoslogic.test.domain.model.Post) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = state.posts.size,
            key = { index -> state.posts[index].id }
        ) { index ->
            val post = state.posts[index]

            PostCard(
                number = index + 1,
                post = post,
                onClick = { onPostClick(post) }
            )

            // Pagination trigger
            if (index == state.posts.lastIndex && state.hasMoreData) {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
            }
        }

        // Pagination loading footer - only show if more data exists
        if (state.isPaginating && state.hasMoreData) {
            item {
                LoadingIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Error footer with retry
        if (state.error != null && state.posts.isNotEmpty()) {
            item {
                ErrorMessage(
                    error = state.error!!,
                    onRetry = onLoadMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}