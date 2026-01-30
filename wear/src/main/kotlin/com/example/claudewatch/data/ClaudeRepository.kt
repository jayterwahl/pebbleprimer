package com.example.claudewatch.data

import com.example.claudewatch.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repository for Claude API interactions.
 * Manages conversation history and API calls.
 */
class ClaudeRepository {

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/"
        private const val MAX_HISTORY_SIZE = 10 // Keep last 10 exchanges (5 user + 5 assistant)
    }

    private val conversationHistory = mutableListOf<Message>()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: ClaudeApi = retrofit.create(ClaudeApi::class.java)

    /**
     * Sends a message to Claude and returns the response.
     * Maintains conversation history for context-aware responses.
     */
    suspend fun sendMessage(userMessage: String): Result<String> {
        return try {
            // Add user message to history
            conversationHistory.add(Message(role = "user", content = userMessage))

            // Trim history if too long
            trimHistory()

            // Make API call with full history
            val request = MessageRequest(
                messages = conversationHistory.toList()
            )

            val response = api.sendMessage(
                apiKey = BuildConfig.CLAUDE_API_KEY,
                request = request
            )

            // Extract text from response
            val responseText = response.content.firstOrNull()?.text
                ?: return Result.failure(Exception("Empty response from Claude"))

            // Add assistant response to history
            conversationHistory.add(Message(role = "assistant", content = responseText))

            Result.success(responseText)
        } catch (e: Exception) {
            // Remove the failed user message from history
            if (conversationHistory.isNotEmpty() &&
                conversationHistory.last().role == "user") {
                conversationHistory.removeAt(conversationHistory.lastIndex)
            }
            Result.failure(e)
        }
    }

    /**
     * Clears conversation history to start a fresh conversation.
     */
    fun clearHistory() {
        conversationHistory.clear()
    }

    /**
     * Returns the current conversation history size.
     */
    fun getHistorySize(): Int = conversationHistory.size

    /**
     * Trims conversation history to keep only recent messages.
     */
    private fun trimHistory() {
        while (conversationHistory.size > MAX_HISTORY_SIZE) {
            conversationHistory.removeAt(0)
        }
    }
}
