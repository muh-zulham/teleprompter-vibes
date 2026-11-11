package com.telepromptervibes.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.telepromptervibes.app.domain.model.Script
import com.telepromptervibes.app.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class HomeUiState(
    val scripts: List<Script> = emptyList(),
    val isLoading: Boolean = true,
    val scriptToDelete: Script? = null,
    val searchQuery: String = "",
)

sealed interface HomeEvent {
    data class NavigateToEditor(val scriptId: String?) : HomeEvent
}

class HomeViewModel(
    private val repository: ScriptRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val scriptToDelete = MutableStateFlow<Script?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAll(),
        searchQuery,
        scriptToDelete
    ) { scripts, query, toDelete ->
        val filtered = if (query.isBlank()) {
            scripts
        } else {
            scripts.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }
        HomeUiState(
            scripts = filtered,
            isLoading = false,
            scriptToDelete = toDelete,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    private val _events = MutableStateFlow<HomeEvent?>(null)
    val events: StateFlow<HomeEvent?> = _events.asStateFlow()

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onAddClicked() {
        _events.value = HomeEvent.NavigateToEditor(scriptId = null)
    }

    fun onScriptClicked(id: String) {
        _events.value = HomeEvent.NavigateToEditor(scriptId = id)
    }

    fun onDeleteRequested(script: Script) {
        scriptToDelete.value = script
    }

    fun onDeleteConfirmed() {
        val script = scriptToDelete.value ?: return
        viewModelScope.launch {
            repository.delete(script.id)
            scriptToDelete.value = null
        }
    }

    fun onDeleteDismissed() {
        scriptToDelete.value = null
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
