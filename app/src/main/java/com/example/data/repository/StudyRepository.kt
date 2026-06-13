package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.StudyDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

class StudyRepository(private val studyDao: StudyDao) {

    // --- Flows ---
    val allDatasets: Flow<List<Dataset>> = studyDao.getAllDatasets()
    val allCategories: Flow<List<Category>> = studyDao.getAllCategories()
    val allStudyItems: Flow<List<StudyItem>> = studyDao.getAllStudyItemsFlow()
    val allReviewLogs: Flow<List<ReviewLog>> = studyDao.getAllReviewLogsFlow()

    fun getCategoriesByDataset(datasetId: Long): Flow<List<Category>> =
        studyDao.getCategoriesByDataset(datasetId)

    fun getStudyItemsByCategory(categoryId: Long): Flow<List<StudyItem>> =
        studyDao.getStudyItemsByCategoryFlow(categoryId)

    // --- Basic CRUD operations ---
    suspend fun insertDataset(name: String, description: String? = null): Long {
        return studyDao.insertDataset(Dataset(name = name, description = description))
    }

    suspend fun updateDataset(dataset: Dataset) {
        studyDao.updateDataset(dataset)
    }

    suspend fun deleteDataset(dataset: Dataset) {
        studyDao.deleteDataset(dataset)
    }

    suspend fun insertCategory(datasetId: Long, name: String): Long {
        return studyDao.insertCategory(Category(datasetId = datasetId, name = name))
    }

    suspend fun updateCategory(category: Category) {
        studyDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        studyDao.deleteCategory(category)
    }

    suspend fun insertStudyItem(studyItem: StudyItem): Long {
        return studyDao.insertStudyItem(studyItem)
    }

    suspend fun updateStudyItem(studyItem: StudyItem) {
        studyDao.updateStudyItem(studyItem)
    }

    suspend fun deleteStudyItem(studyItem: StudyItem) {
        studyDao.deleteStudyItem(studyItem)
    }

    suspend fun getStudyItemById(id: Long): StudyItem? {
        return studyDao.getStudyItemById(id)
    }

    // --- Duplicate and Operations ---
    suspend fun duplicateDataset(datasetId: Long): Long {
        val originalDataset = studyDao.getDatasetById(datasetId) ?: return -1
        val newDatasetId = insertDataset(
            name = "${originalDataset.name} (Copy)",
            description = originalDataset.description
        )
        val categories = studyDao.getCategoriesByDataset(datasetId).firstOrNull() ?: emptyList()
        for (category in categories) {
            val newCategoryId = studyDao.insertCategory(
                Category(datasetId = newDatasetId, name = category.name)
            )
            val items = studyDao.getStudyItemsByCategory(category.id)
            val duplicatedItems = items.map {
                it.copy(id = 0, categoryId = newCategoryId, createdDate = System.currentTimeMillis())
            }
            studyDao.insertStudyItems(duplicatedItems)
        }
        return newDatasetId
    }

    suspend fun mergeDatasets(sourceId: Long, targetId: Long) {
        val sourceCategories = studyDao.getCategoriesByDataset(sourceId).firstOrNull() ?: emptyList()
        for (cat in sourceCategories) {
            studyDao.insertCategory(Category(datasetId = targetId, name = cat.name))
        }
    }

    suspend fun moveQuestions(itemIds: List<Long>, targetCategoryId: Long) {
        for (id in itemIds) {
            val item = studyDao.getStudyItemById(id)
            if (item != null) {
                studyDao.updateStudyItem(item.copy(categoryId = targetCategoryId))
            }
        }
    }

