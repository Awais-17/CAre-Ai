package com.example.careai

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

enum class ReasoningProvider {
    GROK_PRIMARY,
    FUTURE_PROVIDER
}

private const val GROK_PRIMARY_FLAG = "GROK_PRIMARY"

data class GrokClinicalRequest(
    val model: String,
    val messages: List<GrokMessage>,
    val temperature: Double = 0.3
)

data class GrokMessage(
    val role: String,
    val content: String
)

data class GrokClinicalResponse(
    val choices: List<GrokChoice>?
)

data class GrokChoice(
    val message: GrokMessage?
)

data class AbdmInteractionRequest(
    val abhaId: String,
    val currentMeds: String,
    val allergies: String
)

data class AbdmInteractionResponse(
    val interactionSummary: String?
)

data class GeoapifyResponse(
    val features: List<GeoapifyFeature>?
)

data class GeoapifyFeature(
    val properties: GeoapifyProperties?
)

data class GeoapifyProperties(
    val name: String?,
    val street: String?,
    val city: String?,
    val contact: GeoapifyContact?,
    val distance: Double?,
    val categories: List<String>?,
    val address_line1: String?,
    val address_line2: String?
)

data class GeoapifyContact(
    val phone: String?
)

interface GrokDrugApiService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun clinicalPlan(@Body body: GrokClinicalRequest): GrokClinicalResponse
}

interface AbdmApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/interaction-check")
    suspend fun checkInteractions(@Body body: AbdmInteractionRequest): AbdmInteractionResponse
}

interface GeoapifyApiService {
    @GET("v2/places")
    suspend fun search(
        @Query("categories") categories: String,
        @Query("filter") filter: String,
        @Query("limit") limit: Int,
        @Query("apiKey") apiKey: String
    ): GeoapifyResponse
}

interface OverpassApiService {
    @GET("api/interpreter")
    suspend fun search(@Query("data") data: String): OverpassResponse
}

data class OverpassResponse(
    val elements: List<OverpassElement>?
)

data class OverpassElement(
    val id: Long?,
    val lat: Double?,
    val lon: Double?,
    val tags: Map<String, String>?
)

private class AuthHeaderInterceptor(
    private val apiKeyProvider: () -> String,
    private val staticHeaders: Map<String, String> = emptyMap()
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = apiKeyProvider().trim()
        val requestBuilder = chain.request().newBuilder()
        if (key.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $key")
            requestBuilder.addHeader("x-api-key", key)
        }
        staticHeaders.forEach { (name, value) ->
            requestBuilder.addHeader(name, value)
        }
        return chain.proceed(requestBuilder.build())
    }
}

object ApiModule {
    private fun httpClient(
        apiKeyProvider: () -> String,
        staticHeaders: Map<String, String> = emptyMap()
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(apiKeyProvider, staticHeaders))
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    private fun <T> retrofitService(baseUrl: String, client: OkHttpClient, clazz: Class<T>): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(clazz)
    }

    val gemmaApi: GrokDrugApiService = retrofitService(
        baseUrl = BuildConfig.GEMMA_BASE_URL,
        client = httpClient(
            apiKeyProvider = { BuildConfig.GEMMA_API_KEY },
            staticHeaders = mapOf(
                "HTTP-Referer" to "https://careai.local",
                "X-Title" to "CAre Ai"
            )
        ),
        clazz = GrokDrugApiService::class.java
    )

    val abdmApi: AbdmApiService = retrofitService(
        baseUrl = BuildConfig.ABDM_BASE_URL,
        client = httpClient(apiKeyProvider = { BuildConfig.ABDM_API_KEY }),
        clazz = AbdmApiService::class.java
    )

    val groqApi: GrokDrugApiService = retrofitService(
        baseUrl = BuildConfig.GROQ_BASE_URL,
        client = httpClient(apiKeyProvider = { BuildConfig.GROQ_API_KEY }),
        clazz = GrokDrugApiService::class.java
    )

    val geminiApi: GrokDrugApiService = retrofitService(
        baseUrl = BuildConfig.GEMINI_BASE_URL,
        client = httpClient(apiKeyProvider = { BuildConfig.GEMINI_API_KEY }),
        clazz = GrokDrugApiService::class.java
    )

    val geoapifyApi: GeoapifyApiService = retrofitService(
        baseUrl = "https://api.geoapify.com/",
        client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build(),
        clazz = GeoapifyApiService::class.java
    )

    val overpassApi: OverpassApiService = retrofitService(
        baseUrl = "https://overpass-api.de/",
        client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build(),
        clazz = OverpassApiService::class.java
    )
}

