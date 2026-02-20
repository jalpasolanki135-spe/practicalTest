package com.lumoslogic.test.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lumoslogic.test.presentation.detail.PostDetailScreen
import com.lumoslogic.test.presentation.list.PostListScreen

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {

        composable("list") {
            PostListScreen(navController)
        }

        composable(
            route = "detail/{title}/{body}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("body") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
            val encodedBody = backStackEntry.arguments?.getString("body") ?: ""

            PostDetailScreen(
                title = NavigationUtils.decode(encodedTitle),
                body = NavigationUtils.decode(encodedBody)
            )
        }
    }
}
