package com.minimal.today

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.minimal.today.data.TodoRepository
import com.minimal.today.model.TodoItem
import com.minimal.today.ui.screens.AddTodoScreen
import com.minimal.today.ui.screens.LogViewScreen
import com.minimal.today.ui.screens.TodayScreen
import com.minimal.today.ui.theme.TodayTheme
import com.minimal.today.util.IconHelper
import com.minimal.today.widget.WidgetHelper

enum class Screen {
    TODAY,
    ADD_TODO,
    LOG_VIEW
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: TodoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = TodoRepository(applicationContext)

        // Ensure dynamic date icon is synchronized for today
        IconHelper.updateDynamicIcon(applicationContext)

        val prefs = getSharedPreferences("today_prefs", Context.MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember {
                mutableStateOf(prefs.getBoolean("dark_theme", systemDark))
            }

            var currentScreen by remember { mutableStateOf(Screen.TODAY) }
            var todos by remember { mutableStateOf<List<TodoItem>>(emptyList()) }
            var logContent by remember { mutableStateOf("") }
            var filePath by remember { mutableStateOf("") }

            // Update status & navigation bar appearance on theme change
            LaunchedEffect(isDarkTheme) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
                WidgetHelper.updateAllWidgets(applicationContext)
            }

            // Load today's todos from local storage
            LaunchedEffect(Unit) {
                todos = repository.getTodayTodos()
                logContent = repository.getRawFileContent()
                filePath = repository.getFilePath()
                WidgetHelper.updateAllWidgets(applicationContext)
            }

            TodayTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        when (currentScreen) {
                            Screen.TODAY -> {
                                TodayScreen(
                                    todos = todos,
                                    isDarkTheme = isDarkTheme,
                                    onSetDoneStatus = { item, isDone ->
                                        val updated = todos.map {
                                            if (it.id == item.id) it.copy(isDone = isDone) else it
                                        }
                                        todos = updated
                                        repository.saveTodayTodos(updated)
                                        WidgetHelper.updateAllWidgets(applicationContext)
                                    },
                                    onDeleteTodo = { item ->
                                        val updated = todos.filter { it.id != item.id }
                                        todos = updated
                                        repository.saveTodayTodos(updated)
                                        WidgetHelper.updateAllWidgets(applicationContext)
                                    },
                                    onNavigateToAdd = {
                                        currentScreen = Screen.ADD_TODO
                                    },
                                    onNavigateToLog = {
                                        logContent = repository.getRawFileContent()
                                        filePath = repository.getFilePath()
                                        currentScreen = Screen.LOG_VIEW
                                    },
                                    onToggleTheme = { selectedDark ->
                                        isDarkTheme = selectedDark
                                        prefs.edit().putBoolean("dark_theme", selectedDark).apply()
                                        WidgetHelper.updateAllWidgets(applicationContext)
                                    },
                                    onClearCompleted = {
                                        val updated = todos.filter { !it.isDone }
                                        todos = updated
                                        repository.saveTodayTodos(updated)
                                        WidgetHelper.updateAllWidgets(applicationContext)
                                    }
                                )
                            }

                            Screen.ADD_TODO -> {
                                BackHandler {
                                    currentScreen = Screen.TODAY
                                }
                                AddTodoScreen(
                                    onAddTodo = { text, notes, startTime, endTime ->
                                        val newItem = TodoItem(
                                            text = text,
                                            notes = notes,
                                            startTime = startTime,
                                            endTime = endTime,
                                            isDone = false,
                                            date = repository.getTodayDateString()
                                        )
                                        val updated = todos + newItem
                                        todos = updated
                                        repository.saveTodayTodos(updated)
                                        WidgetHelper.updateAllWidgets(applicationContext)
                                        currentScreen = Screen.TODAY
                                    },
                                    onBack = {
                                        currentScreen = Screen.TODAY
                                    }
                                )
                            }

                            Screen.LOG_VIEW -> {
                                BackHandler {
                                    currentScreen = Screen.TODAY
                                }
                                LogViewScreen(
                                    fileContent = logContent,
                                    filePath = filePath,
                                    onBack = {
                                        currentScreen = Screen.TODAY
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        IconHelper.updateDynamicIcon(applicationContext)
    }
}