class CareAiRepository(
    private val groqApi: GrokDrugApiService,
    private val geminiApi: GrokDrugApiService,
    private val gemmaApi: GrokDrugApiService,
    private val abdmApi: AbdmApiService
) {
    suspend fun runDiagnostic(input: DiagnosticInput): DiagnosticOutput {
        val localQuestions = buildRecursiveQuestionsForEngine(input.symptom)
        val localInteraction = performCrossInteractionCheckForEngine(
            currentMeds = input.profile.currentMeds,
            allergies = input.profile.allergies,
            abhaId = input.profile.abhaId
        )
        val localDoctorReply = buildDoctorLikeResponseForEngine(input.symptom, localQuestions)

        var primaryGroqError: String? = null
        val response = runCatching {
            groqApi.clinicalPlan(
                GrokClinicalRequest(
                    model = BuildConfig.GROQ_MODEL,
                    messages = buildGrokMessages(
                        symptom = input.symptom,
                        triage = input.triage.level.label,
                        profile = input.profile,
                        recursiveQuestions = localQuestions
                    )
                )
            )
        }.onFailure { throwable ->
            primaryGroqError = throwable.message
        }.getOrNull()

        var geminiError: String? = null
        val geminiResponse = if (response == null) {
            runCatching {
                geminiApi.clinicalPlan(
                    GrokClinicalRequest(
                        model = "gemini-1.5-flash",
                        messages = buildGrokMessages(
                            symptom = input.symptom,
                            triage = input.triage.level.label,
                            profile = input.profile,
                            recursiveQuestions = localQuestions
                        )
                    )
                )
            }.onFailure { throwable ->
                geminiError = throwable.message
            }.getOrNull()
        } else null

        var gemmaError: String? = null
        val gemmaResponse = if (response == null && geminiResponse == null) {
            runCatching {
                gemmaApi.clinicalPlan(
                    GrokClinicalRequest(
                        model = BuildConfig.GEMMA_MODEL,
                        messages = buildGrokMessages(
                            symptom = input.symptom,
                            triage = input.triage.level.label,
                            profile = input.profile,
                            recursiveQuestions = localQuestions
                        )
                    )
                )
            }.onFailure { throwable ->
                gemmaError = throwable.message
            }.getOrNull()
        } else null

        val interaction = runCatching {
            if (input.profile.abhaId.isBlank()) null else {
                abdmApi.checkInteractions(
                    AbdmInteractionRequest(
                        abhaId = input.profile.abhaId,
                        currentMeds = input.profile.currentMeds,
                        allergies = input.profile.allergies
                    )
                ).interactionSummary?.takeIf { it.isNotBlank() }
            }
        }.getOrNull() ?: localInteraction

        val finalResponse = response ?: geminiResponse ?: gemmaResponse
        val rawContent = finalResponse?.choices?.firstOrNull()?.message?.content?.trim().orEmpty()
        
        val doctorReply = parseSection(rawContent, "Diagnosis")
            ?: parseSection(rawContent, "Assessment")
            ?: rawContent.ifBlank { localDoctorReply }

        val prescription = parseSection(rawContent, "Medicine")
            ?: parseSection(rawContent, "Prescription")
            ?: parseSection(rawContent, "Medication")
            ?: extractMedicineLine(rawContent)
            ?: localPrescriptionForEngine(input.symptom)

        val workout = parseSection(rawContent, "Workout")
            ?: parseSection(rawContent, "Recovery")
            ?: localWorkoutForEngine(input.symptom)

        val questions = parseQuestions(rawContent).ifEmpty { localQuestions }

        return DiagnosticOutput(
            response = doctorReply,
            recursiveQuestions = questions,
            prescriptionPlan = prescription,
            workoutPlan = workout,
            interactionCheck = interaction
        )
    }

    private fun buildGrokMessages(
        symptom: String,
        triage: String,
        profile: UserProfile,
        recursiveQuestions: List<String>
    ): List<GrokMessage> {
        val system = """
            You are a highly professional Indian medical triage assistant (AI Doctor).
            Return plain text with EXACTLY these sections (no conversational filler):
            Diagnosis: <A professional assessment of the symptoms>
            Medicine: <List specific OTC medicines with dosage, frequency, and duration for non-severe symptoms. Format: Name + Strength + Frequency + Duration. Example: Paracetamol 650mg, 3 times a day for 3 days after food.>
            Workout: <Recovery or physical guidance>
            Follow-up Questions: <Questions to narrow down the diagnosis>
            
            CRITICAL RULES:
            1. MANDATORY MEDICINE: For common non-severe issues (fever, cough, cold, pain, etc.), you MUST prescribe at least one standard OTC medicine. 
            2. DO NOT be overly cautious for mild cases. If it's a common cold/fever, give the medicine.
            3. SAFETY: If the triage is SEVERE, do NOT prescribe medicine; instead, write "EMERGENCY: Proceed to nearest hospital immediately." in the Medicine section.
            4. FORMAT: Keep each section on a new line. Use "Medicine:" label clearly.
            5. LIFESTYLE: Always provide some Workout/Recovery advice even if simple.
            6. VOICE-READY: Keep descriptions concise and speakable. Avoid long paragraphs.
        """.trimIndent()
        val user = """
            Patient Details:
            Name: ${profile.name}
            Age: ${profile.age}
            ABHA ID: ${profile.abhaId}
            Current Medications: ${profile.currentMeds}
            Allergies: ${profile.allergies}
            
            Current Issue:
            Symptom: $symptom
            Initial Triage Level: $triage
            Previous Questions Answered: ${recursiveQuestions.joinToString("; ")}
            
            Please provide the clinical plan.
        """.trimIndent()
        return listOf(
            GrokMessage(role = "system", content = system),
            GrokMessage(role = "user", content = user)
        )
    }
}

