package com.telepromptervibes.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.telepromptervibes.app.ui.editor.EditorScreen
import com.telepromptervibes.app.ui.home.HomeScreen

private object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val EDITOR_ARG = "scriptId"
    const val EDITOR_ROUTE = "$EDITOR?$EDITOR_ARG={$EDITOR_ARG}"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToEditor = { scriptId ->
                    if (scriptId == null) {
                        navController.navigate(Routes.EDITOR)
                    } else {
                        navController.navigate("${Routes.EDITOR}?${Routes.EDITOR_ARG}=$scriptId")
                    }
                }
            )
        }

        composable(
            route = Routes.EDITOR_ROUTE,
            arguments = listOf(
                navArgument(Routes.EDITOR_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) { backStackEntry ->
            val scriptId = backStackEntry.arguments?.getString(Routes.EDITOR_ARG)
            EditorScreen(
                scriptId = scriptId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
