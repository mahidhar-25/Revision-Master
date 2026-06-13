package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.ReviewLog
import com.example.data.model.StudyItem
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StudyViewModel) {
    val items by viewModel.studyItems.collectAsState()
    val logs by viewModel.reviewLogs.collectAsState()
    val heatmap by viewModel.studyHeatmap.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val totalCount = items.size
    val reviewedCount = items.count { it.reviewCount > 0 }
    val masteredCount = items.count { it.learningLevel == 2 }
    val learningCount = items.count { it.learningLevel == 1 }

    val avgAccuracy = remember(items) {
        val accurateCards = items.filter { it.reviewCount > 0 }
        if (accurateCards.isEmpty()) 0 else {
            val totalCorrect = accurateCards.sumOf { it.correctCount }
            val totalRev = accurateCards.sumOf { it.reviewCount }
            ((totalCorrect.toDouble() / totalRev) * 100).toInt()
        }
    }

    val retentionRate = remember(logs) {
        if (logs.isEmpty()) 0 else {
            val totalReviews = logs.size
            val successful = logs.count { it.quality != "AGAIN" }
            ((successful.toDouble() / totalReviews) * 100).toInt()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Progress & Analytics", 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // KPI Bento Row 1
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    KpiCard(
                        title = "QUESTIONS",
                        value = totalCount.toString(),
                        subtitle = "Total added",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "MASTERED",
                        value = masteredCount.toString(),
                        subtitle = "Streak maintained",
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF4CAF50)
                    )
                }
            }

            // KPI Bento Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    KpiCard(
                        title = "ACCURACY",
                        value = "$avgAccuracy%",
                        subtitle = "Overall correct",
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    KpiCard(
                        title = "RETENTION",
                        value = "$retentionRate%",
                        subtitle = "Spaced memory",
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF9C27B0)
                    )
                }
            }

            // GitHub Contribution Heatmap Card (Modern Bento Style)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stats_heatmap_panel"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Daily Practice Heatmap",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Visual study activities logged during previous weeks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        HeatmapGrid(heatmap)
                    }
                }
            }

            // Section Header: Categories
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Category Retention Levels",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (categories.isEmpty()) {
                item {
                    Text(
                        text = "No category datasets created yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
            } else {
                items(categories) { category ->
                    val categoryItems = items.filter { it.categoryId == category.id }
                    val accuracyVal = if (categoryItems.isEmpty()) 0 else {
                        val totalReviews = categoryItems.sumOf { it.reviewCount }
                        val totalCorrect = categoryItems.sumOf { it.correctCount }
                        if (totalReviews == 0) 0 else ((totalCorrect.toDouble() / totalReviews) * 100).toInt()
                    }

                    val progressColor = when {
                        accuracyVal < 50 -> MaterialTheme.colorScheme.error
                        accuracyVal < 80 -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$accuracyVal% accuracy",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = progressColor
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { accuracyVal / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = accentColor ?: MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HeatmapGrid(heatmap: Map<String, Int>) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Render 15 columns for the last 15 weeks
            for (col in 0 until 15) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until 7) {
                        val offsetPart = -((14 - col) * 7 + (6 - row))
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, offsetPart)
                        }

                        val dateKey = sdf.format(cal.time)
                        val count = heatmap[dateKey] ?: 0

                        val boxColor = when {
                            count == 0 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            count <= 2 -> Color(0xFFC6F6D5)
                            count <= 5 -> Color(0xFF9AE6B4)
                            count <= 9 -> Color(0xFF48BB78)
                            else -> Color(0xFF38A169)
                        }

                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(boxColor)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    Color(0xFFC6F6D5),
                    Color(0xFF9AE6B4),
                    Color(0xFF48BB78),
                    Color(0xFF38A169)
                ).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c)
                    )
                }
            }
            Text("More active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
