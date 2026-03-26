package com.example.financemanager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel // Важный импорт для ViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.room.Room
import kotlinx.coroutines.delay
import com.example.financemanager.ui.theme.FinanceManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        setContent {
            var isDarkTheme by remember { mutableStateOf(sharedPreferences.getBoolean("isDarkTheme", false)) }
            val toggleTheme: (Boolean) -> Unit = { isDark ->
                isDarkTheme = isDark
                sharedPreferences.edit().putBoolean("isDarkTheme", isDark).apply()
            }

            val context = LocalContext.current
            val db = remember { Room.databaseBuilder(context, AppDatabase::class.java, "finance_db").build() }
            val dao = db.financeDao()

            // Создаем наш "Мозг" (ViewModel) один раз для всего приложения
            val viewModel: FinanceViewModel = viewModel()

            FinanceManagerTheme(darkTheme = isDarkTheme) {
                Surface {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "splash") {

                        composable("splash") {
                            SplashScreen(onNavigateToMain = {})
                            LaunchedEffect(Unit) {
                                delay(2000)
                                navController.navigate("main") { popUpTo("splash") { inclusive = true } }
                            }
                        }

                        composable("main") {
                            MainScreen(
                                dao = dao,
                                viewModel = viewModel, // Передаем ViewModel
                                onNavigateToDetails = { id -> navController.navigate("details/$id") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }

                        composable(
                            "details/{itemId}",
                            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("itemId") ?: 0
                            DetailsScreen(
                                itemId = id,
                                dao = dao,
                                viewModel = viewModel, // Передаем ViewModel
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                isDarkTheme = isDarkTheme,
                                onThemeChange = toggleTheme,
                                viewModel = viewModel, // Передаем ViewModel
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                    }
                }
            }
        }
    }
}