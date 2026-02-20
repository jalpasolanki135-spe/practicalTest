package com.lumoslogic.test.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumoslogic.test.presentation.detail.PostDetailScreen
import com.lumoslogic.test.presentation.list.PostListScreen

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "list"
    ) {

        composable("list") {
            PostListScreen(navController)
        }

        composable(
            "detail/{title}/{body}"
        ) {

            PostDetailScreen(
                title = it.arguments?.getString("title") ?: "",
                body = it.arguments?.getString("body") ?: ""
            )
        }
    }
}