private fun parseSection(content: String, title: String): String? {
    val lines = content.lines()
    val start = lines.indexOfFirst { it.trim().startsWith("$title:", ignoreCase = true) }
    if (start == -1) return null
    val collected = mutableListOf<String>()
    val firstLineAfterTitle = lines[start].substringAfter(":", "").trim()
    if (firstLineAfterTitle.isNotBlank()) collected.add(firstLineAfterTitle)
    
    for (i in (start + 1) until lines.size) {
        val line = lines[i].trim()
        if (line.isBlank()) continue
        val isNextSection = line.contains(":") && !line.startsWith("-") && !line.startsWith("*") && 
                           (line.startsWith("Diagnosis", ignoreCase = true) || 
                            line.startsWith("Medicine", ignoreCase = true) || 
                            line.startsWith("Workout", ignoreCase = true) || 
                            line.startsWith("Follow-up", ignoreCase = true))
        if (isNextSection) break
        collected.add(line)
    }
    val result = collected.joinToString("\n").trim()
    return result.takeIf { it.isNotBlank() }
}

private fun parseQuestions(content: String): List<String> {
    val section = parseSection(content, "Follow-up Questions") ?: return emptyList()
    return section.split("?", "\n", ";")
        .map { it.trim().trimStart('-', '*', '•') }
        .filter { it.isNotBlank() }
        .map { if (it.endsWith("?")) it else "$it?" }
        .take(4)
}

