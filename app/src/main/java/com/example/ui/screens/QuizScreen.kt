package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: StudyViewModel,
    onStartQuiz: (String, Long?, Int) -> Unit
) {
    val items by viewModel.studyItems.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedLimit by remember { mutableIntStateOf(10) }
    var selectedCategory by remember { mutableStateOf<Long?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val limits = listOf(5, 10, 15, 25, 50)

    val quizTypes = listOf(
        QuizModeItem(
            title = "Single Correct MCQ",
            desc = "Standard 4-option multiple choice generated automatically with smart distractors.",
            icon = Icons.Default.FactCheck,
            typeString = "Single Correct MCQ"
        ),
        QuizModeItem(
            title = "True / False Quick Fire",
            desc = "Fast, binary correct/incorrect matching format based on questions and definition statements.",
            icon = Icons.Default.Lightbulb,
            typeString = "True/False"
        ),
        QuizModeItem(
            title = "Weak Topics Boost",
            desc = "Double down on items you have previously answered incorrectly to build concrete retention.",
            icon = Icons.Default.TrendingDown,
            typeString = "Weak Topics Quiz",
            accentColor = MaterialTheme.colorScheme.error
        ),
        QuizModeItem(
            title = "Starred Items Quiz",
            desc = "Test your knowledge specifically of bookmarked and favorited flashcards.",
            icon = Icons.Default.Star,
            typeString = "Starred Questions Quiz",
            accentColor = Color(0xFFFFC107)
        ),
        QuizModeItem(
            title = "Important Items Quiz",
            desc = "Challenge yourself with cards flagged explicitly as highly important.",
            icon = Icons.Default.PriorityHigh,
            typeString = "Important Questions Quiz",
            accentColor = Color(0xFFE91E63)
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Smart Quiz Practice", 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // General Configuration Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Quiz Settings",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Limit Selection
                        Text(
                            text = "Number of Questions:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            limits.forEach { limit ->
                                FilterChip(
                                    selected = selectedLimit == limit,
                                    onClick = { selectedLimit = limit },
                                    label = { Text("$limit") },
                                    modifier = Modifier.testTag("quiz_limit_$limit"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Filter Dropdown
                        Text(
                            text = "Filter by Category (Optional):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedCatName = categories.find { it.id == selectedCategory }?.name ?: "All Categories (Mixed)"
                            OutlinedButton(
                                onClick = { categoryDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("quiz_category_filter"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedCatName, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown, 
                                        contentDescription = "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Categories (Mixed)") },
                                    onClick = {
                                        selectedCategory = null
                                        categoryDropdownExpanded = false
                                    }
                                )
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategory = cat.id
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quiz Modes Listing
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select Quiz Type to Begin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        text = "You don't have any study items yet. Head to the Datasets tab below to import or create questions!",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else {
                items(quizTypes) { quizMode ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_type_${quizMode.typeString.replace(" ", "_")}")
                            .clickable {
                                onStartQuiz(quizMode.typeString, selectedCategory, selectedLimit)
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background((quizMode.accentColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = quizMode.icon,
                                    contentDescription = quizMode.title,
                                    tint = quizMode.accentColor ?: MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quizMode.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = quizMode.desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

data class QuizModeItem(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val typeString: String,
    val accentColor: Color? = null
)
