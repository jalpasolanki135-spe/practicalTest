package com.lumoslogic.test.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

@Composable
fun PostListScreen(
    navController: NavHostController,
    viewModel: PostListViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsState()

    Scaffold { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {

            when {

                state.isLoading && state.posts.isEmpty() -> {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center)
                    )
                }

                state.error != null -> {
                    Text(
                        text = state.error!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.posts.isEmpty() -> {
                    Text(
                        text = "No Posts Found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {

                    LazyColumn {

                        items(state.posts.size) { index ->

                            val post = state.posts[index]

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .clickable {

                                        navController.navigate(
                                            "detail/${post.title}/${post.body}"
                                        )
                                    }
                            ) {

                                Text(
                                    text = post.title,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            if (index == state.posts.lastIndex) {
                                LaunchedEffect(Unit) {
                                    viewModel.loadMore()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}