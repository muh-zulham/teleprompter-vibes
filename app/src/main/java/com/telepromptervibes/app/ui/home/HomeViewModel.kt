package com.telepromptervibes.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.telepromptervibes.app.domain.model.Script
import com.telepromptervibes.app.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class HomeUiState(
    val scripts: List<Script> = emptyList(),
    val isLoading: Boolean = true,
    val scriptToDelete: Script? = null,
)

sealed interface HomeEvent {
    data class NavigateToEditor(val scriptId: String?) : HomeEvent
}

class HomeViewModel(
    private val repository: ScriptRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<HomeEvent?>(null)
    val events: StateFlow<HomeEvent?> = _events.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll()
                .map { list -> HomeUiState(scripts = list, isLoading = false) }
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    fun onAddClicked() {
        _events.value = HomeEvent.NavigateToEditor(scriptId = null)
    }

    fun onScriptClicked(id: String) {
        _events.value = HomeEvent.NavigateToEditor(scriptId = id)
    }

    fun onDeleteRequested(script: Script) {
        _uiState.value = _uiState.value.copy(scriptToDelete = script)
    }

    fun onDeleteConfirmed() {
        val script = _uiState.value.scriptToDelete ?: return
        viewModelScope.launch {
            repository.delete(script.id)
            _uiState.value = _uiState.value.copy(scriptToDelete = null)
        }
    }

    fun onDeleteDismissed() {
        _uiState.value = _uiState.value.copy(scriptToDelete = null)
    }

    fun consumeEvent() {
        _events.value = null
    }

    companion object {
        fun factory(repository: ScriptRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
