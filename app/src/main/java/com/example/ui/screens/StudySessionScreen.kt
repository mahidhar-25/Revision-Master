package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionScreen(
    viewModel: StudyViewModel,
    sessionType: String, // "FLASHCARD" or "QUIZ"
    onExitSession: () -> Unit
) {
    val items by viewModel.activeSessionItems.collectAsState()
    val currentIndex by viewModel.currentSessionIndex.collectAsState()
    val isRevealed by viewModel.isAnswerRevealed.collectAsState()

    val quizQuestions by viewModel.activeQuizQuestions.collectAsState()
    val quizIndex by viewModel.currentQuizIndex.collectAsState()
    val quizFinished by viewModel.quizFinished.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (sessionType == "FLASHCARD") "Flashcard Revision" else "Smart Practice Quiz",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExitSession, modifier = Modifier.testTag("session_exit_btn")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Session")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (sessionType == "FLASHCARD") {
                if (items.isEmpty()) {
                    SessionEmptyState("No study cards available in this filter range.", onExitSession)
                } else if (currentIndex >= items.size) {
                    SessionFinishState("Revision Session Complete!", "You have finished reviewing all cards in this list.", onExitSession)
                } else {
                    val currentCard = items[currentIndex]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Card ${currentIndex + 1} of ${items.size}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = (currentIndex + 1).toFloat() / items.size,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            IconButton(onClick = { viewModel.skipCurrentCard() }, modifier = Modifier.testTag("card_skip_btn")) {
                                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Skip")
                            }
                        }

                        // Central Flashcard Container
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 16.dp)
                                .testTag("active_flashcard_surface"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Question Text
                                item {
                                    Text(
                                        text = currentCard.difficulty.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (currentCard.difficulty == "Hard") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                (if (currentCard.difficulty == "Hard") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = currentCard.question,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().testTag("active_flashcard_question")
                                    )
                                }

                                // Hint Image loading if present
                                if (!currentCard.imageUrl.isNullOrEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(currentCard.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Image Hint",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                // Quick Bookmarking controls inside card
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { viewModel.toggleItemStar(currentCard) }, modifier = Modifier.testTag("flashcard_bookmark_star")) {
                                            Icon(
                                                imageVector = if (currentCard.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                contentDescription = "Star",
                                                tint = if (currentCard.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { viewModel.toggleItemImportant(currentCard) }, modifier = Modifier.testTag("flashcard_bookmark_important")) {
                                            Icon(
                                                imageVector = if (currentCard.isImportant) Icons.Filled.PriorityHigh else Icons.Outlined.PriorityHigh,
                                                contentDescription = "Important",
                                                tint = if (currentCard.isImportant) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { viewModel.toggleItemFlagged(currentCard) }, modifier = Modifier.testTag("flashcard_bookmark_flag")) {
                                            Icon(
                                                imageVector = if (currentCard.isFlagged) Icons.Filled.Flag else Icons.Outlined.Flag,
                                                contentDescription = "Flag",
                                                tint = if (currentCard.isFlagged) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Answer Details revealed
                                if (isRevealed) {
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                                        Text(
                                            text = "ANSWER DEFINITION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentCard.answer,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().testTag("active_flashcard_answer")
                                        )

                                        if (!currentCard.explanation.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "HINT & EXPLANATION",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = currentCard.explanation,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions: Show Answer or Spaced Repetition Buttons
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (!isRevealed) {
                                Button(
                                    onClick = { viewModel.revealCurrentAnswer() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("flashcard_reveal_answer_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Show Answer Definition", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "How difficult was this question?",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SrsActionButton(
                                            label = "Again",
                                            secLabel = "15m",
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.weight(1f).testTag("rate_again_btn"),
                                            onClick = { viewModel.submitCardReview("AGAIN") }
                                        )
                                        SrsActionButton(
                                            label = "Hard",
                                            secLabel = "1d",
                                            color = Color(0xFFFF9800),
                                            modifier = Modifier.weight(1f).testTag("rate_hard_btn"),
                                            onClick = { viewModel.submitCardReview("HARD") }
                                        )
                                        SrsActionButton(
                                            label = "Medium",
                                            secLabel = "3d",
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f).testTag("rate_medium_btn"),
                                            onClick = { viewModel.submitCardReview("MEDIUM") }
                                        )
                                        SrsActionButton(
                                            label = "Easy",
                                            secLabel = "7d",
                                            color = Color(0xFF4CAF50),
                                            modifier = Modifier.weight(1f).testTag("rate_easy_btn"),
                                            onClick = { viewModel.submitCardReview("EASY") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // QUIZ MODE
                if (quizQuestions.isEmpty()) {
                    SessionEmptyState("No matching quiz items generated.", onExitSession)
                } else if (quizFinished) {
                    val correct = quizQuestions.count { it.isCorrect == true }
                    SessionFinishState(
                        title = "Quiz Complete!",
                        desc = "You scored $correct out of ${quizQuestions.size} correct answers!",
                        onExitSession = onExitSession
                    )
                } else {
                    val currentQuestion = quizQuestions[quizIndex]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${quizIndex + 1} of ${quizQuestions.size}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = (quizIndex + 1).toFloat() / quizQuestions.size,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }

                        // Question Board
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 16.dp)
                                .testTag("active_quiz_question_surface"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentQuestion.questionText,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().testTag("active_quiz_question_text")
                                )

                                // Image Hint
                                if (!currentQuestion.studyItem.imageUrl.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "IMAGE HINT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(currentQuestion.studyItem.imageUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Quiz Image Hint",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }

                                if (currentQuestion.selectedOption != null && currentQuestion.explanation != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = currentQuestion.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Answer Options list
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            currentQuestion.options.forEachIndexed { optIdx, option ->
                                val optionSelected = currentQuestion.selectedOption == option
                                val isAnswered = currentQuestion.selectedOption != null
                                val isCorrect = option == currentQuestion.correctAnswer

                                val buttonColor = when {
                                    optionSelected && isCorrect -> Color(0xFF4CAF50) // selected + correct = green
                                    optionSelected && !isCorrect -> MaterialTheme.colorScheme.error // selected + incorrect = red
                                    isAnswered && isCorrect -> Color(0xFF4CAF50) // unselected but actual correct answer = green
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // default unselected
                                }

                                val textColor = when {
                                    isAnswered && (optionSelected || isCorrect) -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("quiz_option_$optIdx")
                                        .clickable(enabled = !isAnswered) {
                                            viewModel.answerQuizQuestion(option)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = buttonColor)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isAnswered && isCorrect) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = "Correct", tint = Color.White)
                                            } else if (optionSelected && !isCorrect) {
                                                Icon(imageVector = Icons.Default.Error, contentDescription = "Incorrect", tint = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Next Action
                            if (currentQuestion.selectedOption != null) {
                                Button(
                                    onClick = { viewModel.nextQuizQuestion() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("quiz_next_question_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Next Question", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SrsActionButton(
    label: String,
    secLabel: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Text(secLabel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        }
    }
}

@Composable
fun SessionEmptyState(desc: String, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Checklist,
            contentDescription = "Empty",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = desc,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onExit, modifier = Modifier.testTag("empty_state_exit_btn")) {
            Text("Go Back")
        }
    }
}

@Composable
fun SessionFinishState(title: String, desc: String, onExitSession: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Done",
            modifier = Modifier.size(72.dp),
            tint = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = desc,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onExitSession,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp)
                .testTag("completed_exit_btn")
        ) {
            Text("Return to Hub", fontWeight = FontWeight.Bold)
        }
    }
}
