package com.commitgpt.ai.model

enum class AiProviderType(val displayName: String, val defaultUrl: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo"),
    QWEN("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-turbo"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-chat"),
    OLLAMA("Ollama", "http://localhost:11434/api/generate", "llama3"),
    CUSTOM("自定义", "", "");

    companion object {
        fun fromName(name: String?): AiProviderType =
            entries.find { it.name == name } ?: OPENAI
    }
}

data class AiProviderConfig(
    val providerType: AiProviderType = AiProviderType.OPENAI,
    val apiUrl: String = AiProviderType.OPENAI.defaultUrl,
    val model: String = AiProviderType.OPENAI.defaultModel,
    val apiKey: String = "",
    val customPrompt: String = "",
)
