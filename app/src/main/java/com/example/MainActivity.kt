package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.preference.UserPreferences
import com.example.data.repository.StudyRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudyViewModel
import com.example.ui.viewmodel.StudyViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manual Dependency Injection Setup
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = StudyRepository(database.studyDao())
        val userPrefs = UserPreferences(applicationContext)
        val viewModelFactory = StudyViewModelFactory(application, repository, userPrefs)

        setContent {
            val viewModel: StudyViewModel = viewModels<StudyViewModel> { viewModelFactory }.value
            val isDarkTheme by viewModel.darkModeEnabled.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                AppMainShell(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AppMainShell(viewModel: StudyViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeSessionType by remember { mutableStateOf<String?>(null) } // null, "FLASHCARD", or "QUIZ"

    if (activeSessionType != null) {
        // Fullscreen study practicing mode overlay
        StudySessionScreen(
            viewModel = viewModel,
            sessionType = activeSessionType!!,
            onExitSession = { activeSessionType = null }
        )
    } else {
        // Default App Workspace Shell
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("app_navigation_bar"),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_item_home")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.Class else Icons.Outlined.Class,
                                contentDescription = "Review"
                            )
                        },
                        label = { Text("Review", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_item_review")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.FactCheck else Icons.Outlined.FactCheck,
                                contentDescription = "Quiz"
                            )
                        },
                        label = { Text("Quiz", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_item_quiz")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.FolderZip else Icons.Outlined.FolderZip,
                                contentDescription = "Datasets"
                            )
                        },
                        label = { Text("Datasets", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_item_datasets")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 4) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                contentDescription = "Stats"
                            )
                        },
                        label = { Text("Stats", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_item_stats")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onStartRevision = { mode, categoryId ->
                            viewModel.startRevisionSession(mode, categoryId)
                            activeSessionType = "FLASHCARD"
                        },
                        onNavigateToStats = { selectedTab = 4 }
                    )
                    1 -> ReviewScreen(
                        viewModel = viewModel,
                        onStartRevision = { mode, categoryId ->
                            viewModel.startRevisionSession(mode, categoryId)
                            activeSessionType = "FLASHCARD"
                        }
                    )
                    2 -> QuizScreen(
                        viewModel = viewModel,
                        onStartQuiz = { quizType, categoryId, limit ->
                            viewModel.startQuizSession(quizType, categoryId, limit)
                            activeSessionType = "QUIZ"
                        }
                    )
                    3 -> DatasetScreen(viewModel = viewModel)
                    4 -> StatsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
