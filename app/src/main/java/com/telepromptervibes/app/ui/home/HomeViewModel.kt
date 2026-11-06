package com.telepromptervibes.app.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val script: String = "",
)

/** Event satu-kali dari ViewModel ke UI; UI yang mengeksekusi (butuh Context). */
sealed interface HomeEvent {
    data class StartPrompter(val script: String) : HomeEvent
}

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = MutableStateFlow<HomeEvent?>(null)
    val events: StateFlow<HomeEvent?> = _events.asStateFlow()

    fun onScriptChange(value: String) {
        _state.value = _state.value.copy(script = value)
    }

    fun onStartClicked() {
        val script = _state.value.script.trim()
        if (script.isEmpty()) return
        _events.value = HomeEvent.StartPrompter(script)
    }

    fun consumeEvent() {
        _events.value = null
    }
}
