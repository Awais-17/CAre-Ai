package com.example.careai

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CareAiViewModel(
    private val repository: CareAiRepository,
    private val healthDao: HealthDao
) : ViewModel() {
    var uiState = mutableStateOf(AssistantUiState())
        private set

    val healthHistory: Flow<List<HealthRecord>> = healthDao.getAllHealthRecords()
    val medicineReminders: Flow<List<MedicineReminder>> = healthDao.getAllReminders()

    fun addHealthRecord(record: HealthRecord) {
        viewModelScope.launch {
            healthDao.insertHealthRecord(record)
        }
    }

    fun addMedicineReminder(reminder: MedicineReminder) {
        viewModelScope.launch {
            healthDao.insertReminder(reminder)
        }
    }

    fun updateMedicineReminder(reminder: MedicineReminder) {
        viewModelScope.launch {
            healthDao.updateReminder(reminder)
        }
    }

    fun deleteMedicineReminder(reminder: MedicineReminder) {
        viewModelScope.launch {
            healthDao.deleteReminder(reminder)
        }
    }

    fun submitDiagnostic(symptom: String, profile: UserProfile, triage: TriageResult) {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(loading = true, error = null)
            runCatching {
                repository.runDiagnostic(
                    DiagnosticInput(
                        symptom = symptom,
                        profile = profile,
                        triage = triage
                    )
                )
            }.onSuccess { output ->
                uiState.value = AssistantUiState(
                    loading = false,
                    response = output.response,
                    recursiveQuestions = output.recursiveQuestions,
                    prescriptionPlan = output.prescriptionPlan,
                    workoutPlan = output.workoutPlan,
                    interactionCheck = output.interactionCheck
                )
            }.onFailure { throwable ->
                uiState.value = AssistantUiState(
                    loading = false,
                    error = throwable.message ?: "Unable to process diagnostic request."
                )
            }
        }
    }
}

class CareAiViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = CareAiRepository(
            grokApi = ApiModule.groqApi,
            geminiApi = ApiModule.geminiApi,
            gemmaApi = ApiModule.gemmaApi,
            abdmApi = ApiModule.abdmApi
        )
        val healthDao = AppDatabase.getDatabase(context).healthDao()
        @Suppress("UNCHECKED_CAST")
        return CareAiViewModel(repository, healthDao) as T
    }
}
