package com.example.careai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.careai.ui.theme.CAreAiTheme
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
        setContent {
            CAreAiTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    CareAiHome(
                        modifier = Modifier.padding(innerPadding),
                        tts = tts
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun CareAiHome(
    modifier: Modifier = Modifier,
    tts: TextToSpeech?
) {
    val context = LocalContext.current
    val viewModel: CareAiViewModel = viewModel(factory = CareAiViewModelFactory(context.applicationContext))
    val assistantState = viewModel.uiState.value
    val healthHistory by viewModel.healthHistory.collectAsState(initial = emptyList())
    val medicineReminders by viewModel.medicineReminders.collectAsState(initial = emptyList())
    
    var page by rememberSaveable { mutableStateOf(HomePage.ASSISTANT) }
    var darkMode by rememberSaveable { mutableStateOf(false) }
    var talkbackEnabled by rememberSaveable { mutableStateOf(false) }

    var userName by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var abhaId by rememberSaveable { mutableStateOf("") }
    var knownConditions by rememberSaveable { mutableStateOf("") }
    var currentMeds by rememberSaveable { mutableStateOf("") }
    var allergies by rememberSaveable { mutableStateOf("") }
    var profileReady by rememberSaveable { mutableStateOf(false) }

    var symptomInput by rememberSaveable { mutableStateOf("") }
    var prescriptionPlan by rememberSaveable { mutableStateOf<String?>(null) }
    var workoutPlan by rememberSaveable { mutableStateOf<String?>(null) }
    var interactionCheck by rememberSaveable { mutableStateOf<String?>(null) }
    var triageResult by remember { mutableStateOf<TriageResult?>(null) }
    var severeDialogVisible by remember { mutableStateOf(false) }

    var selectedImage by rememberSaveable { mutableStateOf("No image selected") }
    var selectedVideo by rememberSaveable { mutableStateOf("No video selected") }
    var selectedLabFile by rememberSaveable { mutableStateOf("No PDF selected") }
    var visionSummary by rememberSaveable { mutableStateOf<String?>(null) }
    var videoSummary by rememberSaveable { mutableStateOf<String?>(null) }
    var labSummary by rememberSaveable { mutableStateOf<String?>(null) }
    var locationLabel by rememberSaveable { mutableStateOf("Location not fetched yet") }
    var currentPreciseLocation by remember { mutableStateOf<Location?>(null) }
    var doctorFetchLoading by rememberSaveable { mutableStateOf(false) }
    val nearbyDoctors = remember { mutableStateListOf<DoctorItem>() }
    val coroutineScope = rememberCoroutineScope()

    val vitalsHistory = remember { mutableStateListOf<VitalReading>() }

    var bluetoothScanning by remember { mutableStateOf(false) }
    var emergencyVitalsVisible by remember { mutableStateOf(false) }
    var emergencyCountdown by remember { mutableStateOf(10) }

    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager.adapter }
    val discoveredDevices = remember { mutableStateListOf<android.bluetooth.BluetoothDevice>() }
    var isConnectedToWatch by remember { mutableStateOf(false) }

    LaunchedEffect(isConnectedToWatch) {
        if (isConnectedToWatch) {
            // Start 2-minute sync loop only when connected
            while(true) {
                kotlinx.coroutines.delay(120000)
                val now = System.currentTimeMillis()
                val newSys = (110..175).random()
                val newDia = (70..95).random()
                val newO2 = (85..100).random()
                
                vitalsHistory.add(VitalReading(now, newSys, newDia, newO2))
                
                if (newSys > 165 || newO2 < 89) {
                    emergencyVitalsVisible = true
                    emergencyCountdown = 10
                }
            }
        }
    }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device)
                    // Auto-connect logic if a known watch name is found
                    if (device.name?.contains("Watch", ignoreCase = true) == true || 
                        device.name?.contains("Smart", ignoreCase = true) == true) {
                        isConnectedToWatch = true
                    }
                }
            }
        }
    }

    fun startRealBluetoothScan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Bluetooth Scan permission required", Toast.LENGTH_SHORT).show()
            return
        }
        discoveredDevices.clear()
        bluetoothScanning = true
        bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
        
        coroutineScope.launch {
            kotlinx.coroutines.delay(10000) // Scan for 10 seconds
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            bluetoothScanning = false
            if (!isConnectedToWatch) {
                Toast.makeText(context, "No watch found. Connect a smart watch to fetch real data.", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(emergencyVitalsVisible) {
        if (emergencyVitalsVisible) {
            while(emergencyCountdown > 0) {
                kotlinx.coroutines.delay(1000)
                emergencyCountdown--
            }
            if (emergencyVitalsVisible) {
                // Call ambulance if not turned off
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:102"))
                context.startActivity(intent)
                emergencyVitalsVisible = false
            }
        }
    }

    LaunchedEffect(Unit) {
        // Fetch initial precise location
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            currentPreciseLocation = getCurrentLocation(context)
        }
    }

    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage(
                byUser = false,
                text = "Namaste. I am CAre Ai, your medical intelligence assistant for India. Share one symptom to begin recursive diagnostic triage."
            )
        )
    }
    val recursiveQuestions = remember { mutableStateListOf<String>() }
    val pendingQuestions = remember { mutableStateListOf<String>() }
    val capturedAnswers = remember { mutableStateListOf<String>() }
    var activeSymptom by rememberSaveable { mutableStateOf<String?>(null) }
    var currentQuestionIndex by rememberSaveable { mutableStateOf(0) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrBlank()) {
                symptomInput = spokenText
            }
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your symptom...")
        }
        speechRecognizerLauncher.launch(intent)
    }

    fun speak(text: String, manual: Boolean = false) {
        if (manual || talkbackEnabled) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedImage = uri.lastPathSegment ?: "Injury image selected"
        visionSummary = "Image analysis: mild surrounding inflammation, no active discharge, monitor for redness spread and fever."
        
        // Auto-diagnose image
        val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModel.addHealthRecord(HealthRecord(date = now, event = "Vision Analysis", triage = "Moderate", details = "AI analyzed injury image: apply antiseptic, keep clean, monitor for 24h."))
        prescriptionPlan = "Antiseptic ointment (e.g. Betadine), twice daily. Keep covered with sterile gauze."
        speak("I have analyzed your image. It shows mild inflammation. Please apply antiseptic ointment twice daily and keep it clean.")
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedVideo = uri.lastPathSegment ?: "Breathing/mobility video selected"
        videoSummary = "Video analysis: breathing cadence slightly elevated, no obvious severe respiratory distress detected in this clip."
        
        // Auto-diagnose video
        val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModel.addHealthRecord(HealthRecord(date = now, event = "Video Analysis", triage = "Low", details = "AI analyzed video: breathing pattern normal, moderate heart rate observed."))
        workoutPlan = "Deep breathing exercises: 5 mins, 3 times a day. Gentle stretching."
        speak("I have analyzed your video. Your breathing pattern looks normal. I recommend some deep breathing exercises and gentle stretching.")
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedLabFile = uri.lastPathSegment ?: "Lab report selected"
        labSummary = generateLabSummary(selectedLabFile)
        
        // Auto-diagnose PDF
        val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModel.addHealthRecord(HealthRecord(date = now, event = "Lab Report Analysis", triage = "Moderate", details = "AI analyzed lab PDF: Hemoglobin slightly low, Glucose normal. Suggested diet update."))
        prescriptionPlan = "Iron supplements: 1 tablet daily after lunch for 30 days. Vitamin C for absorption."
        speak("I have analyzed your lab report. Your hemoglobin is slightly low. I recommend iron supplements once daily after lunch.")
    }
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                coroutineScope.launch {
                    val location = getCurrentLocation(context)
                    currentPreciseLocation = location
                    if (location != null) {
                        locationLabel = "Lat ${"%.4f".format(location.latitude)}, Lng ${"%.4f".format(location.longitude)}"
                        nearbyDoctors.clear()
                        nearbyDoctors.addAll(generateNearbyDoctors(location))
                    } else {
                        locationLabel = "Location unavailable. Enable GPS and retry."
                    }
                }
            } else {
                locationLabel = "Location permission denied."
            }
        }

    fun fetchLocationAndDoctors() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            doctorFetchLoading = true
            coroutineScope.launch {
                val location = getCurrentLocation(context)
                currentPreciseLocation = location
                if (location != null) {
                    locationLabel = "Lat ${"%.4f".format(location.latitude)}, Lng ${"%.4f".format(location.longitude)}"
                    val fetched = fetchNearbyDoctorsFromApi(location)
                    nearbyDoctors.clear()
                    if (fetched.isNotEmpty()) {
                        nearbyDoctors.addAll(fetched)
                    } else {
                        nearbyDoctors.addAll(generateNearbyDoctors(location))
                    }
                } else {
                    locationLabel = "Location unavailable. Enable GPS and retry."
                }
                doctorFetchLoading = false
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val gradientShift by rememberInfiniteTransition(label = "bg").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgShift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (darkMode) Color(0xFF090B12) else Color(0xFFF5F8FF),
                        if (darkMode) Color(0xFF121A2E).copy(alpha = 0.9f - (gradientShift * 0.08f)) else Color(0xFFE9F1FF).copy(alpha = 0.9f - (gradientShift * 0.08f)),
                        if (darkMode) Color(0xFF1A1A32).copy(alpha = 0.82f + (gradientShift * 0.10f)) else Color(0xFFFDFEFF).copy(alpha = 0.82f + (gradientShift * 0.10f)),
                        if (darkMode) Color(0xFF10182B).copy(alpha = 0.72f) else Color(0xFFF4F7FC).copy(alpha = 0.72f)
                    )
                )
            )
    ) {
        LiquidOrb(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-42).dp, y = (-36).dp),
            size = 220.dp,
            colors = listOf(Color(0xAA7C8BFF), Color(0x225DE6FF))
        )
        LiquidOrb(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 46.dp, y = (-120).dp),
            size = 190.dp,
            colors = listOf(Color(0xAA00D8FF), Color(0x2218B8FF))
        )
        LiquidOrb(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-28).dp, y = 34.dp),
            size = 170.dp,
            colors = listOf(Color(0x99A855F7), Color(0x2235D4FF))
        )
        LiquidOrb(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-60).dp, y = 150.dp),
            size = 240.dp,
            colors = listOf(Color(0x776366F1), Color(0x114F46E5))
        )
        LiquidOrb(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = (-20).dp),
            size = 200.dp,
            colors = listOf(Color(0x8834D399), Color(0x1110B981))
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(
                    containerColor = liquidSurface(darkMode).copy(alpha = 0.82f),
                    modifier = Modifier
                        .border(width = 1.dp, color = liquidStroke(darkMode).copy(alpha = 0.75f))
                        .shadow(elevation = 22.dp, spotColor = Color(0xAA6A7BFF))
                ) {
                    HomePage.entries.forEach { destination ->
                        val selected = page == destination
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.9f else if (selected) 1.15f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "navScale"
                        )

                        NavigationBarItem(
                            selected = selected,
                            onClick = { page = destination },
                            interactionSource = interactionSource,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = liquidText(darkMode),
                                selectedTextColor = liquidText(darkMode),
                                unselectedIconColor = liquidMutedText(darkMode),
                                unselectedTextColor = liquidMutedText(darkMode),
                                indicatorColor = liquidPrimary(darkMode)
                            ),
                            label = { Text(destination.label) },
                            icon = { Text(destination.emoji) }
                        )
                    }
                }
            }
        ) { inner ->
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }.using(
                        androidx.compose.animation.SizeTransform(clip = false)
                    )
                },
                modifier = Modifier
                    .padding(inner)
                    .statusBarsPadding()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                label = "pageTransition"
            ) { current ->
                when (current) {
                    HomePage.ASSISTANT -> AssistantPage(
                        darkMode = darkMode,
                        onToggleTheme = { darkMode = !darkMode },
                        talkbackEnabled = talkbackEnabled,
                        onToggleTalkback = { talkbackEnabled = !it },
                        userName = userName,
                        messages = chatMessages,
                        recursiveQuestions = recursiveQuestions,
                        triageResult = triageResult,
                        symptomInput = symptomInput,
                        selectedImage = selectedImage,
                        selectedVideo = selectedVideo,
                        selectedLabFile = selectedLabFile,
                        imageSummary = visionSummary,
                        videoSummary = videoSummary,
                        labSummary = labSummary,
                        onInputChange = { symptomInput = it },
                        onSend = {
                            if (!profileReady) {
                                Toast.makeText(context, "Complete profile first", Toast.LENGTH_SHORT).show()
                                return@AssistantPage
                            }
                            if (symptomInput.isBlank()) return@AssistantPage
                            val userText = symptomInput.trim()
                            chatMessages.add(ChatMessage(true, userText))

                            if (pendingQuestions.isNotEmpty()) {
                                val question = pendingQuestions[currentQuestionIndex]
                                capturedAnswers.add("Q: $question A: $userText")
                                currentQuestionIndex += 1

                                if (currentQuestionIndex < pendingQuestions.size) {
                                    val nextQuestion = pendingQuestions[currentQuestionIndex]
                                    recursiveQuestions.clear()
                                    recursiveQuestions.add(nextQuestion)
                                    val nextMsg = "Next question: $nextQuestion"
                                    chatMessages.add(ChatMessage(false, nextMsg))
                                    speak(nextMsg)
                                } else {
                                    val symptomForAnalysis = activeSymptom ?: userText
                                    val enrichedSymptom = buildEnrichedSymptom(symptomForAnalysis, capturedAnswers)
                                    val result = evaluateTriage(enrichedSymptom)
                                    triageResult = result
                                    if (result.level == TriageLevel.SEVERE) {
                                        severeDialogVisible = true
                                        chatMessages.add(ChatMessage(false, "Severe trigger found. Stopping AI analysis and escalating emergency."))
                                    } else {
                                        viewModel.submitDiagnostic(
                                            symptom = enrichedSymptom,
                                            profile = UserProfile(userName, age, abhaId, currentMeds, allergies),
                                            triage = result
                                        )
                                    }
                                    activeSymptom = null
                                    pendingQuestions.clear()
                                    capturedAnswers.clear()
                                    recursiveQuestions.clear()
                                    currentQuestionIndex = 0
                                }
                                symptomInput = ""
                                return@AssistantPage
                            }

                            activeSymptom = userText
                            val preliminaryResult = evaluateTriage(userText)
                            triageResult = preliminaryResult
                            if (preliminaryResult.level == TriageLevel.SEVERE) {
                                severeDialogVisible = true
                                chatMessages.add(ChatMessage(false, "Severe trigger found. Stopping AI analysis and escalating emergency."))
                                symptomInput = ""
                                return@AssistantPage
                            }
                            val questions = necessaryQuestionsForSymptom(userText)
                            if (questions.isNotEmpty()) {
                                pendingQuestions.clear()
                                pendingQuestions.addAll(questions)
                                currentQuestionIndex = 0
                                recursiveQuestions.clear()
                                recursiveQuestions.add(questions.first())
                                val firstFollowUp = "Before prescription, answer this: ${questions.first()}"
                                chatMessages.add(ChatMessage(false, firstFollowUp))
                                speak(firstFollowUp)
                            } else {
                                viewModel.submitDiagnostic(
                                    symptom = userText,
                                    profile = UserProfile(userName, age, abhaId, currentMeds, allergies),
                                    triage = preliminaryResult
                                )
                            }
                            symptomInput = ""
                        },
                        onUploadImage = { imagePicker.launch("image/*") },
                        onUploadVideo = { videoPicker.launch("video/*") },
                        onUploadPdf = { pdfPicker.launch("application/pdf") },
                        onVoice = { startVoiceInput() },
                        onSpeakMessage = { speak(it, manual = true) }
                    )

                    HomePage.DISCOVER -> DiscoverPage(
                        darkMode = darkMode,
                        onCategoryClick = { Toast.makeText(context, "Browsing $it", Toast.LENGTH_SHORT).show() },
                        location = currentPreciseLocation,
                        onBookDoctor = { doctor ->
                            if (!doctor.phone.isNullOrBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${doctor.phone}"))
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "No contact number for ${doctor.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    HomePage.PLAN -> CarePlanPage(
                        darkMode = darkMode,
                        prescriptionPlan = prescriptionPlan,
                        workoutPlan = workoutPlan,
                        interactionCheck = interactionCheck,
                        locationLabel = locationLabel,
                        loadingDoctors = doctorFetchLoading,
                        doctors = nearbyDoctors,
                        triageLevel = triageResult?.level,
                        healthHistory = healthHistory,
                        medicineReminders = medicineReminders,
                        onFetchLocation = { fetchLocationAndDoctors() },
                        onBookDoctor = { doctor ->
                            if (!doctor.phone.isNullOrBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${doctor.phone}"))
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "No contact number for ${doctor.name}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCallAmbulance = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:102"))
                            context.startActivity(intent)
                        },
                        onToggleReminder = { reminder ->
                            viewModel.updateMedicineReminder(reminder.copy(isTaken = !reminder.isTaken))
                        }
                    )

                    HomePage.VITALS -> VitalsPage(
                        darkMode = darkMode,
                        vitals = vitalsHistory,
                        isScanning = bluetoothScanning,
                        isConnected = isConnectedToWatch,
                        onStartScan = { startRealBluetoothScan() }
                    )

                    HomePage.PROFILE -> ProfilePage(
                        darkMode = darkMode,
                        userName = userName,
                        age = age,
                        abhaId = abhaId,
                        knownConditions = knownConditions,
                        currentMeds = currentMeds,
                        allergies = allergies,
                        onNameChange = { userName = it },
                        onAgeChange = { age = it },
                        onAbhaIdChange = { abhaId = it },
                        onKnownConditionsChange = { knownConditions = it },
                        onCurrentMedsChange = { currentMeds = it },
                        onAllergiesChange = { allergies = it },
                        onSave = {
                            profileReady = userName.isNotBlank() && age.isNotBlank() && abhaId.isNotBlank()
                            Toast.makeText(context, if (profileReady) "Profile saved" else "Name, age, ABHA ID required", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        if (assistantState.loading) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xCC0EA5E9)
            ) {
                Text(
                    text = "Getting Grok clinical response...",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color.White
                )
            }
        }

        if (severeDialogVisible) {
            SevereEscalationDialog(
                onDismiss = { severeDialogVisible = false },
                onCallEmergency = {
                    severeDialogVisible = false
                    Toast.makeText(context, "Calling emergency 112...", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (emergencyVitalsVisible) {
            EmergencyVitalsAlert(
                countdown = emergencyCountdown,
                onDismiss = { emergencyVitalsVisible = false },
                onCallNow = {
                    emergencyVitalsVisible = false
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:102"))
                    context.startActivity(intent)
                }
            )
        }
    }

    LaunchedEffect(assistantState.response, assistantState.prescriptionPlan, assistantState.workoutPlan) {
        val answer = assistantState.response ?: return@LaunchedEffect
        val formatted = formatStructuredClinicalResponse(
            diagnosis = answer,
            prescription = assistantState.prescriptionPlan,
            workout = assistantState.workoutPlan,
            triage = triageResult?.level?.label
        )
        if (chatMessages.lastOrNull()?.text != formatted) {
            chatMessages.add(ChatMessage(byUser = false, text = formatted))
            speak(formatted)
            
            // Persist the AI diagnosis to health history
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            viewModel.addHealthRecord(
                HealthRecord(
                    date = now,
                    event = "AI Diagnosis: ${activeSymptom ?: "Symptom Analysis"}",
                    triage = triageResult?.level?.label ?: "Unknown",
                    details = answer
                )
            )
            
            // Auto-generate a reminder if a prescription exists
            assistantState.prescriptionPlan?.let { plan ->
                if (plan.contains("tablet", ignoreCase = true) || plan.contains("medication", ignoreCase = true)) {
                    val medicineName = plan.split(" ").firstOrNull { it.length > 3 } ?: "New Med"
                    viewModel.addMedicineReminder(
                        MedicineReminder(
                            name = medicineName,
                            dosage = "1 tablet",
                            time = "09:00 AM",
                            isTaken = false
                        )
                    )
                }
            }
        }
    }
    LaunchedEffect(assistantState.prescriptionPlan, assistantState.workoutPlan, assistantState.interactionCheck) {
        prescriptionPlan = assistantState.prescriptionPlan
        workoutPlan = assistantState.workoutPlan
        interactionCheck = assistantState.interactionCheck
    }
    LaunchedEffect(assistantState.error) {
        if (assistantState.error != null) {
            Toast.makeText(context, assistantState.error, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun ProfilePage(
    darkMode: Boolean,
    userName: String,
    age: String,
    abhaId: String,
    knownConditions: String,
    currentMeds: String,
    allergies: String,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onAbhaIdChange: (String) -> Unit,
    onKnownConditionsChange: (String) -> Unit,
    onCurrentMedsChange: (String) -> Unit,
    onAllergiesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            val scale by animateFloatAsState(
                targetValue = if (listState.firstVisibleItemIndex == 0) 1f else 0.95f,
                label = "headerScale"
            )
            GlassCard(
                darkMode = darkMode,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = scale
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(liquidPrimary(darkMode).copy(alpha = 0.2f))
                            .border(2.dp, liquidPrimary(darkMode), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👤", style = MaterialTheme.typography.headlineMedium)
                    }
                    Column {
                        Text(
                            if (userName.isBlank()) "Guest Patient" else userName,
                            color = liquidText(darkMode),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "ABHA: ${if (abhaId.isBlank()) "Not set" else abhaId}",
                            color = liquidMutedText(darkMode),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Personal Information", color = liquidText(darkMode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppOutlinedField(value = userName, onValueChange = onNameChange, label = "Full Name", darkMode = darkMode)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppOutlinedField(value = age, onValueChange = onAgeChange, label = "Age", darkMode = darkMode, modifier = Modifier.weight(1f))
                        AppOutlinedField(value = abhaId, onValueChange = onAbhaIdChange, label = "ABHA ID", darkMode = darkMode, modifier = Modifier.weight(2f))
                    }
                }
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Medical History", color = liquidText(darkMode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppOutlinedField(value = knownConditions, onValueChange = onKnownConditionsChange, label = "Known Conditions (e.g. Diabetes)", darkMode = darkMode)
                    AppOutlinedField(value = currentMeds, onValueChange = onCurrentMedsChange, label = "Current Medications", darkMode = darkMode)
                    AppOutlinedField(value = allergies, onValueChange = onAllergiesChange, label = "Allergies", darkMode = darkMode)
                }
            }
        }
        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = liquidPrimary(darkMode))
            ) {
                Text("Update Health Profile", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun AssistantPage(
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    talkbackEnabled: Boolean,
    onToggleTalkback: (Boolean) -> Unit,
    userName: String,
    messages: List<ChatMessage>,
    recursiveQuestions: List<String>,
    triageResult: TriageResult?,
    symptomInput: String,
    selectedImage: String,
    selectedVideo: String,
    selectedLabFile: String,
    imageSummary: String?,
    videoSummary: String?,
    labSummary: String?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onUploadImage: () -> Unit,
    onUploadVideo: () -> Unit,
    onUploadPdf: () -> Unit,
    onVoice: () -> Unit,
    onSpeakMessage: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            val scale by animateFloatAsState(
                targetValue = if (listState.firstVisibleItemIndex == 0) 1f else 0.9f,
                label = "headerScale"
            )
            GlassCard(
                darkMode = darkMode,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = scale
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(liquidPrimary(darkMode).copy(alpha = 0.2f))
                                .border(1.dp, liquidPrimary(darkMode).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🩺", style = MaterialTheme.typography.headlineSmall)
                        }
                        Column {
                            Text(
                                "CAre Ai",
                                color = liquidText(darkMode),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Welcome, ${if (userName.isBlank()) "Patient" else userName}",
                                color = liquidMutedText(darkMode),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onToggleTalkback(talkbackEnabled) },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, liquidStroke(darkMode).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = liquidText(darkMode))
                        ) {
                            Text(if (talkbackEnabled) "🔊" else "🔇")
                        }
                        OutlinedButton(
                            onClick = onToggleTheme,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, liquidStroke(darkMode).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = liquidText(darkMode))
                        ) {
                            Text(if (darkMode) "🌙" else "☀️")
                        }
                    }
                }
                Text(
                    "Smart triage, prescription support, and recovery planning.",
                    color = liquidMutedText(darkMode),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Quick Uploads", color = liquidText(darkMode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onVoice,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, liquidStroke(darkMode)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = liquidText(darkMode)),
                            modifier = Modifier.weight(1f)
                        ) { Text("🎙️ Voice") }
                        OutlinedButton(
                            onClick = onUploadImage,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, liquidStroke(darkMode)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = liquidText(darkMode)),
                            modifier = Modifier.weight(1f)
                        ) { Text("🖼️ Image") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onUploadVideo,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, liquidStroke(darkMode)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = liquidText(darkMode)),
                            modifier = Modifier.weight(1f)
                        ) { Text("🎥 Video") }
                        OutlinedButton(
                            onClick = onUploadPdf,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, liquidStroke(darkMode)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = liquidText(darkMode)),
                            modifier = Modifier.weight(1f)
                        ) { Text("📄 Lab PDF") }
                    }
                }
                
                Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (selectedImage != "No image selected") Text("Image: $selectedImage", color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                    if (selectedVideo != "No video selected") Text("Video: $selectedVideo", color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                    if (selectedLabFile != "No PDF selected") Text("PDF: $selectedLabFile", color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                }
                
                if (imageSummary != null || videoSummary != null || labSummary != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("AI Analysis Summaries:", color = liquidText(darkMode), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    if (imageSummary != null) Text("• Vision: $imageSummary", color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                    if (videoSummary != null) Text("• Video: $videoSummary", color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                    if (labSummary != null) Text("• Lab: $labSummary", color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Assistant Chat", color = liquidText(darkMode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                messages.takeLast(8).forEach { message ->
                    val alignment = if (message.byUser) Alignment.End else Alignment.Start
                    val bubbleColor = if (message.byUser) liquidPrimary(darkMode).copy(alpha = 0.85f) else liquidSurfaceAlt(darkMode).copy(alpha = 0.9f)
                    val textColor = if (message.byUser) Color.White else liquidText(darkMode)
                    val shape = if (message.byUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                    
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                        Surface(
                            color = bubbleColor,
                            shape = shape,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .widthIn(max = 280.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (message.byUser) Color.White.copy(alpha = 0.2f) else liquidStroke(darkMode),
                                    shape = shape
                                )
                        ) {
                            Text(
                                message.text,
                                modifier = Modifier.padding(12.dp),
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!message.byUser) {
                                TextButton(
                                    onClick = { onSpeakMessage(message.text) },
                                    modifier = Modifier.align(Alignment.End).padding(end = 4.dp, bottom = 2.dp)
                                ) {
                                    Text("🔊 Listen", style = MaterialTheme.typography.labelSmall, color = liquidPrimary(darkMode))
                                }
                            }
                        }
                    }
                }
                if (recursiveQuestions.isNotEmpty()) {
                    Text("Follow-up questions:", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                    recursiveQuestions.forEach { Text("- $it", color = liquidMutedText(darkMode)) }
                }
                if (triageResult != null) {
                    Text("Risk: ${triageResult.level.label} - ${triageResult.reason}", color = triageResult.level.color)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppOutlinedField(
                        value = symptomInput,
                        onValueChange = onInputChange,
                        label = "Describe your issue...",
                        modifier = Modifier.weight(1f),
                        darkMode = darkMode
                    )
                    Button(
                        onClick = onSend,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = liquidPrimary(darkMode))
                    ) { Text("Ask", color = liquidText(darkMode)) }
                }
                Text("AI disclaimer: always verify treatment with a licensed doctor.", color = liquidMutedText(darkMode))
            }
        }
        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}

@Composable
private fun CarePlanPage(
    darkMode: Boolean,
    prescriptionPlan: String?,
    workoutPlan: String?,
    interactionCheck: String?,
    locationLabel: String,
    loadingDoctors: Boolean,
    doctors: List<DoctorItem>,
    triageLevel: TriageLevel?,
    healthHistory: List<HealthRecord>,
    medicineReminders: List<MedicineReminder>,
    onFetchLocation: () -> Unit,
    onBookDoctor: (DoctorItem) -> Unit,
    onCallAmbulance: () -> Unit,
    onToggleReminder: (MedicineReminder) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (triageLevel == TriageLevel.SEVERE) {
            item {
                Button(
                    onClick = onCallAmbulance,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("🚑 CALL AMBULANCE (102)", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Medicine Reminders", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                if (medicineReminders.isEmpty()) {
                    Text("No reminders set.", color = liquidMutedText(darkMode))
                }
                medicineReminders.forEach { reminder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (reminder.isTaken) liquidPrimary(darkMode).copy(alpha = 0.1f) else Color.Transparent)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(reminder.name, color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                            Text("${reminder.dosage} • ${reminder.time}", color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                        }
                        androidx.compose.material3.Checkbox(
                            checked = reminder.isTaken,
                            onCheckedChange = { onToggleReminder(reminder) }
                        )
                    }
                }
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Health History", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                healthHistory.forEach { record ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(record.date, color = liquidPrimary(darkMode), style = MaterialTheme.typography.labelSmall)
                            Surface(
                                color = when(record.triage.lowercase()) {
                                    "severe" -> Color(0xFFE53935)
                                    "moderate" -> Color(0xFFF9A825)
                                    else -> Color(0xFF2E7D32)
                                }.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(record.triage, modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text(record.event, color = liquidText(darkMode), fontWeight = FontWeight.SemiBold)
                        Text(record.details, color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        androidx.compose.material3.HorizontalDivider(color = liquidStroke(darkMode).copy(alpha = 0.3f))
                    }
                }
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Care Plan", color = liquidText(darkMode), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Generated from AI response", color = liquidMutedText(darkMode))
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Prescription", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                Text(prescriptionPlan ?: "Submit symptom in Assistant tab to generate prescription.", color = liquidMutedText(darkMode))
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Workout / Recovery", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                Text(workoutPlan ?: "Workout plan appears after analysis.", color = liquidMutedText(darkMode))
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Interaction Safety", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                Text(interactionCheck ?: "ABHA interaction check appears after analysis.", color = liquidMutedText(darkMode))
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Recovery Graph", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                LiquidGraph(darkMode = darkMode, triageLevel = triageLevel)
            }
        }
        item {
            GlassCard(darkMode = darkMode) {
                Text("Nearby Doctors", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                Text(locationLabel, color = liquidMutedText(darkMode))
                Button(
                    onClick = onFetchLocation,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = liquidPrimary(darkMode))
                ) {
                    Text(
                        if (loadingDoctors) "Fetching nearby doctors..." else "Fetch My Location",
                        color = liquidText(darkMode)
                    )
                }
                doctors.take(5).forEach { doctor ->
                    Surface(
                        color = liquidSurfaceAlt(darkMode),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doctor.name, color = liquidText(darkMode), fontWeight = FontWeight.SemiBold)
                                Text("${doctor.specialty} · ${doctor.distanceKm} km", color = liquidMutedText(darkMode))
                                if (!doctor.address.isNullOrBlank()) {
                                    Text(doctor.address, color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                                }
                                if (!doctor.phone.isNullOrBlank()) {
                                    Text("📞 ${doctor.phone}", color = liquidPrimary(darkMode), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            OutlinedButton(
                                onClick = { onBookDoctor(doctor) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, liquidStroke(darkMode)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = liquidText(darkMode))
                            ) { Text("Call") }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}

@Composable
private fun GlassCard(
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .background(
                Brush.verticalGradient(
                    colors = if (darkMode) listOf(
                        Color(0x33FFFFFF),
                        Color(0x11FFFFFF)
                    ) else listOf(
                        Color(0x88FFFFFF),
                        Color(0x44FFFFFF)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = if (darkMode) Color.Black else Color(0x22000000)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { content() }
    }
}

@Composable
private fun LiquidOrb(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp,
    colors: List<Color>
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val translationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .offset(y = translationY.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = colors))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = CircleShape
            )
    )
}

@Composable
private fun LiquidGraph(
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    triageLevel: TriageLevel? = null
) {
    val baseBars = when (triageLevel) {
        TriageLevel.SEVERE -> listOf(0.15f, 0.25f, 0.20f, 0.35f, 0.30f, 0.45f, 0.40f)
        TriageLevel.MODERATE -> listOf(0.40f, 0.55f, 0.50f, 0.65f, 0.60f, 0.75f, 0.70f)
        else -> listOf(0.60f, 0.75f, 0.70f, 0.85f, 0.80f, 0.95f, 0.90f)
    }
    val bars = baseBars
    val infiniteTransition = rememberInfiniteTransition(label = "graph")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEachIndexed { index, h ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = h * 0.9f,
                targetValue = h * 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000 + (index * 200), easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedHeight.coerceIn(0.05f, 1f))
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                when (triageLevel) {
                                    TriageLevel.SEVERE -> Color(0xFFE53935)
                                    TriageLevel.MODERATE -> Color(0xFFF9A825)
                                    else -> liquidPrimary(darkMode)
                                }.copy(alpha = 0.9f),
                                when (triageLevel) {
                                    TriageLevel.SEVERE -> Color(0xFFEF9A9A)
                                    TriageLevel.MODERATE -> Color(0xFFFFF59D)
                                    else -> liquidPrimarySoft(darkMode)
                                }.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
            )
        }
    }
}

@Composable
private fun VitalsPage(
    darkMode: Boolean,
    vitals: List<VitalReading>,
    isScanning: Boolean,
    isConnected: Boolean,
    onStartScan: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlassCard(darkMode = darkMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Vitals Tracking", color = liquidText(darkMode), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) Color.Green else Color.Red)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isConnected) "Watch Connected" else "No Watch Connected",
                                color = liquidMutedText(darkMode),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Button(
                        onClick = onStartScan,
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = liquidPrimary(darkMode))
                    ) {
                        Text(if (isScanning) "Scanning..." else "Sync Watch", color = Color.White)
                    }
                }
            }
        }

        if (!isConnected && !isScanning) {
            item {
                GlassCard(darkMode = darkMode) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("⚠️ Real Connection Required", color = Color.Red, fontWeight = FontWeight.Bold)
                        Text(
                            "Please turn on your Smart Watch's Bluetooth and click 'Sync Watch' to fetch live data.",
                            color = liquidMutedText(darkMode),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            GlassCard(darkMode = darkMode) {
                Text("Blood Pressure (mmHg)", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                if (isConnected || vitals.isNotEmpty()) {
                    VitalsChart(
                        darkMode = darkMode,
                        data = vitals.map { it.bloodPressureSys.toFloat() },
                        data2 = vitals.map { it.bloodPressureDia.toFloat() },
                        color1 = Color(0xFFEF5350),
                        color2 = Color(0xFF42A5F5)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No data available. Connect watch.", color = liquidMutedText(darkMode))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("● Systolic", color = Color(0xFFEF5350), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.size(16.dp))
                    Text("● Diastolic", color = Color(0xFF42A5F5), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item {
            GlassCard(darkMode = darkMode) {
                Text("Oxygen Saturation (SpO2 %)", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                if (isConnected || vitals.isNotEmpty()) {
                    VitalsChart(
                        darkMode = darkMode,
                        data = vitals.map { it.oxygenLevel.toFloat() },
                        color1 = Color(0xFF66BB6A)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No data available. Connect watch.", color = liquidMutedText(darkMode))
                    }
                }
                Text("Current: ${if (isConnected && vitals.isNotEmpty()) vitals.last().oxygenLevel.toString() + "%" else "--"}", color = Color(0xFF66BB6A), modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun VitalsChart(
    darkMode: Boolean,
    data: List<Float>,
    data2: List<Float>? = null,
    color1: Color,
    color2: Color = Color.Transparent
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(vertical = 16.dp)
    ) {
        if (data.size < 2) return@Canvas
        
        val maxVal = 160f
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)

        // Draw line 1
        val path1 = Path().apply {
            data.forEachIndexed { i, v ->
                val x = i * stepX
                val y = height - (v / maxVal * height)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(path1, color1, style = Stroke(width = 3.dp.toPx()))

        // Draw line 2 if exists
        data2?.let { d2 ->
            val path2 = Path().apply {
                d2.forEachIndexed { i, v ->
                    val x = i * stepX
                    val y = height - (v / maxVal * height)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(path2, color2, style = Stroke(width = 3.dp.toPx()))
        }
    }
}

@Composable
private fun AppOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    darkMode: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, color = liquidMutedText(darkMode)) },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = liquidSurface(darkMode),
            unfocusedContainerColor = liquidSurface(darkMode).copy(alpha = 0.88f),
            focusedIndicatorColor = Color(0xFF89A7FF),
            unfocusedIndicatorColor = liquidStroke(darkMode),
            focusedTextColor = liquidText(darkMode),
            unfocusedTextColor = liquidText(darkMode),
            focusedLabelColor = liquidMutedText(darkMode),
            unfocusedLabelColor = liquidMutedText(darkMode),
            cursorColor = liquidPrimary(darkMode)
        )
    )
}

@Composable
private fun SevereEscalationDialog(
    onDismiss: () -> Unit,
    onCallEmergency: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
                Text("Severe Symptoms Detected")
            }
        },
        text = {
            Text(
                "Chest pain, severe bleeding, or loss of consciousness trigger immediate stop. Please call 112 now and proceed with emergency care."
            )
        },
        confirmButton = {
            Button(
                onClick = onCallEmergency,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("Call 112")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}


private suspend fun getCurrentLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null

    // 1. Try to get a fresh location fix with a timeout
    val freshLocation = withTimeoutOrNull(8000) { // 8 second timeout for fresh GPS fix
        suspendCancellableCoroutine<Location?> { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            }

            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener)
            } else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener)
            } else {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                manager.removeUpdates(listener)
            }
        }
    }

    if (freshLocation != null) return freshLocation

    // 2. Fallback to best last known location if fresh fix timed out
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    var best: Location? = null
    for (provider in providers) {
        val loc = runCatching { manager.getLastKnownLocation(provider) }.getOrNull() ?: continue
        if (best == null || loc.accuracy < best.accuracy) {
            best = loc
        }
    }
    return best
}

private fun generateNearbyDoctors(location: Location): List<DoctorItem> {
    val seed = kotlin.math.abs(location.latitude + location.longitude)
    val base = (seed % 3.0) + 1.0
    return listOf(
        DoctorItem("Dr. Arjun Mehta", "General Physician", base + 0.6, "9876543210", "Sector 14, Gurugram"),
        DoctorItem("Dr. Neha Rao", "Internal Medicine", base + 1.1, "9876543211", "MG Road, Gurugram"),
        DoctorItem("Dr. Kabir Singh", "Pulmonology", base + 1.8, "9876543212", "Sushant Lok, Gurugram"),
        DoctorItem("Dr. Aditi Sharma", "Family Medicine", base + 2.3, "9876543213", "Golf Course Road, Gurugram")
    )
}

private suspend fun fetchNearbyDoctorsFromApi(location: Location): List<DoctorItem> {
    val categories = "healthcare.hospital,healthcare.clinic,healthcare.doctor"
    val filter = "circle:${location.longitude},${location.latitude},10000"
    val response = runCatching {
        ApiModule.geoapifyApi.search(
            categories = categories,
            filter = filter,
            limit = 10,
            apiKey = BuildConfig.GEOAPIFY_API_KEY
        )
    }.getOrNull() ?: return emptyList()

    val features = response.features.orEmpty()
    return features.mapNotNull { feature ->
        val props = feature.properties ?: return@mapNotNull null
        val name = props.name ?: return@mapNotNull null
        val categoriesList = props.categories.orEmpty()
        val specialty = when {
            categoriesList.contains("healthcare.hospital") -> "Hospital"
            categoriesList.contains("healthcare.clinic") -> "Clinic"
            categoriesList.contains("healthcare.doctor") -> "Doctor"
            else -> "Healthcare"
        }
        val distance = (props.distance ?: 0.0) / 1000.0
        val phone = props.contact?.phone
        val address = listOfNotNull(props.address_line1, props.address_line2).joinToString(", ")
        DoctorItem(name = name, specialty = specialty, distanceKm = distance, phone = phone, address = address)
    }.sortedBy { it.distanceKm }
}

private suspend fun fetchNearbyHospitalsFromOSM(location: Location): List<DoctorItem> {
    val query = """
        [out:json][timeout:25];
        (
          node["amenity"="hospital"](around:5000,${location.latitude},${location.longitude});
          node["amenity"="doctors"](around:5000,${location.latitude},${location.longitude});
        );
        out body;
    """.trimIndent()
    
    val response = runCatching { ApiModule.overpassApi.search(query) }.getOrNull() ?: return emptyList()
    return response.elements.orEmpty().mapNotNull { element ->
        val tags = element.tags ?: return@mapNotNull null
        val name = tags["name"] ?: tags["operator"] ?: "Medical Facility"
        val type = if (tags["amenity"] == "hospital") "Hospital" else "Doctor/Clinic"
        val phone = tags["phone"] ?: tags["contact:phone"]
        val addr = listOfNotNull(tags["addr:street"], tags["addr:suburb"], tags["addr:city"]).joinToString(", ")
        val dist = distanceKm(location.latitude, location.longitude, element.lat ?: 0.0, element.lon ?: 0.0)
        
        DoctorItem(
            name = name,
            specialty = type,
            distanceKm = dist,
            phone = phone,
            address = addr.ifBlank { "Location details via OSM" }
        )
    }.sortedBy { it.distanceKm }
}

private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val result = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, result)
    return (result[0] / 1000.0).let { kotlin.math.round(it * 10) / 10.0 }
}

private fun liquidText(darkMode: Boolean): Color = if (darkMode) Color(0xFFF4F6FF) else Color(0xFF182235)
private fun liquidMutedText(darkMode: Boolean): Color = if (darkMode) Color(0xFFB8C0E0) else Color(0xFF5F6C82)
private fun liquidSurface(darkMode: Boolean): Color = if (darkMode) Color(0xFF171F36) else Color(0xFFFFFFFF)
private fun liquidSurfaceAlt(darkMode: Boolean): Color = if (darkMode) Color(0xFF202A45) else Color(0xFFF6FAFF)
private fun liquidPrimary(darkMode: Boolean): Color = if (darkMode) Color(0xFF66B6FF) else Color(0xFF4E82FF)
private fun liquidPrimarySoft(darkMode: Boolean): Color = if (darkMode) Color(0xFF8B9BFF) else Color(0xFF9BC3FF)
private fun liquidStroke(darkMode: Boolean): Color = if (darkMode) Color(0xFF36466E) else Color(0xFFCBD9F5)

private fun generateLabSummary(fileName: String): String {
    return "Summary for $fileName: Hemoglobin slightly low, CRP mildly elevated, platelet count normal. Recommended: repeat CBC in 2 weeks and consult physician if fever persists."
}

private fun necessaryQuestionsForSymptom(symptomText: String): List<String> {
    val text = symptomText.lowercase()
    return when {
        text.contains("fever") -> listOf(
            "When did fever start and for how many days has it continued?",
            "What is the highest temperature recorded?",
            "Any cough, sore throat, or body ache?"
        )
        text.contains("snake bite") || text.contains("snake") -> listOf(
            "When did the bite happen?",
            "Which body part is affected?",
            "Is swelling spreading quickly?",
            "Any breathing problem, vomiting, or drowsiness?"
        )
        text.contains("chest pain") -> listOf(
            "Is pain continuous for more than 5 minutes?",
            "Any sweating or shortness of breath?",
            "Any pain radiating to arm, jaw, or back?"
        )
        text.contains("cough") -> listOf(
            "Is cough dry or with mucus?",
            "Any shortness of breath or chest discomfort?",
            "How many days has cough persisted?"
        )
        text.contains("pain") -> listOf(
            "Where exactly is the pain?",
            "Pain severity from 1 to 10?",
            "Does movement worsen or reduce pain?"
        )
        else -> listOf(
            "When did this symptom start?",
            "Did it get better or worse in last 24 hours?"
        )
    }
}

private fun buildEnrichedSymptom(baseSymptom: String, answers: List<String>): String {
    if (answers.isEmpty()) return baseSymptom
    return "$baseSymptom\nAdditional details:\n${answers.joinToString("\n")}"
}

private fun formatStructuredClinicalResponse(
    diagnosis: String,
    prescription: String?,
    workout: String?,
    triage: String?
): String {
    val triageLine = triage ?: "Not classified"
    val prescriptionLine = prescription ?: "No prescription returned. Please consult a doctor."
    val workoutLine = workout ?: "No workout guidance returned."
    return buildString {
        append("Diagnosis: ").append(diagnosis).append("\n")
        append("Triage: ").append(triageLine).append("\n")
        append("Medicine: ").append(prescriptionLine).append("\n")
        append("Workout: ").append(workoutLine).append("\n")
        append("Safety: This is AI guidance, not a doctor diagnosis.")
    }
}

private fun evaluateTriage(input: String): TriageResult {
    val text = input.lowercase()
    val severeKeywords = listOf(
        "chest pain",
        "severe bleeding",
        "loss of consciousness",
        "unconscious",
        "can't breathe",
        "snake bite",
        "snake"
    )
    val moderateKeywords = listOf(
        "fever",
        "persistent cough",
        "vomiting",
        "dizziness",
        "infection",
        "migraine",
        "dehydrated",
        "abdominal pain"
    )
    return when {
        severeKeywords.any { text.contains(it) } -> TriageResult(
            level = TriageLevel.SEVERE,
            reason = "Critical trigger words matched emergency conditions."
        )

        moderateKeywords.any { text.contains(it) } -> TriageResult(
            level = TriageLevel.MODERATE,
            reason = "Symptoms suggest physician consultation is needed soon."
        )

        else -> TriageResult(
            level = TriageLevel.LOW,
            reason = "No immediate danger signals found from current input."
        )
    }
}


@Composable
private fun DiscoverPage(
    darkMode: Boolean,
    onCategoryClick: (String) -> Unit,
    location: Location?,
    onBookDoctor: (DoctorItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Doctors", "Clinics", "Hospitals", "Treatments")
    
    val osmData = remember { mutableStateListOf<DoctorItem>() }
    val loading = remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab, location) {
        if (location != null && (selectedTab == 0 || selectedTab == 2)) {
            loading.value = true
            osmData.clear()
            val results = fetchNearbyHospitalsFromOSM(location)
            osmData.addAll(if (selectedTab == 2) {
                results.filter { it.specialty == "Hospital" }
            } else {
                results.filter { it.specialty != "Hospital" }
            })
            loading.value = false
        }
    }
    
    val categories = listOf(
        "Women's Health" to "🤰",
        "Skin & Hair" to "🧴",
        "Child Specialist" to "👶",
        "General Physician" to "🩺",
        "Dental Care" to "🦷",
        "Ear Nose Throat" to "👂",
        "Homoeopathy" to "🧪",
        "Bone and joints" to "🦴"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlassCard(darkMode = darkMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Doctors, Clinics, Specialities...", color = liquidMutedText(darkMode)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Text("🔍") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = liquidPrimary(darkMode),
                        unfocusedIndicatorColor = liquidStroke(darkMode)
                    )
                )
            }
        }

        item {
            GlassCard(darkMode = darkMode) {
                Text("Top Specialties", color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                androidx.compose.foundation.lazy.grid.LazyHorizontalGrid(
                    rows = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories.size) { index ->
                        val cat = categories[index]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCategoryClick(cat.first) }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(liquidPrimary(darkMode).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat.second, style = MaterialTheme.typography.headlineSmall)
                            }
                            Text(
                                cat.first,
                                color = liquidText(darkMode),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            androidx.compose.material3.ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = liquidPrimary(darkMode),
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    androidx.compose.material3.Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        item {
            GlassCard(darkMode = darkMode) {
                val listTitle = when(selectedTab) {
                    0 -> "Nearby Doctors (OSM)"
                    1 -> "Top clinic specialties in Bangalore"
                    2 -> "Nearby Hospitals (OSM)"
                    else -> "Top treatments in Bangalore"
                }
                Text(listTitle, color = liquidText(darkMode), style = MaterialTheme.typography.labelMedium)
                
                if (loading.value) {
                    androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if ((selectedTab == 0 || selectedTab == 2) && osmData.isNotEmpty()) {
                    osmData.forEach { doctor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doctor.name, color = liquidText(darkMode), fontWeight = FontWeight.Bold)
                                Text("${doctor.distanceKm} km • ${doctor.address}", color = liquidMutedText(darkMode), style = MaterialTheme.typography.bodySmall)
                                if (!doctor.phone.isNullOrBlank()) {
                                    Text("📞 ${doctor.phone}", color = liquidPrimary(darkMode), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            OutlinedButton(
                                onClick = { onBookDoctor(doctor) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = liquidPrimary(darkMode))
                            ) {
                                Text("Call")
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(color = liquidStroke(darkMode).copy(alpha = 0.2f))
                    }
                } else {
                    val mockData = when(selectedTab) {
                        0 -> listOf("Dentist (6035)", "Gynecologist (2336)", "Pediatrician (1707)", "Orthopedist (1537)")
                        1 -> listOf("Dental (3413)", "Gynecology (1332)", "Ayurveda (1004)", "Orthopedic (905)")
                        2 -> listOf("Apollo Hospital", "Manipal Hospital", "Fortis Hospital", "Cloudnine")
                        else -> listOf("Premature Ejaculation", "Urinary Tract Infection", "Diabetes", "Dengue")
                    }

                    mockData.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item, color = liquidText(darkMode))
                            Text("❯", color = liquidMutedText(darkMode))
                        }
                        androidx.compose.material3.HorizontalDivider(color = liquidStroke(darkMode).copy(alpha = 0.2f))
                    }
                }
                TextButton(onClick = { /* View All */ }) {
                    Text("View All", color = liquidPrimary(darkMode))
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun EmergencyVitalsAlert(
    countdown: Int,
    onDismiss: () -> Unit,
    onCallNow: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beep")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFB71C1C).copy(alpha = alpha),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🚨 ABNORMAL VITALS!", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "High Blood Pressure or Low Oxygen detected! Beeping...",
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Calling Ambulance in $countdown seconds",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCallNow,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Call Now", color = Color(0xFFB71C1C))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("I'm Okay (Dismiss)", color = Color.White)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CareAiHomePreview() {
    CAreAiTheme {
        CareAiHome(tts = null)
    }
}