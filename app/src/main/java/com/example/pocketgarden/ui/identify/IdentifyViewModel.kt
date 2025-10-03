package com.example.pocketgarden.ui.identify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketgarden.network.IdentificationResponse
import com.example.pocketgarden.repository.IdentificationResult
import com.example.pocketgarden.repository.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class IdentifyUiState {
    object Idle : IdentifyUiState()
    object Loading : IdentifyUiState()
    data class Success(val response: IdentificationResponse?) : IdentifyUiState()
    data class Error(val message: String) : IdentifyUiState()
    data class PendingSaved(val localId: Long) : IdentifyUiState()
}

class IdentifyViewModel(private val repo: PlantRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<IdentifyUiState>(IdentifyUiState.Idle)
    val uiState: StateFlow<IdentifyUiState> = _uiState

    fun identifyBase64(base64: String, details: String? = "common_names,taxonomy,watering,toxicity") {
        viewModelScope.launch {
            _uiState.value = IdentifyUiState.Loading
            when (val result = repo.identifyPlantFromBitmapBase64V3(base64)) {
                is IdentificationResult.Success -> {
                    _uiState.value = IdentifyUiState.Success(result.response)
                }
                is IdentificationResult.Error -> {
                    val msg = "Error ${result.code}: ${result.message}"
                    _uiState.value = IdentifyUiState.Error(msg)
                }
            }
        }
    }

    fun savePlantOffline(imageUri: String) {
        viewModelScope.launch {
            val localId = repo.addPlantOffline(imageUri)
            _uiState.value = IdentifyUiState.PendingSaved(localId)
        }
    }
}