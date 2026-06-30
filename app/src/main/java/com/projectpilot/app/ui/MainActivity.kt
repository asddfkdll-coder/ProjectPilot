package com.projectpilot.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.projectpilot.app.ui.screens.add.AddProjectScreen
import com.projectpilot.app.ui.screens.detail.ProjectDetailScreen
import com.projectpilot.app.ui.screens.git.GitScreen
import com.projectpilot.app.ui.screens.home.HomeScreen
import com.projectpilot.app.ui.screens.recipes.RecipesScreen
import com.projectpilot.app.ui.screens.settings.SettingsScreen
import com.projectpilot.app.ui.theme.ProjectPilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectPilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) { AppNav() }
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    const val GIT = "git/{id}"
    const val RECIPES = "recipes/{id}"
    fun detail(id: Long) = "detail/$id"
    fun git(id: Long) = "git/$id"
    fun recipes(id: Long) = "recipes/$id"
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAdd = { nav.navigate(Routes.ADD) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onOpen = { id -> nav.navigate(Routes.detail(id)) }
            )
        }
        composable(Routes.ADD) { AddProjectScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DETAIL) { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            ProjectDetailScreen(
                projectId = id,
                onBack = { nav.popBackStack() },
                onOpenGit = { nav.navigate(Routes.git(it)) },
                onOpenRecipes = { nav.navigate(Routes.recipes(it)) }
            )
        }
        composable(Routes.GIT) { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            GitScreen(projectId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.RECIPES) { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            RecipesScreen(projectId = id, onBack = { nav.popBackStack() })
        }
    }
}
