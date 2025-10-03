package com.example.pocketgarden.ui.identify

import androidx.lifecycle.*
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.repository.PlantRepository
import kotlinx.coroutines.launch

class SuggestionsViewModel(
    val repo: PlantRepository
) : ViewModel() {

    private val _suggestions = MutableLiveData<List<SuggestionUiModel>>()
    val suggestions: LiveData<List<SuggestionUiModel>> = _suggestions

    private val _selected = MutableLiveData<SuggestionUiModel?>()
    val selected: LiveData<SuggestionUiModel?> = _selected

    // load suggestions into LiveData
    fun setSuggestions(list: List<SuggestionUiModel>) {
        _suggestions.value = list
    }

    fun selectSuggestion(item: SuggestionUiModel) {
        _selected.value = item
    }

    fun confirmSelection() {
        val chosen = _selected.value ?: return
        viewModelScope.launch {
            val plant = PlantEntity(
                name = chosen.name,
                probability = chosen.probability,
                commonNames = chosen.commonNames.joinToString(", "),
                imageUri = "placeholder://uri" // TODO: pass actual image URI
            )
            repo.savePlant(plant)
        }
    }
}
