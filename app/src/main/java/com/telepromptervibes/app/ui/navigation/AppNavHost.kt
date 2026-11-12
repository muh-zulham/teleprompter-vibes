package com.telepromptervibes.app.ui.navigation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.telepromptervibes.app.overlay.OverlayController
import com.telepromptervibes.app.overlay.OverlayService
import com.telepromptervibes.app.ui.editor.EditorScreen
import com.telepromptervibes.app.ui.home.HomeScreen
import com.telepromptervibes.app.ui.prompter.PrompterScreen

private object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val EDITOR_ARG = "scriptId"
    const val EDITOR_ROUTE = "$EDITOR?$EDITOR_ARG={$EDITOR_ARG}"
    const val PROMPTER = "prompter"
    const val PROMPTER_ROUTE = "$PROMPTER?$EDITOR_ARG={$EDITOR_ARG}"
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
                onStartPrompter = { savedId ->
                    navController.navigate("${Routes.PROMPTER}?${Routes.EDITOR_ARG}=$savedId")
                },
            )
        }

        composable(
            route = Routes.PROMPTER_ROUTE,
            arguments = listOf(
                navArgument(Routes.EDITOR_ARG) {
                    type = NavType.StringType
                    nullable = false
                }
            ),
        ) { backStackEntry ->
            val scriptId = backStackEntry.arguments?.getString(Routes.EDITOR_ARG)
            if (scriptId != null) {
                val context = LocalContext.current
                PrompterScreen(
                    scriptId = scriptId,
                    onExit = { navController.popBackStack() },
                    onStartOverlay = { script, speed, progress, onResult ->
                        var alreadySent = false
                        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
                            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                                if (alreadySent) return
                                alreadySent = true
                                when (resultCode) {
                                    OverlayService.RESULT_OVERLAY_STARTED -> onResult(true)
                                    OverlayService.RESULT_OVERLAY_FAILED -> onResult(false)
                                    else -> onResult(false)
                                }
                            }
                        }
                        try {
                            OverlayController.start(
                                context = context,
                                script = script,
                                speedDpPerSec = speed,
                                initialProgress = progress,
                                resultReceiver = receiver,
                            )
                        } catch (e: Exception) {
                            if (!alreadySent) {
                                alreadySent = true
                                onResult(false)
                            }
                        }
                    },
                )
            }
        }
    }
}
