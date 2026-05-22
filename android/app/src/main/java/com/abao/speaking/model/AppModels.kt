package com.abao.speaking.model

data class Scenario(
    val id: String,
    val title: String,
    val opening: String,
    val zh: String,
    val target: String,
    val next: String,
    val hints: List<String>
)

data class DialogueMessage(
    val role: String,
    val text: String,
    val sub: String = ""
)

data class ScoreResult(
    val values: Map<String, Int>,
    val total: Int,
    val wpm: Int,
    val coverage: Double
)
