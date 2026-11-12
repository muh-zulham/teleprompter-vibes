package com.telepromptervibes.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.telepromptervibes.app.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorUiState(
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = true,
    val hasChanges: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val isSaving: Boolean = false,
) {
    /** Jumlah kata dari content. Whitespace berlebih diabaikan. */
    val wordCount: Int
        get() = content.trim()
            .split(Regex("\\s+"))
            .count { it.isNotBlank() }

    /** Estimasi durasi baca dalam menit (dibulatkan ke atas). 0 jika kosong. */
    val estimatedMinutes: Int
        get() {
            val wc = wordCount
            if (wc == 0) return 0
            return (wc + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE
        }

    companion object {
        const val WORDS_PER_MINUTE = 130
    }
}

sealed interface EditorEvent {
    data object NavigateBack : EditorEvent
}

class EditorViewModel(
    private val repository: ScriptRepository,
    private val scriptId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<EditorEvent?>(null)
    val events: StateFlow<EditorEvent?> = _events.asStateFlow()

    init {
        viewModelScope.launch {
            if (scriptId == null) {
                // Create mode
                _uiState.value = EditorUiState(isLoading = false)
            } else {
                // Edit mode: load existing. If not found (defensive), emit NavigateBack.
                val existing = repository.getById(scriptId)
                if (existing == null) {
                    _events.value = EditorEvent.NavigateBack
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value = EditorUiState(
                        title = existing.title,
                        content = existing.content,
                        isLoading = false,
                        hasChanges = false,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value, hasChanges = true)
    }

    fun onContentChange(value: String) {
        _uiState.value = _uiState.value.copy(content = value, hasChanges = true)
    }

    fun onSave() {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) return

        viewModelScope.launch {
            if (scriptId == null) {
                // Create
                repository.createNew(state.title, state.content)
            } else {
                // Edit: preserve createdAt
                val existing = repository.getById(scriptId)
                if (existing != null) {
                    repository.upsert(
                        existing.copy(
                            title = state.title.trim().ifEmpty { "Untitled" },
                            content = state.content,
                            updatedAt = System.currentTimeMillis(),
                            // createdAt TIDAK diubah — tetap dari existing
                        )
                    )
                }
            }
            _events.value = EditorEvent.NavigateBack
        }
    }

    /**
     * Save lalu panggil onSuccess(scriptId) tanpa emit NavigateBack.
     * Digunakan untuk alur "Mulai Prompter" (auto-save sebelum navigate).
     *
     * Owner constraints:
     * - onSuccess hanya dipanggil setelah save BERHASIL.
     * - Edit mode dengan existing == null: tidak navigate, tidak buat duplikat.
     * - Exception ditangkap, tidak crash, user tetap di Editor.
     */
    fun onSaveThen(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) return
        if (state.isSaving) return

        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val savedId: String = if (scriptId == null) {
                    // Create mode: createNew mengembalikan Script dengan ID baru
                    val created = repository.createNew(state.title, state.content)
                    created.id
                } else {
                    // Edit mode: preserve createdAt, no duplicate
                    val existing = repository.getById(scriptId)
                    if (existing == null) {
                        // Defensive: script hilang di DB. Jangan navigate.
                        return@launch
                    }
                    repository.upsert(
                        existing.copy(
                            title = state.title.trim().ifEmpty { "Untitled" },
                            content = state.content,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                    scriptId
                }
                _uiState.value = _uiState.value.copy(isSaving = false)
                onSuccess(savedId)
            } catch (e: Exception) {
                // TODO(ui-error): tampilkan error UI proper. Untuk sekarang: log + tetap di Editor.
                android.util.Log.e("EditorViewModel", "onSaveThen failed", e)
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    fun onBackPressed() {
        if (_uiState.value.hasChanges) {
            _uiState.value = _uiState.value.copy(showDiscardDialog = true)
        } else {
            _events.value = EditorEvent.NavigateBack
        }
    }

    fun onDiscardConfirmed() {
        _uiState.value = _uiState.value.copy(showDiscardDialog = false)
        _events.value = EditorEvent.NavigateBack
    }

    fun onDiscardDismissed() {
        _uiState.value = _uiState.value.copy(showDiscardDialog = false)
    }

    fun consumeEvent() {
        _events.value = null
    }

    companion object {
        fun factory(repository: ScriptRepository, scriptId: String?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EditorViewModel(repository, scriptId) as T
                }
            }
    }
}
