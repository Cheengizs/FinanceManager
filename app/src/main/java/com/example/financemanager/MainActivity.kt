package com.example.financemanager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.financemanager.data.local.AppDatabase
import com.example.financemanager.ui.screens.AuthScreen
import com.example.financemanager.ui.screens.DetailsScreen
import com.example.financemanager.ui.screens.MainScreen
import com.example.financemanager.ui.screens.SettingsScreen
import com.example.financemanager.ui.screens.SplashScreen
import com.example.financemanager.ui.theme.FinanceManagerTheme
import com.example.financemanager.viewmodel.FinanceViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        setContent {
            var isDarkTheme by rememberSaveable {
                mutableStateOf(
                    sharedPreferences.getBoolean(
                        "isDarkTheme", false
                    )
                )
            }
            val toggleTheme: (Boolean) -> Unit = { isDark ->
                isDarkTheme = isDark
                sharedPreferences.edit().putBoolean("isDarkTheme", isDark).apply()
            }

            val context = LocalContext.current

            val db = remember {
                Room.databaseBuilder(context, AppDatabase::class.java, "finance_db")
                    .fallbackToDestructiveMigration().build()
            }
            val dao = remember { db.financeDao() }
            val viewModel: FinanceViewModel = viewModel()

            LaunchedEffect(Unit) {
                scheduleDailyReminder(context)
            }

            FinanceManagerTheme(darkTheme = isDarkTheme) {
                Surface {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "splash") {

                        composable("splash") {
                            SplashScreen(
                                onNavigateToMain = {
                                    navController.navigate("main") {
                                        popUpTo("splash") {
                                            inclusive = true
                                        }
                                    }
                                },
                                onNavigateToAuth = {
                                    navController.navigate("auth") {
                                        popUpTo("splash") {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }

                        composable("auth") {
                            AuthScreen(
                                onAuthSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("auth") {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }

                        composable("main") {
                            MainScreen(
                                dao = dao,
                                viewModel = viewModel,
                                onNavigateToDetails = { id -> navController.navigate("details/$id") },
                                onNavigateToSettings = { navController.navigate("settings") })
                        }

                        composable(
                            "details/{itemId}",
                            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("itemId") ?: ""
                            DetailsScreen(
                                itemId = id,
                                dao = dao,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() })
                        }

                        composable("settings") {
                            SettingsScreen(
                                isDarkTheme = isDarkTheme,
                                onThemeChange = toggleTheme,
                                viewModel = viewModel,
                                dao = dao,
                                onNavigateBack = { navController.popBackStack() },
                                onLogout = {
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun scheduleDailyReminder(context: Context) {
    val testWorkRequest =
        OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(10, TimeUnit.SECONDS).build()
    WorkManager.getInstance(context).enqueue(testWorkRequest)

    val dailyWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "DailyFinanceReminder", ExistingPeriodicWorkPolicy.KEEP, dailyWorkRequest
    )
}

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val context = applicationContext
        val notificationManager = NotificationManagerCompat.from(context)

        val channelId = "finance_channel_high_priority"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.notification_channel_name)
            val channelDesc = context.getString(R.string.notification_channel_desc)

            val channel = android.app.NotificationChannel(
                channelId,
                channelName,
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
            }
            notificationManager.createNotificationChannel(channel)
        }

        val titleText = context.getString(R.string.notification_title)
        val contentText = context.getString(R.string.notification_text)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        try {
            notificationManager.notify(1, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}