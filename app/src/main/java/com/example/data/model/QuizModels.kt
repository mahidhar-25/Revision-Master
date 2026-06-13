package com.example.data.model

data class QuizQuestion(
    val studyItem: StudyItem,
    val questionText: String,
    val correctAnswer: String,
    val options: List<String>,
    val explanation: String?,
    var selectedOption: String? = null,
    var isCorrect: Boolean? = null
)