private fun extractMedicineLine(content: String): String? {
    val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }
    val candidate = lines.firstOrNull {
        it.startsWith("Medicine", ignoreCase = true) ||
            it.startsWith("Prescription", ignoreCase = true) ||
            it.contains("mg", ignoreCase = true) ||
            it.contains("tablet", ignoreCase = true) ||
            it.contains("capsule", ignoreCase = true)
    }
    return candidate?.trimStart('-', '*', '•')?.trim()
}

private fun buildDoctorLikeResponseForEngine(
    firstSymptom: String,
    questions: List<String>
): String {
    val questionsText = if (questions.isEmpty()) {
        "No further immediate questions."
    } else {
        "Please answer next: ${questions.joinToString(" | ")}"
    }
    return "Based on '$firstSymptom', continue careful monitoring and follow this plan. $questionsText"
}

private fun buildRecursiveQuestionsForEngine(symptomText: String): List<String> {
    val text = symptomText.lowercase()
    return when {
        text.contains("fever") -> listOf(
            "Since when do you have fever?",
            "Highest measured temperature?",
            "Any cough, throat pain, or breathlessness?",
            "Any existing condition like diabetes?"
        )
        text.contains("headache") || text.contains("migraine") -> listOf(
            "Is the pain one-sided or whole head?",
            "Any vision blur, vomiting, or fainting?",
            "How long does each episode last?",
            "Did you take any painkiller already?"
        )
        text.contains("snake bite") -> listOf(
            "Where was the bite location?",
            "Is swelling increasing quickly?",
            "Any dizziness, vomiting, or breathing issue?",
            "Reach nearest emergency center immediately."
        )
        else -> listOf(
            "When did the symptom start?",
            "Pain severity out of 10?",
            "Any recent medicine or allergies?",
            "Any worsening since onset?"
        )
    }
}

private fun localPrescriptionForEngine(symptomText: String): String {
    val text = symptomText.lowercase()
    return when {
        text.contains("fever") -> "Medicine: Paracetamol. Support: ORS, hydration, and rest. Avoid self-antibiotics."
        text.contains("cough") -> "Steam inhalation, warm fluids, low-cost expectorant per label guidance."
        text.contains("pain") -> "Start with generic paracetamol only; avoid combining painkillers."
        else -> "Pocket-friendly first line: rest, hydration, symptom logging, and follow-up if worsening."
    }
}

private fun localWorkoutForEngine(symptomText: String): String {
    val text = symptomText.lowercase()
    return when {
        text.contains("cough") || text.contains("breath") -> "Breathing routine: 5 minutes diaphragmatic breathing x 3/day, avoid intense cardio."
        text.contains("back pain") || text.contains("neck pain") -> "Mobility routine: cat-cow, gentle hamstring stretch, 10-minute walk twice daily."
        text.contains("fever") -> "No workout during fever. Only light walking after temperature normal for 24h."
        else -> "Recovery routine: 20-minute light walk, hydration breaks, and sleep hygiene."
    }
}

private fun performCrossInteractionCheckForEngine(currentMeds: String, allergies: String, abhaId: String): String {
    if (abhaId.isBlank()) return "ABHA ID missing. Cannot complete history cross-check."
    val medsText = currentMeds.lowercase()
    val allergyText = allergies.lowercase()
    return when {
        medsText.contains("warfarin") -> "Caution: avoid unsupervised analgesic combinations with anticoagulants. Doctor review recommended."
        allergyText.contains("penicillin") -> "Allergy alert: avoid penicillin class without medical supervision."
        allergyText.contains("ibuprofen") -> "Allergy alert: avoid ibuprofen-containing brands."
        else -> "No immediate high-risk interaction detected from provided records. Continue monitored usage."
    }
}
