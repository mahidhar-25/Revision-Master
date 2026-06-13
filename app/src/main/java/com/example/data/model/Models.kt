package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "datasets")
data class Dataset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Dataset::class,
            parentColumns = ["id"],
            childColumns = ["datasetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["datasetId"])]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datasetId: Long,
    val name: String
)

@Entity(
    tableName = "study_items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class StudyItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val question: String,
    val answer: String,
    val explanation: String? = null,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val tags: String = "", // Comma-separated list of tags
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false,
    val isImportant: Boolean = false,
    val isFlagged: Boolean = false,
    val notes: String? = null,
    val lastReviewedDate: Long? = null,
    val nextReviewDate: Long? = null,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val learningLevel: Int = 0, // 0 = New, 1 = Learning, 2 = Mastered, 3 = Overdue (dynamically determined)
    val source: String? = null,
    val easeFactor: Double = 2.5,
    val successStreak: Int = 0
)

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = StudyItem::class,
            parentColumns = ["id"],
            childColumns = ["studyItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studyItemId"])]
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studyItemId: Long,
    val reviewedDate: Long = System.currentTimeMillis(),
    val quality: String, // "AGAIN", "HARD", "MEDIUM", "EASY"
    val durationMs: Long = 0
)
