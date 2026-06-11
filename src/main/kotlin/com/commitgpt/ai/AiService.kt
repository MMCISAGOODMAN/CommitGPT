package com.commitgpt.ai

import com.commitgpt.ai.model.AiProviderConfig
import com.commitgpt.ai.model.AiProviderType
import com.commitgpt.ai.model.CommitJsonResponse
import com.commitgpt.settings.CommitGptSettings
import com.commitgpt.template.TemplateEngine
import com.commitgpt.template.WorkMode
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class AiServiceException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Service(Service.Level.PROJECT)
class AiService(private val project: Project) {
    private val log = Logger.getInstance(AiService::class.java)
    private val gson = Gson()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    fun generateCommitMessage(
        diff: String,
        workMode: WorkMode,
        templateContent: String?,
        author: String,
        branch: String,
        settings: CommitGptSettings,
    ): String {
        val config = settings.getAiProviderConfig()
        val systemPrompt = PromptBuilder.buildSystemPrompt(settings, workMode, templateContent)
        val userPrompt = PromptBuilder.buildUserPrompt(diff)

        val rawResponse = when (config.providerType) {
            AiProviderType.OLLAMA -> callOllama(config, systemPrompt, userPrompt)
            else -> callChatCompletions(config, systemPrompt, userPrompt)
        }

        return when (workMode) {
            WorkMode.NO_TEMPLATE -> ResponseParser.parseFreeTextResponse(rawResponse)
            else -> {
                val json = ResponseParser.parseJsonResponse(rawResponse)
                TemplateEngine.fillTemplate(
                    template = templateContent ?: "{subject}",
                    aiResponse = json,
                    author = author,
                    branch = branch,
                )
            }
        }
    }

    fun testConnection(settings: CommitGptSettings): String {
        val config = settings.getAiProviderConfig()
        val testPrompt = "Reply with exactly: OK"
        return when (config.providerType) {
            AiProviderType.OLLAMA -> callOllama(config, "You are a test assistant.", testPrompt)
            else -> callChatCompletions(config, "You are a test assistant.", testPrompt)
        }
    }

    private fun callChatCompletions(
        config: AiProviderConfig,
        systemPrompt: String,
        userPrompt: String,
    ): String {
        if (config.apiUrl.isBlank()) {
            throw AiServiceException("API URL 未配置")
        }
        if (config.providerType != AiProviderType.OLLAMA && config.apiKey.isBlank()) {
            throw AiServiceException("API Key 未配置")
        }

        val body = JsonObject().apply {
            addProperty("model", config.model)
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userPrompt),
            )))
            addProperty("temperature", 0.3)
        }

        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(config.apiUrl))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))

        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
        }

        return executeRequest(requestBuilder.build()) { responseBody ->
            parseChatCompletionResponse(responseBody)
        }
    }

    private fun callOllama(
        config: AiProviderConfig,
        systemPrompt: String,
        userPrompt: String,
    ): String {
        val url = config.apiUrl.ifBlank { AiProviderType.OLLAMA.defaultUrl }
        val fullPrompt = "$systemPrompt\n\n$userPrompt"

        val body = JsonObject().apply {
            addProperty("model", config.model.ifBlank { "llama3" })
            addProperty("prompt", fullPrompt)
            addProperty("stream", false)
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()

        return executeRequest(request) { responseBody ->
            val json = JsonParser.parseString(responseBody).asJsonObject
            json.get("response")?.asString
                ?: throw AiServiceException("Ollama 响应格式无效: $responseBody")
        }
    }

    private fun parseChatCompletionResponse(responseBody: String): String {
        val json = JsonParser.parseString(responseBody).asJsonObject
        if (json.has("error")) {
            val error = json.getAsJsonObject("error")
            throw AiServiceException(error.get("message")?.asString ?: "API 返回错误")
        }
        val choices = json.getAsJsonArray("choices")
            ?: throw AiServiceException("API 响应缺少 choices 字段")
        if (choices.isEmpty) {
            throw AiServiceException("API 返回空的 choices")
        }
        return choices[0].asJsonObject
            .getAsJsonObject("message")
            .get("content")
            .asString
    }

    private fun <T> executeRequest(request: HttpRequest, parser: (String) -> T): T {
        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw AiServiceException("API 请求失败 (${response.statusCode()}): ${response.body()}")
            }
            return parser(response.body())
        } catch (e: AiServiceException) {
            throw e
        } catch (e: Exception) {
            log.warn("AI service call failed", e)
            throw AiServiceException("AI 调用失败: ${e.message}", e)
        }
    }
}
