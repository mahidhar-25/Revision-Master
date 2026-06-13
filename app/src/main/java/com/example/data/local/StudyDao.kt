package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // --- Datasets ---
    @Query("SELECT * FROM datasets ORDER BY name ASC")
    fun getAllDatasets(): Flow<List<Dataset>>

    @Query("SELECT * FROM datasets WHERE id = :id LIMIT 1")
    suspend fun getDatasetById(id: Long): Dataset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataset(dataset: Dataset): Long

    @Update
    suspend fun updateDataset(dataset: Dataset)

    @Delete
    suspend fun deleteDataset(dataset: Dataset)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE datasetId = :datasetId ORDER BY name ASC")
    fun getCategoriesByDataset(datasetId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // --- Study Items ---
    @Query("SELECT * FROM study_items ORDER BY createdDate DESC")
    fun getAllStudyItemsFlow(): Flow<List<StudyItem>>

    @Query("SELECT * FROM study_items ORDER BY createdDate DESC")
    suspend fun getAllStudyItems(): List<StudyItem>

    @Query("SELECT * FROM study_items WHERE categoryId = :categoryId ORDER BY createdDate DESC")
    fun getStudyItemsByCategoryFlow(categoryId: Long): Flow<List<StudyItem>>

    @Query("SELECT * FROM study_items WHERE categoryId = :categoryId")
    suspend fun getStudyItemsByCategory(categoryId: Long): List<StudyItem>

    @Query("SELECT * FROM study_items WHERE id = :id LIMIT 1")
    suspend fun getStudyItemById(id: Long): StudyItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyItem(studyItem: StudyItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyItems(studyItems: List<StudyItem>)

    @Update
    suspend fun updateStudyItem(studyItem: StudyItem)

    @Delete
    suspend fun deleteStudyItem(studyItem: StudyItem)

    @Query("DELETE FROM study_items WHERE categoryId = :categoryId")
    suspend fun deleteStudyItemsByCategory(categoryId: Long)

    // --- Review Logs ---
    @Query("SELECT * FROM review_logs ORDER BY reviewedDate DESC")
    fun getAllReviewLogsFlow(): Flow<List<ReviewLog>>

    @Query("SELECT * FROM review_logs ORDER BY reviewedDate DESC")
    suspend fun getAllReviewLogs(): List<ReviewLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(reviewLog: ReviewLog): Long

    @Query("DELETE FROM review_logs")
    suspend fun clearReviewLogs()
}
