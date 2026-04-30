🏆 3rd Place Winner - PROTONEX 2026 National Level Hackathon (IEEE)
🏥 CAre-Ai: Intelligent Healthcare Assistant
==================================================
CAre-Ai is a full-stack AI-driven healthcare platform designed to bridge the gap between initial symptom onset and clinical intervention. By combining recursive AI triage, real-time IoT vitals tracking, and personalized recovery planning, CAre-Ai provides a true "Doctor in your pocket" experience.
🚀 KEY FEATURES
1. 🧠 Recursive AI Triage (The "Brain")
💬 Initial Inquiry: The user provides a single symptom (e.g., "I have a headache").
🔍 Contextual Probing: The AI asks targeted follow-up questions to determine severity, such as duration, pain level, and associated symptoms.
🏷️ Triage Tagging: Automatically categorizes the case into Mild, Moderate, or Severe/Emergency levels.
2. 💊 Smart Prescription & Dosage Support
🎯 Precision Recommendations: Suggests over-the-counter support based on triage data (e.g., recommending Paracetamol 650mg for moderate fever).
🛡️ Medication Safety: Cross-references symptoms with historical health data to flag potential risks.
3. ⌚ IoT Vitals Integration
📈 Real-Time Monitoring: Syncs with smartwatches and wearables to track Blood Pressure, SpO2, and Heart Rate.
📊 Visual Analytics: Displays health trends via interactive glassmorphic charts to help users identify patterns over time.
4. 🎙️ Multi-Modal Data Ingestion
🗣️ Voice-to-Text: Supports hands-free symptom reporting for when you're too under the weather to type.
📄 Document Analysis: Future-ready OCR for reading lab reports and PDFs.
5. 🇮🇳 ABHA Digital Health Integration
🔗 Seamless Ecosystem: Full compatibility with India’s Ayushman Bharat Health Account (ABHA) ecosystem, allowing users to sync national health IDs for a unified medical history.
🛠️ HOW IT WORKS (THE WORKFLOW)
👋 Onboarding: The user logs in and connects their health profile (ABHA ID) and wearable devices.
🗣️ Symptom Reporting: The user interacts with the Care-Ai Assistant, which uses a structured reasoning chain to analyze input.
🩺 Diagnosis & Triage:
✅ If Mild: Suggests home care and monitors vitals.
⚠️ If Moderate: Provides a recovery plan and local clinic suggestions.
🚨 If Severe: Triggers an emergency alert and provides a "Find Hospital" shortcut.
🛌 Recovery Phase: The system generates a personalized Rx Plan, medication reminders, and health-tracking goals.
🎨 UI/UX PHILOSOPHY: "CALM & CLINICAL"
The interface utilizes a Glassmorphism Design Language to reduce patient anxiety:
🌙 Dark Mode by Default: Reduces eye strain during late-night health emergencies.
🌊 Fluid Navigation: Seamless transitions between the AI Chat, Hospital Finder, and Vitals Dashboard.
🚦 High-Contrast Indicators: Triage levels are color-coded (🟢/🟡/🔴) for instant recognition.
💻 TECH STACK
🌐 Frontend: React / Next.js (Tailwind CSS for Glassmorphic UI)
⚙️ Backend: Python / FastAPI
🗄️ Database: Supabase / MySQL
🤖 AI Engine: Groq API / Claude API (for triage logic)
⚠️ Note: CAre-Ai is an AI-driven support tool designed to assist in triage and health management. It is not a replacement for professional medical advice in life-threatening emergencies—always listen to your human doctor!

## ⚙️ Setup & Installation

### 1. Prerequisites
- Android Studio Ladybug or later.
- Java 17+.
- API Keys for the following services:
  - [Groq API](https://console.groq.com/)
  - [Google Gemini API](https://aistudio.google.com/)
  - [Geoapify API](https://www.geoapify.com/) (Optional, fallback to OSM)

### 2. Configure API Keys
Add your API keys to the `local.properties` file or define them in your `build.gradle` via `BuildConfig`:

```properties
GROQ_API_KEY="your_groq_key"
GEMINI_API_KEY="your_gemini_key"
GEOAPIFY_API_KEY="your_geoapify_key"
```

### 3. Build & Run
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle and build the project.
4. Run on a physical device or emulator (Android 8.0+ recommended).

## 📂 Project Structure

- `MainActivity.kt`: Main UI entry point and core navigation logic.
- `CareAiEngine.kt`: Centralized API logic, LLM fallback handling, and repository.
- `CareAiViewModel.kt`: Manages UI state and business logic.
- `Database.kt`: Room database configuration for health records and reminders.
- `Models.kt`: Standardized data classes used throughout the app.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License.
