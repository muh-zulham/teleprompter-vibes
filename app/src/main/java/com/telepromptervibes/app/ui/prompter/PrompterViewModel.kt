package com.telepromptervibes.app.ui.prompter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.telepromptervibes.app.domain.model.Script
import com.telepromptervibes.app.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrompterUiState(
    val script: Script? = null,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val isPlaying: Boolean = false,
    val speedDpPerSec: Float = DEFAULT_SPEED_DP_PER_SEC,
    val progress: Float = 0f,
    val savedOffsetPx: Float = 0f,
) {
    companion object {
        const val DEFAULT_SPEED_DP_PER_SEC = 40f
        const val MIN_SPEED_DP_PER_SEC = 10f
        const val MAX_SPEED_DP_PER_SEC = 120f
    }
}

class PrompterViewModel(
    private val repository: ScriptRepository,
    private val scriptId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrompterUiState())
    val uiState: StateFlow<PrompterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val script = try {
                repository.getById(scriptId)
            } catch (e: Exception) {
                null
            }
            if (script == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isNotFound = true,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    script = script,
                    isLoading = false,
                    isNotFound = false,
                )
            }
        }
    }

    fun onPlayPauseToggle() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    fun onSpeedChange(speed: Float) {
        val clamped = speed.coerceIn(
            PrompterUiState.MIN_SPEED_DP_PER_SEC,
            PrompterUiState.MAX_SPEED_DP_PER_SEC,
        )
        _uiState.value = _uiState.value.copy(speedDpPerSec = clamped)
    }

    fun onProgressChanged(progress: Float) {
        val safe = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
        _uiState.value = _uiState.value.copy(progress = safe)
    }

    fun onScrollOffsetChanged(offsetPx: Float) {
        val safe = if (offsetPx.isFinite()) offsetPx.coerceAtLeast(0f) else 0f
        _uiState.value = _uiState.value.copy(savedOffsetPx = safe)
    }

    companion object {
        fun factory(repository: ScriptRepository, scriptId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PrompterViewModel(repository, scriptId) as T
                }
            }
    }
}
