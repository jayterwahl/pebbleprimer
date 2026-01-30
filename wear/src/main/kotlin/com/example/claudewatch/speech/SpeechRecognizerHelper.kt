package com.example.claudewatch.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Helper class for managing speech recognition on Wear OS.
 * Wraps Android's SpeechRecognizer with a simpler callback-based interface.
 */
class SpeechRecognizerHelper(
    private val context: Context,
    private val listener: SpeechResultListener
) {

    interface SpeechResultListener {
        fun onSpeechResult(text: String)
        fun onSpeechError(errorMessage: String)
        fun onReadyForSpeech()
        fun onEndOfSpeech()
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    /**
     * Checks if speech recognition is available on this device.
     */
    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Starts listening for speech input.
     * Results are delivered through the SpeechResultListener.
     */
    fun startListening() {
        if (isListening) {
            return
        }

        if (!isRecognitionAvailable()) {
            listener.onSpeechError("Speech recognition not available on this device")
            return
        }

        // Create recognizer if needed
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        isListening = true
        speechRecognizer?.startListening(intent)
    }

    /**
     * Stops the current speech recognition session.
     */
    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    /**
     * Cancels the current speech recognition session.
     */
    fun cancel() {
        speechRecognizer?.cancel()
        isListening = false
    }

    /**
     * Releases all resources. Call this when done with the helper.
     */
    fun destroy() {
        cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listener.onReadyForSpeech()
            }

            override fun onBeginningOfSpeech() {
                // User has started speaking
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - could be used for visualization
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Sound data received
            }

            override fun onEndOfSpeech() {
                isListening = false
                listener.onEndOfSpeech()
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = getErrorMessage(error)
                listener.onSpeechError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()

                if (text.isNullOrBlank()) {
                    listener.onSpeechError("No speech detected. Please try again.")
                } else {
                    listener.onSpeechResult(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Partial results not used in this implementation
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for future events
            }
        }
    }

    /**
     * Converts speech recognizer error codes to user-friendly messages.
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network error. Please check your connection."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Please try again."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy. Please wait."
            SpeechRecognizer.ERROR_SERVER -> "Server error. Please try again."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Please try again."
            else -> "Speech recognition error. Please try again."
        }
    }
}