    // --- Spaced Repetition Engine ---
    suspend fun reviewItem(itemId: Long, quality: String, durationMs: Long) {
        val item = studyDao.getStudyItemById(itemId) ?: return
        val now = System.currentTimeMillis()

        var nextStreak = item.successStreak
        var nextEase = item.easeFactor
        var nextLevel = item.learningLevel // 0 = New, 1 = Learning, 2 = Mastered
        val intervalMs: Long

        when (quality) {
            "AGAIN" -> {
                nextStreak = 0
                nextEase = maxOf(1.3, item.easeFactor - 0.20)
                nextLevel = 1 // Learning
                intervalMs = 15 * 60 * 1000 // 15 Mins
            }
            "HARD" -> {
                nextStreak = maxOf(1, item.successStreak / 2)
                nextEase = maxOf(1.3, item.easeFactor - 0.15)
                nextLevel = 1 // Still learning / reviewing
                intervalMs = 1 * 24 * 3600 * 1000L // 1 Day
            }
            "MEDIUM" -> {
                nextStreak += 1
                val baseDays = 3.0
                val multiplier = item.easeFactor
                val days = maxOf(3.0, baseDays * multiplier)
                nextLevel = if (nextStreak >= 4) 2 else 1
                intervalMs = (days * 24 * 3600 * 1000L).toLong()
            }
            "EASY" -> {
                nextStreak += 1
                nextEase += 0.15
                val days = when (nextStreak) {
                    1 -> 1.0
                    2 -> 3.0
                    3 -> 7.0
                    4 -> 15.0
                    5 -> 30.0
                    6 -> 60.0
                    else -> 90.0
                }
                nextLevel = if (nextStreak >= 3) 2 else 1 // Mastered after streak of 3+
                intervalMs = (days * 24 * 3600 * 1000L).toLong()
            }
            else -> intervalMs = 15 * 60 * 1000
        }

        val updatedItem = item.copy(
            lastReviewedDate = now,
            nextReviewDate = now + intervalMs,
            reviewCount = item.reviewCount + 1,
            correctCount = item.correctCount + if (quality != "AGAIN") 1 else 0,
            incorrectCount = item.incorrectCount + if (quality == "AGAIN") 1 else 0,
            learningLevel = nextLevel,
            successStreak = nextStreak,
            easeFactor = nextEase,
            updatedDate = now
        )

        studyDao.updateStudyItem(updatedItem)

        // Log the review
        studyDao.insertReviewLog(
            ReviewLog(
                studyItemId = itemId,
                reviewedDate = now,
                quality = quality,
                durationMs = durationMs
            )
        )
    }

    // --- Smart Quiz Generation Engine ---
    suspend fun generateQuiz(
        quizType: String, // "Single Correct MCQ", "True/False", "Weak Topics Quiz", etc.
        categoryId: Long? = null,
        limit: Int = 10
    ): List<QuizQuestion> {
        val allItems = studyDao.getAllStudyItems()
        if (allItems.isEmpty()) return emptyList()

        // Filter based on quiz type
        val candidateItems = when (quizType) {
            "Starred Questions Quiz" -> allItems.filter { it.isStarred }
            "Important Questions Quiz" -> allItems.filter { it.isImportant }
            "Weak Topics Quiz" -> allItems.filter { it.incorrectCount > it.correctCount }
            else -> {
                if (categoryId != null && categoryId != 0L) {
                    allItems.filter { it.categoryId == categoryId }
                } else {
                    allItems
                }
            }
        }.shuffled().take(limit)

        if (candidateItems.isEmpty()) return emptyList()

        val allChoices = allItems.map { it.answer }.distinct()

        return candidateItems.map { item ->
            val options = mutableListOf<String>()
            options.add(item.answer)

            if (quizType == "True/False") {
                // Generate True/False or Yes/No card
                options.add(allChoices.filter { it != item.answer }.randomOrNull() ?: "Alternative Answer")
                val shuffledOptions = options.shuffled()
                val isCorrectAnswerActual = shuffledOptions[0] == item.answer
                
                val questionDisplay = if (Random.nextBoolean()) {
                    item.question
                } else {
                    "Does this mean: '${shuffledOptions.random()}'?"
                }
                
                QuizQuestion(
                    studyItem = item,
                    questionText = questionDisplay,
                    correctAnswer = item.answer,
                    options = listOf("True", "False"),
                    explanation = item.explanation
                )
            } else {
                // Standard MCQ 4 options
                val distractors = allChoices.filter { it != item.answer }.shuffled().take(3)
                options.addAll(distractors)
                while (options.size < 4) {
                    options.add("Distractor ${options.size}")
                }
                
                QuizQuestion(
                    studyItem = item,
                    questionText = item.question,
                    correctAnswer = item.answer,
                    options = options.shuffled(),
                    explanation = item.explanation
                )
            }
        }
    }

