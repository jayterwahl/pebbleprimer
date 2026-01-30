package com.example.claudewatch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.claudewatch.data.ClaudeRepository
import com.example.claudewatch.presentation.UiState
import com.example.claudewatch.speech.SpeechRecognizerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing chat state and coordinating between
 * speech recognition and Claude API calls.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val repository = ClaudeRepository()

    private var speechHelper: SpeechRecognizerHelper? = null
    private var lastUserMessage: String? = null

    private val speechListener = object : SpeechRecognizerHelper.SpeechResultListener {
        override fun onSpeechResult(text: String) {
            lastUserMessage = text
            sendMessageToClaude(text)
        }

        override fun onSpeechError(errorMessage: String) {
            _uiState.value = UiState.Error(errorMessage)
        }

        override fun onReadyForSpeech() {
            _uiState.value = UiState.Listening
        }

        override fun onEndOfSpeech() {
            // Transition to loading while we wait for speech results
            // The actual loading state will be set when sendMessageToClaude is called
        }
    }

    init {
        speechHelper = SpeechRecognizerHelper(application, speechListener)
    }

    /**
     * Starts listening for voice input.
     * Called when user taps the microphone button.
     */
    fun startListening() {
        if (_uiState.value == UiState.Loading) {
            return // Don't interrupt ongoing API call
        }

        _uiState.value = UiState.Listening
        speechHelper?.startListening()
    }

    /**
     * Stops the current listening session.
     */
    fun stopListening() {
        speechHelper?.stopListening()
    }

    /**
     * Retries the last failed request.
     */
    fun retry() {
        lastUserMessage?.let { message ->
            sendMessageToClaude(message)
        } ?: run {
            // If no last message, go back to idle and let user try again
            _uiState.value = UiState.Idle
        }
    }

    /**
     * Resets the UI to idle state for asking a new question.
     */
    fun resetToIdle() {
        _uiState.value = UiState.Idle
    }

    /**
     * Clears conversation history and resets to idle.
     */
    fun clearConversation() {
        repository.clearHistory()
        lastUserMessage = null
        _uiState.value = UiState.Idle
    }

    /**
     * Sends a message to Claude API and updates UI state accordingly.
     */
    private fun sendMessageToClaude(message: String) {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.sendMessage(message)

            result.fold(
                onSuccess = { response ->
                    _uiState.value = UiState.Response(response)
                },
                onFailure = { error ->
                    val errorMessage = when {
                        error.message?.contains("401") == true ->
                            "Invalid API key"
                        error.message?.contains("429") == true ->
                            "Rate limited. Please wait."
                        error.message?.contains("timeout", ignoreCase = true) == true ||
                        error.message?.contains("timed out", ignoreCase = true) == true ->
                            "Request timed out. Please try again."
                        error.message?.contains("Unable to resolve host") == true ||
                        error.message?.contains("No address associated") == true ->
                            "No internet connection"
                        else ->
                            error.message ?: "An error occurred"
                    }
                    _uiState.value = UiState.Error(errorMessage)
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper?.destroy()
        speechHelper = null
    }
}
