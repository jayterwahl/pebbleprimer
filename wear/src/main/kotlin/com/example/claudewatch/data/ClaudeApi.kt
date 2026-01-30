package com.example.claudewatch.data

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Data classes for Claude API request/response
 */
data class MessageRequest(
    val model: String = "claude-sonnet-4-20250514",
    val max_tokens: Int = 512,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class MessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    val stop_reason: String?,
    val usage: Usage?
)

data class ContentBlock(
    val type: String,
    val text: String
)

data class Usage(
    val input_tokens: Int,
    val output_tokens: Int
)

/**
 * Retrofit interface for Claude API
 */
interface ClaudeApi {
    @POST("v1/messages")
    @Headers("anthropic-version: 2023-06-01")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Body request: MessageRequest
    ): MessageResponse
}
