package com.example.careai

import androidx.compose.ui.graphics.Color

data class TriageResult(
    val level: TriageLevel,
    val reason: String
)

enum class TriageLevel(val label: String, val color: Color) {
    LOW("Low", Color(0xFF2E7D32)),
    MODERATE("Moderate", Color(0xFFF9A825)),
    SEVERE("Severe", Color(0xFFE53935))
}

data class DiagnosticInput(
    val symptom: String,
    val profile: UserProfile,
    val triage: TriageResult,
    val visionSummary: String? = null,
    val videoSummary: String? = null,
    val labSummary: String? = null
)

data class UserProfile(
    val name: String,
    val age: String,
    val abhaId: String,
    val currentMeds: String,
    val allergies: String
)

data class DiagnosticOutput(
    val response: String,
    val recursiveQuestions: List<String>,
    val prescriptionPlan: String,
    val workoutPlan: String,
    val interactionCheck: String
)

data class AssistantUiState(
    val loading: Boolean = false,
    val response: String? = null,
    val recursiveQuestions: List<String> = emptyList(),
    val prescriptionPlan: String? = null,
    val workoutPlan: String? = null,
    val interactionCheck: String? = null,
    val error: String? = null
)

data class ChatMessage(
    val byUser: Boolean,
    val text: String
)

data class VitalReading(
    val timestamp: Long,
    val bloodPressureSys: Int,
    val bloodPressureDia: Int,
    val oxygenLevel: Int
)

data class DoctorItem(
    val name: String,
    val specialty: String,
    val distanceKm: Double,
    val phone: String? = null,
    val address: String? = null
)

enum class HomePage(val label: String, val emoji: String) {
    ASSISTANT("Assistant", "AI"),
    DISCOVER("Find", "🔍"),
    PLAN("Care Plan", "Rx"),
    VITALS("Vitals", "📊"),
    PROFILE("Profile", "Me")
}