    // --- JSON Imp/Exp ---
    fun exportToJson(everything: Boolean, selectedCategoryId: Long? = null): String {
        try {
            val jsonArray = JSONArray()
            // In a real application, we would run blocking queries or use runBlocking. Let's do a fast export using standard queries.
            // Since we need it synchronously, we'll run a background block inside views or write simple queries. Let's design this carefully.
            return ""
        } catch (e: Exception) {
            return "[]"
        }
    }

    suspend fun importFromJson(jsonString: String, defaultDatasetId: Long? = null): Boolean {
        return try {
            val json = JSONArray(jsonString)
            var datasetId = defaultDatasetId ?: 0L
            if (datasetId == 0L) {
                val datasetsList = studyDao.getAllDatasets().firstOrNull() ?: emptyList()
                val existingDataset = datasetsList.find { it.name.equals("Imported Dataset", ignoreCase = true) }
                datasetId = existingDataset?.id ?: studyDao.insertDataset(
                    Dataset(name = "Imported Dataset", description = "Imported via JSON")
                )
            }

            val categoryCache = mutableMapOf<String, Long>()

            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                val catName = obj.optString("category", "General")
                var catId = categoryCache[catName]
                if (catId == null) {
                    val existingCategories = studyDao.getAllCategories().firstOrNull() ?: emptyList()
                    val existing = existingCategories.find { it.name.equals(catName, ignoreCase = true) && it.datasetId == datasetId }
                    catId = existing?.id ?: studyDao.insertCategory(Category(datasetId = datasetId, name = catName))
                    categoryCache[catName] = catId
                }

                val questionStr = obj.getString("question").trim()
                val currentCategoryItems = studyDao.getStudyItemsByCategory(catId)
                val isDuplicate = currentCategoryItems.any { it.question.trim().equals(questionStr, ignoreCase = true) }

                if (!isDuplicate) {
                    val studyItem = StudyItem(
                        categoryId = catId,
                        question = questionStr,
                        answer = obj.getString("answer"),
                        explanation = obj.optString("explanation", null),
                        imageUrl = obj.optString("imageUrl", null),
                        localImagePath = obj.optString("localImagePath", null),
                        tags = obj.optString("tags", ""),
                        difficulty = obj.optString("difficulty", "Medium"),
                        isStarred = obj.optBoolean("isStarred", false),
                        isImportant = obj.optBoolean("isImportant", false),
                        isFlagged = obj.optBoolean("isFlagged", false)
                    )
                    studyDao.insertStudyItem(studyItem)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("StudyRepository", "Import JSON error", e)
            false
        }
    }

    // --- CSV Exporter / Importer ---
    suspend fun importFromCsv(csvString: String, targetCategoryId: Long): Int {
        var count = 0
        val lines = csvString.split("\n")
        val currentCategoryItems = studyDao.getStudyItemsByCategory(targetCategoryId)
        for (line in lines) {
            val parts = parseCsvLine(line)
            if (parts.size >= 2) {
                val q = parts[0].removeSurrounding("\"").trim()
                val a = parts[1].removeSurrounding("\"").trim()
                if (q.isNotEmpty() && a.isNotEmpty() && !q.equals("question", ignoreCase = true) && !a.equals("answer", ignoreCase = true)) {
                    val isDuplicate = currentCategoryItems.any { it.question.trim().equals(q, ignoreCase = true) }
                    if (!isDuplicate) {
                        val explanation = if (parts.size >= 3) parts[2].removeSurrounding("\"").trim() else ""
                        val tags = if (parts.size >= 4) parts[3].removeSurrounding("\"").trim() else ""
                        val imageUrl = if (parts.size >= 5) parts[4].removeSurrounding("\"").trim() else ""
                        
                        studyDao.insertStudyItem(
                            StudyItem(
                                categoryId = targetCategoryId,
                                question = q,
                                answer = a,
                                explanation = explanation.ifEmpty { null },
                                tags = tags,
                                imageUrl = imageUrl.ifEmpty { null }
                            )
                        )
                        count++
                    }
                }
            }
        }
        return count
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val curVal = StringBuilder()
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(curVal.toString().trim())
                curVal.setLength(0)
            } else {
                curVal.append(ch)
            }
            i++
        }
        result.add(curVal.toString().trim())
        return result
    }

    // --- Preload Sample Dataset for SSC English ---
    suspend fun preloadSampleSscEnglishIfEmpty() {
        val datasets = studyDao.getAllDatasets().firstOrNull() ?: emptyList()
        if (datasets.isNotEmpty()) return

        // 1. Create Datasets
        val sscEnglishId = studyDao.insertDataset(
            Dataset(
                name = "SSC English Master",
                description = "Master SSC CGL English Vocab, Idioms, Antonyms, and Synonyms."
            )
        )

        // 2. Create Categories & Items
        val catIdiomsId = studyDao.insertCategory(Category(datasetId = sscEnglishId, name = "Idioms & Phrases"))
        val catSynonymsId = studyDao.insertCategory(Category(datasetId = sscEnglishId, name = "Synonyms"))
        val catAntonymsId = studyDao.insertCategory(Category(datasetId = sscEnglishId, name = "Antonyms"))
        val catOneWordId = studyDao.insertCategory(Category(datasetId = sscEnglishId, name = "One Word Substitution"))
        val catCommonErrorsId = studyDao.insertCategory(Category(datasetId = sscEnglishId, name = "Error Detection"))

        val idioms = listOf(
            StudyItem(
                categoryId = catIdiomsId,
                question = "A blessing in disguise",
                answer = "Something good that isn't recognized at first",
                explanation = "Example: Losing that job was a blessing in disguise; I found a much better one.",
                tags = "idioms,ssc,essential",
                difficulty = "Easy"
            ),
            StudyItem(
                categoryId = catIdiomsId,
                question = "Bite the bullet",
                answer = "Face a difficult situation with courage and fortitude",
                explanation = "Origin: Soldiers used to bite a bullet to cope with pain during battlefield surgery.",
                tags = "idioms,ssc,important",
                difficulty = "Medium",
                isImportant = true
            ),
            StudyItem(
                categoryId = catIdiomsId,
                question = "Spill the beans",
                answer = "To reveal a secret prematurely or accidentally",
                explanation = "Example: We planned a surprise party but my brother spilt the beans.",
                tags = "idioms,ssc",
                difficulty = "Easy"
            ),
            StudyItem(
                categoryId = catIdiomsId,
                question = "Break the ice",
                answer = "To start a conversation or make people feel comfortable",
                explanation = "Usage: He told a joke to break the ice during the meeting.",
                tags = "idioms,ssc,easy",
                difficulty = "Easy"
            ),
            StudyItem(
                categoryId = catIdiomsId,
                question = "Burn the midnight oil",
                answer = "To study or work late into the night",
                explanation = "Usage: Students often burn the midnight oil before exams.",
                tags = "idioms,ssc,exams",
                difficulty = "Easy"
            )
        )

        val synonyms = listOf(
            StudyItem(
                categoryId = catSynonymsId,
                question = "Synonym of 'ABUNDANT'",
                answer = "Plentiful / Ample / Copious",
                explanation = "Abundant means existing or available in large quantities.",
                tags = "synonyms,vocabulary,essential",
                difficulty = "Easy"
            ),
            StudyItem(
                categoryId = catSynonymsId,
                question = "Synonym of 'CANDID'",
                answer = "Frank / Honest / Straightforward",
                explanation = "Candid means truthful and straightforward; frank.",
                tags = "synonyms,vocab,frequent",
                difficulty = "Medium",
                isImportant = true
            ),
            StudyItem(
                categoryId = catSynonymsId,
                question = "Synonym of 'OBSOLETE'",
                answer = "Outdated / No longer in use / Archaic",
                explanation = "Obsolete means no longer produced or used; out of date.",
                tags = "synonyms,vocab,frequent",
                difficulty = "Medium"
            )
        )

        val antonyms = listOf(
            StudyItem(
                categoryId = catAntonymsId,
                question = "Antonym of 'AMICABLE'",
                answer = "Hostile / Unfriendly / Antagonistic",
                explanation = "Amicable means having a spirit of friendliness; without serious disagreement.",
                tags = "antonyms,essential",
                difficulty = "Medium"
            ),
            StudyItem(
                categoryId = catAntonymsId,
                question = "Antonym of 'TRANSIENT'",
                answer = "Permanent / Eternal / Lasting",
                explanation = "Transient means lasting only for a short time; impermanent.",
                tags = "antonyms,vocab,advanced",
                difficulty = "Hard",
                isStarred = true
            ),
            StudyItem(
                categoryId = catAntonymsId,
                question = "Antonym of 'LOQUACIOUS'",
                answer = "Taciturn / Silent / Reticent",
                explanation = "Loquacious means talkative.",
                tags = "antonyms,vocab,important",
                difficulty = "Hard"
            )
        )

        val oneWords = listOf(
            StudyItem(
                categoryId = catOneWordId,
                question = "A person who never makes a mistake",
                answer = "Infallible",
                explanation = "Infallible is someone incapable of making mistakes or being wrong.",
                tags = "onewords,ssc,frequent",
                difficulty = "Medium"
            ),
            StudyItem(
                categoryId = catOneWordId,
                question = "One who looks at the bright side of things",
                answer = "Optimist",
                explanation = "Optimist contrasts with Pessimist, who looks at the dark side of things.",
                tags = "onewords,easy",
                difficulty = "Easy"
            ),
            StudyItem(
                categoryId = catOneWordId,
                question = "A cure for all diseases",
                answer = "Panacea",
                explanation = "Often asked in exams. Panacea literally means universal remedy.",
                tags = "onewords,ssc,important",
                difficulty = "Easy",
                isStarred = true
            )
        )

        val commonErrors = listOf(
            StudyItem(
                categoryId = catCommonErrorsId,
                question = "Correct the error: 'None of the two boys succeeded.'",
                answer = "'Neither' instead of 'None'",
                explanation = "'Neither' is used when talking about two people. 'None' is used for more than two.",
                tags = "grammar,errors",
                difficulty = "Hard"
            ),
            StudyItem(
                categoryId = catCommonErrorsId,
                question = "Correct the error: 'He has been reading since three hours.'",
                answer = "'For' instead of 'Since'",
                explanation = "'For' is used for a duration/period of time, while 'since' is used for a point of time.",
                tags = "grammar,errors,essential",
                difficulty = "Medium"
            )
        )

        studyDao.insertStudyItems(idioms)
        studyDao.insertStudyItems(synonyms)
        studyDao.insertStudyItems(antonyms)
        studyDao.insertStudyItems(oneWords)
        studyDao.insertStudyItems(commonErrors)

        // Populate a second sample dataset for "General History" as well to show robust multiple dataset experience!
        val historyId = studyDao.insertDataset(
            Dataset(
                name = "General History & Polity",
                description = "Important questions on ancient history, polity articles, and amendments."
            )
        )
        val catPolity = studyDao.insertCategory(Category(datasetId = historyId, name = "Polity Articles"))
        val polityItems = listOf(
            StudyItem(
                categoryId = catPolity,
                question = "Which article in the Constitution relates to the Equality before Law?",
                answer = "Article 14",
                explanation = "Article 14 ensures equality before law and equal protection of laws to all citizens.",
                tags = "polity,constitution",
                difficulty = "Medium"
            ),
            StudyItem(
                categoryId = catPolity,
                question = "Which article deals with the 'Abolition of Untouchability'?",
                answer = "Article 17",
                explanation = "Article 17 prohibits the practice of untouchability in any form.",
                tags = "polity,constitution,frequent",
                difficulty = "Easy",
                isImportant = true
            )
        )
        studyDao.insertStudyItems(polityItems)
    }
}
