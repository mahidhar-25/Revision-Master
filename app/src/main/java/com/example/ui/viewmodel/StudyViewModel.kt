package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.preference.UserPreferences
import com.example.data.repository.StudyRepository
import com.example.service.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StudyViewModel(
    application: Application,
    private val repository: StudyRepository,
    private val userPrefs: UserPreferences
) : AndroidViewModel(application) {

    private val notificationHelper = NotificationHelper(application)

    // --- Core Database Flows ---
    val datasets: StateFlow<List<Dataset>> = repository.allDatasets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyItems: StateFlow<List<StudyItem>> = repository.allStudyItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviewLogs: StateFlow<List<ReviewLog>> = repository.allReviewLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- User preferences State Flows ---
    val studyStreak = userPrefs.studyStreakFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val longestStreak = userPrefs.longestStreakFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val lastStudyDate = userPrefs.lastStudyDateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val totalStudyTimeMs = userPrefs.totalStudyTimeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val reminderHour = userPrefs.reminderHourFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 9)

    val reminderMinute = userPrefs.reminderMinuteFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val notificationsEnabled = userPrefs.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val darkModeEnabled = userPrefs.darkModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // --- Search & Filtering States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDifficultyFilter = MutableStateFlow<String?>(null)
    val selectedDifficultyFilter: StateFlow<String?> = _selectedDifficultyFilter.asStateFlow()

    private val _filterStarredOnly = MutableStateFlow(false)
    val filterStarredOnly: StateFlow<Boolean> = _filterStarredOnly.asStateFlow()

    private val _filterImportantOnly = MutableStateFlow(false)
    val filterImportantOnly: StateFlow<Boolean> = _filterImportantOnly.asStateFlow()

    private val _filterFlaggedOnly = MutableStateFlow(false)
    val filterFlaggedOnly: StateFlow<Boolean> = _filterFlaggedOnly.asStateFlow()

    private data class SearchFilters(
        val query: String,
        val difficulty: String?,
        val starred: Boolean,
        val important: Boolean,
        val flagged: Boolean
    )

    private val filterStateFlow = combine(
        _searchQuery,
        _selectedDifficultyFilter,
        _filterStarredOnly,
        _filterImportantOnly,
        _filterFlaggedOnly
    ) { query, diff, starred, important, flagged ->
        SearchFilters(query, diff, starred, important, flagged)
    }

    val filteredStudyItems: StateFlow<List<StudyItem>> = combine(
        studyItems,
        filterStateFlow
    ) { items, filters ->
        items.filter { item ->
            val matchesQuery = filters.query.isEmpty() ||
                    item.question.contains(filters.query, ignoreCase = true) ||
                    item.answer.contains(filters.query, ignoreCase = true) ||
                    item.tags.contains(filters.query, ignoreCase = true)

            val matchesDifficulty = filters.difficulty == null || item.difficulty.equals(filters.difficulty, ignoreCase = true)
            val matchesStarred = !filters.starred || item.isStarred
            val matchesImportant = !filters.important || item.isImportant
            val matchesFlagged = !filters.flagged || item.isFlagged

            matchesQuery && matchesDifficulty && matchesStarred && matchesImportant && matchesFlagged
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Study Session States ---
    private val _activeSessionItems = MutableStateFlow<List<StudyItem>>(emptyList())
    val activeSessionItems: StateFlow<List<StudyItem>> = _activeSessionItems.asStateFlow()

    private val _currentSessionIndex = MutableStateFlow(0)
    val currentSessionIndex: StateFlow<Int> = _currentSessionIndex.asStateFlow()

    private val _isAnswerRevealed = MutableStateFlow(false)
    val isAnswerRevealed: StateFlow<Boolean> = _isAnswerRevealed.asStateFlow()

    private var sessionStartTime: Long = 0L

    // --- Active Quiz Session States ---
    private val _activeQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val activeQuizQuestions: StateFlow<List<QuizQuestion>> = _activeQuizQuestions.asStateFlow()

    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex: StateFlow<Int> = _currentQuizIndex.asStateFlow()

    private val _quizFinished = MutableStateFlow(false)
    val quizFinished: StateFlow<Boolean> = _quizFinished.asStateFlow()

    init {
        // Create notifications channel and preload sample data on first launch
        viewModelScope.launch {
            notificationHelper.createNotificationChannel()
            repository.preloadSampleSscEnglishIfEmpty()
            updateStreakOnOpen()
        }
    }

    // --- Streak & Progress logic ---
    private suspend fun updateStreakOnOpen() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val lastDate = lastStudyDate.value

        if (lastDate.isEmpty()) {
            userPrefs.saveStreak(0)
        } else {
            val todayCal = Calendar.getInstance()
            val lastCal = Calendar.getInstance()
            try {
                sdf.parse(todayStr)?.let { todayCal.time = it }
                sdf.parse(lastDate)?.let { lastCal.time = it }
                
                val diffDays = ((todayCal.timeInMillis - lastCal.timeInMillis) / (24 * 3600 * 1000)).toInt()
                if (diffDays == 1) {
                    // Studied yesterday, streak continues when they review today
                } else if (diffDays > 1) {
                    // Broke the streak
                    userPrefs.saveStreak(0)
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }

    fun markStudiedNow() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val lastDate = lastStudyDate.value

            if (lastDate != todayStr) {
                val currentStreakVal = studyStreak.value
                userPrefs.saveStreak(currentStreakVal + 1)
                userPrefs.saveLastStudyDate(todayStr)
            }
        }
    }

    // --- Search & Filter Adjustments ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDifficultyFilter(difficulty: String?) {
        _selectedDifficultyFilter.value = difficulty
    }

    fun toggleStarredFilter() {
        _filterStarredOnly.value = !_filterStarredOnly.value
    }

    fun toggleImportantFilter() {
        _filterImportantOnly.value = !_filterImportantOnly.value
    }

    fun toggleFlaggedFilter() {
        _filterFlaggedOnly.value = !_filterFlaggedOnly.value
    }

    // --- Local Notifications Settings ---
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setNotificationsEnabled(enabled)
            if (enabled) {
                notificationHelper.sendReminderNotification(
                    "Revision Reminder Active",
                    "We will remind you to revise your daily flashcards and master your exams!"
                )
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPrefs.saveReminderTime(hour, minute)
            notificationHelper.sendReminderNotification(
                "Reminder Time Updated",
                "Your daily revision alarm is set to " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            )
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.toggleDarkMode(enabled)
        }
    }

    // --- Item Bookmark Actions ---
    fun toggleItemStar(item: StudyItem) {
        viewModelScope.launch {
            repository.updateStudyItem(item.copy(isStarred = !item.isStarred, updatedDate = System.currentTimeMillis()))
        }
    }

    fun toggleItemImportant(item: StudyItem) {
        viewModelScope.launch {
            repository.updateStudyItem(item.copy(isImportant = !item.isImportant, updatedDate = System.currentTimeMillis()))
        }
    }

    fun toggleItemFlagged(item: StudyItem) {
        viewModelScope.launch {
            repository.updateStudyItem(item.copy(isFlagged = !item.isFlagged, updatedDate = System.currentTimeMillis()))
        }
    }

    fun updateItemNotes(item: StudyItem, notes: String?) {
        viewModelScope.launch {
            repository.updateStudyItem(item.copy(notes = notes, updatedDate = System.currentTimeMillis()))
        }
    }

    // --- Dataset & Category CRUD triggers ---
    fun addDataset(name: String, description: String? = null) {
        viewModelScope.launch {
            repository.insertDataset(name, description)
        }
    }

    fun addCategory(datasetId: Long, name: String) {
        viewModelScope.launch {
            repository.insertCategory(datasetId, name)
        }
    }

    fun addStudyItem(
        categoryId: Long,
        question: String,
        answer: String,
        explanation: String? = null,
        imageUrl: String? = null,
        localImagePath: String? = null,
        tags: String = "",
        difficulty: String = "Medium"
    ) {
        viewModelScope.launch {
            repository.insertStudyItem(
                StudyItem(
                    categoryId = categoryId,
                    question = question,
                    answer = answer,
                    explanation = explanation,
                    imageUrl = imageUrl,
                    localImagePath = localImagePath,
                    tags = tags,
                    difficulty = difficulty
                )
            )
        }
    }

    fun editStudyItem(item: StudyItem) {
        viewModelScope.launch {
            repository.updateStudyItem(item.copy(updatedDate = System.currentTimeMillis()))
        }
    }

    fun deleteStudyItem(item: StudyItem) {
        viewModelScope.launch {
            repository.deleteStudyItem(item)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun deleteDataset(dataset: Dataset) {
        viewModelScope.launch {
            repository.deleteDataset(dataset)
        }
    }

    fun duplicateDataset(datasetId: Long) {
        viewModelScope.launch {
            repository.duplicateDataset(datasetId)
        }
    }

    fun mergeDataset(sourceId: Long, targetId: Long) {
        viewModelScope.launch {
            repository.mergeDatasets(sourceId, targetId)
        }
    }

    // --- Active Revision Practice Session ---
    fun startRevisionSession(mode: String, categoryId: Long? = null) {
        val allItems = studyItems.value
        val now = System.currentTimeMillis()

        val selectedItems = when (mode) {
            "Due Only", "Revision Only" -> allItems.filter {
                // Due items: nextReviewDate <= now OR nextReviewDate is null (never reviewed before!)
                it.nextReviewDate == null || it.nextReviewDate!! <= now
            }
            "Weak Areas" -> allItems.filter { it.incorrectCount > it.correctCount || it.difficulty == "Hard" }.sortedByDescending { it.incorrectCount }
            "Random Practice" -> allItems.shuffled()
            "Starred" -> allItems.filter { it.isStarred }
            "Important" -> allItems.filter { it.isImportant }
            "Flagged" -> allItems.filter { it.isFlagged }
            "Category" -> allItems.filter { it.categoryId == categoryId }
            else -> allItems // All cards
        }

        _activeSessionItems.value = selectedItems
        _currentSessionIndex.value = 0
        _isAnswerRevealed.value = false
        sessionStartTime = System.currentTimeMillis()
    }

    fun revealCurrentAnswer() {
        _isAnswerRevealed.value = true
    }

    fun submitCardReview(quality: String) {
        val currentItems = _activeSessionItems.value
        val currentIndex = _currentSessionIndex.value
        if (currentIndex < currentItems.size) {
            val item = currentItems[currentIndex]
            val duration = System.currentTimeMillis() - sessionStartTime

            viewModelScope.launch {
                repository.reviewItem(item.id, quality, duration)
                userPrefs.incrementStudyTime(duration)
                markStudiedNow()

                // Move to next card
                _isAnswerRevealed.value = false
                _currentSessionIndex.value = currentIndex + 1
                sessionStartTime = System.currentTimeMillis()

                // Check and post dynamic motivation alerts if master status achieved
                if (quality == "EASY" && item.successStreak >= 2) {
                    notificationHelper.sendReminderNotification("Keep it up!", "You mastered the card: '${item.question.take(20)}...'")
                }
            }
        }
    }

    fun skipCurrentCard() {
        val nextIndex = _currentSessionIndex.value + 1
        _isAnswerRevealed.value = false
        if (nextIndex <= _activeSessionItems.value.size) {
            _currentSessionIndex.value = nextIndex
            sessionStartTime = System.currentTimeMillis()
        }
    }

    // --- Active Quiz Sessions ---
    fun startQuizSession(quizType: String, categoryId: Long? = null, limit: Int = 10) {
        viewModelScope.launch {
            val questions = repository.generateQuiz(quizType, categoryId, limit)
            _activeQuizQuestions.value = questions
            _currentQuizIndex.value = 0
            _quizFinished.value = false
            sessionStartTime = System.currentTimeMillis()
        }
    }

    fun answerQuizQuestion(selectedOption: String) {
        val list = _activeQuizQuestions.value.toMutableList()
        val index = _currentQuizIndex.value
        if (index < list.size) {
            val q = list[index]
            val isCorrect = selectedOption == q.correctAnswer
            list[index] = q.copy(selectedOption = selectedOption, isCorrect = isCorrect)
            _activeQuizQuestions.value = list

            // Record duration
            val duration = System.currentTimeMillis() - sessionStartTime
            viewModelScope.launch {
                // Record the review in the database as a medium log if correct, or again if incorrect
                repository.reviewItem(
                    q.studyItem.id,
                    if (isCorrect) "MEDIUM" else "AGAIN",
                    duration
                )
                userPrefs.incrementStudyTime(duration)
                markStudiedNow()
            }
        }
    }

    fun nextQuizQuestion() {
        val nextIndex = _currentQuizIndex.value + 1
        if (nextIndex < _activeQuizQuestions.value.size) {
            _currentQuizIndex.value = nextIndex
            sessionStartTime = System.currentTimeMillis()
        } else {
            _quizFinished.value = true
            // Celebrate quiz accomplishment!
            val total = _activeQuizQuestions.value.size
            val correct = _activeQuizQuestions.value.count { it.isCorrect == true }
            notificationHelper.sendReminderNotification(
                "Quiz Completed!",
                "You scored $correct out of $total questions! Double down on weak areas."
            )
        }
    }

    // --- Import and Export Triggers ---
    fun handleJsonImport(jsonString: String, datasetId: Long?): Flow<Boolean> = flow {
        val success = repository.importFromJson(jsonString, datasetId)
        emit(success)
    }

    fun handleCsvImport(csvString: String, categoryId: Long): Flow<Int> = flow {
        val count = repository.importFromCsv(csvString, categoryId)
        emit(count)
    }

    // --- Complex Stats aggregation (Heatmap) ---
    val studyHeatmap: StateFlow<Map<String, Int>> = reviewLogs.map { logs ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        logs.groupBy {
            sdf.format(Date(it.reviewedDate))
        }.mapValues { (_, group) -> group.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}

class StudyViewModelFactory(
    private val application: Application,
    private val repository: StudyRepository,
    private val userPrefs: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            return StudyViewModel(application, repository, userPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
