package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: StudyViewModel,
    onStartRevision: (String, Long?) -> Unit
) {
    val items by viewModel.studyItems.collectAsState()
    val now = System.currentTimeMillis()

    // Aggregate counts for modes
    val dueCount = items.count { it.nextReviewDate == null || it.nextReviewDate!! <= now }
    val weakCount = items.count { it.incorrectCount > it.correctCount }
    val starredCount = items.count { it.isStarred }
    val importantCount = items.count { it.isImportant }
    val flaggedCount = items.count { it.isFlagged }
    val randomCount = items.size

    val studyModes = listOf(
        StudyModeItem(
            name = "Due Only",
            desc = "Revised spaced repeat lists",
            count = dueCount,
            icon = Icons.Default.Restore,
            tag = "due_cards_selector"
        ),
        StudyModeItem(
            name = "Revision Only",
            desc = "Review previously studied",
            count = items.count { it.reviewCount > 0 },
            icon = Icons.Default.MenuBook,
            tag = "revision_cards_selector"
        ),
        StudyModeItem(
            name = "Weak Areas",
            desc = "Focus on incorrect answers",
            count = weakCount,
            icon = Icons.Default.TrendingDown,
            tag = "weak_cards_selector"
        ),
        StudyModeItem(
            name = "Random Practice",
            desc = "Vocabulary shuffle cards",
            count = randomCount,
            icon = Icons.Default.Shuffle,
            tag = "random_cards_selector"
        ),
        StudyModeItem(
            name = "Starred",
            desc = "Quick access favorites",
            count = starredCount,
            icon = Icons.Filled.Star,
            iconColor = Color(0xFFFFC107),
            tag = "starred_cards_selector"
        ),
        StudyModeItem(
            name = "Important",
            desc = "High-priority items",
            count = importantCount,
            icon = Icons.Default.PriorityHigh,
            iconColor = Color(0xFFE91E63),
            tag = "important_cards_selector"
        ),
        StudyModeItem(
            name = "Flagged",
            desc = "Marked for double-checking",
            count = flaggedCount,
            icon = Icons.Default.Flag,
            iconColor = Color(0xFF2196F3),
            tag = "flagged_cards_selector"
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Study & Revision Hub", 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Select a Revision Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(studyModes) { mode ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .testTag(mode.tag)
                            .clickable { onStartRevision(mode.name, null) },
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background((mode.iconColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = mode.icon,
                                        contentDescription = mode.name,
                                        tint = mode.iconColor ?: MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = mode.count.toString(),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column {
                                Text(
                                    text = mode.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mode.desc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class StudyModeItem(
    val name: String,
    val desc: String,
    val count: Int,
    val icon: ImageVector,
    val iconColor: Color? = null,
    val tag: String
)
