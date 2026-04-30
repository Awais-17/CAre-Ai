3rd place in national level hackathon because of the unique features and user experience it offers.



# CAre Ai 🩺🤖

CAre Ai is a cutting-edge, AI-powered medical assistant Android application built with Jetpack Compose. It provides users with instant clinical assessments, nearby medical facility discovery, and proactive health monitoring.

## 🚀 Features

- **AI Diagnostic Engine**: Multi-LLM fallback system (Groq → Gemini → Gemma) for reliable medical triage and advice.
- **Vitals Monitoring**: Real-time tracking of Blood Pressure and Oxygen levels with emergency alert systems.
- **Smart Discovery**: Find nearby doctors, clinics, and hospitals using OpenStreetMap (OSM) data.
- **Care Plans**: Personalized medicine reminders and recovery workout routines.
- **ABHA Integration**: Secure history cross-checks for drug-drug interactions and allergies.
- **Modern UI**: Beautiful Glassmorphism design with Dark Mode support and intuitive navigation.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Local Database**: Room Persistence Library
- **Networking**: Retrofit & OkHttp
- **Architecture**: MVVM (Model-View-ViewModel)
- **AI Integration**: Groq API, Google Gemini, and Gemma LLMs

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
