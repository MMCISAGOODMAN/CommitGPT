package com.commitgpt.settings

import com.commitgpt.ai.model.AiProviderConfig
import com.commitgpt.ai.model.AiProviderType
import com.commitgpt.template.CustomTemplate
import com.commitgpt.template.PresetTemplates
import com.commitgpt.template.WorkMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Transient
import java.util.UUID

@Service(Service.Level.PROJECT)
class CommitGptSettings(private val project: Project) {
    private val properties = com.intellij.ide.util.PropertiesComponent.getInstance(project)
    private val gson = Gson()

    var defaultWorkMode: WorkMode
        get() = WorkMode.fromName(properties.getValue(KEY_DEFAULT_WORK_MODE))
        set(value) = properties.setValue(KEY_DEFAULT_WORK_MODE, value.name)

    var defaultPresetTemplateId: String
        get() = properties.getValue(KEY_DEFAULT_PRESET_TEMPLATE, PresetTemplates.default.id)
        set(value) = properties.setValue(KEY_DEFAULT_PRESET_TEMPLATE, value)

    var defaultCustomTemplateId: String
        get() = properties.getValue(KEY_DEFAULT_CUSTOM_TEMPLATE, "")
        set(value) = properties.setValue(KEY_DEFAULT_CUSTOM_TEMPLATE, value)

    var lastWorkMode: WorkMode
        get() = WorkMode.fromName(properties.getValue(KEY_LAST_WORK_MODE, defaultWorkMode.name))
        set(value) = properties.setValue(KEY_LAST_WORK_MODE, value.name)

    var lastPresetTemplateId: String
        get() = properties.getValue(KEY_LAST_PRESET_TEMPLATE, defaultPresetTemplateId)
        set(value) = properties.setValue(KEY_LAST_PRESET_TEMPLATE, value)

    var lastCustomTemplateId: String
        get() = properties.getValue(KEY_LAST_CUSTOM_TEMPLATE, defaultCustomTemplateId)
        set(value) = properties.setValue(KEY_LAST_CUSTOM_TEMPLATE, value)

    var aiProviderType: AiProviderType
        get() = AiProviderType.fromName(properties.getValue(KEY_AI_PROVIDER))
        set(value) = properties.setValue(KEY_AI_PROVIDER, value.name)

    var apiUrl: String
        get() = properties.getValue(KEY_API_URL, aiProviderType.defaultUrl)
        set(value) = properties.setValue(KEY_API_URL, value)

    var model: String
        get() = properties.getValue(KEY_MODEL, aiProviderType.defaultModel)
        set(value) = properties.setValue(KEY_MODEL, value)

    var apiKey: String
        get() = properties.getValue(KEY_API_KEY, "")
        set(value) = properties.setValue(KEY_API_KEY, value)

    var customPrompt: String
        get() = properties.getValue(KEY_CUSTOM_PROMPT, "")
        set(value) = properties.setValue(KEY_CUSTOM_PROMPT, value)

    fun getCustomTemplates(): List<CustomTemplate> {
        val json = properties.getValue(KEY_CUSTOM_TEMPLATES, "[]")
        return try {
            val type = object : TypeToken<List<CustomTemplate>>() {}.type
            gson.fromJson<List<CustomTemplate>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setCustomTemplates(templates: List<CustomTemplate>) {
        properties.setValue(KEY_CUSTOM_TEMPLATES, gson.toJson(templates))
    }

    fun addCustomTemplate(name: String, content: String): CustomTemplate {
        val template = CustomTemplate(
            id = UUID.randomUUID().toString(),
            name = name,
            content = content,
        )
        setCustomTemplates(getCustomTemplates() + template)
        return template
    }

    fun updateCustomTemplate(template: CustomTemplate) {
        setCustomTemplates(getCustomTemplates().map {
            if (it.id == template.id) template else it
        })
    }

    fun deleteCustomTemplate(id: String) {
        setCustomTemplates(getCustomTemplates().filter { it.id != id })
        if (defaultCustomTemplateId == id) {
            defaultCustomTemplateId = ""
        }
        if (lastCustomTemplateId == id) {
            lastCustomTemplateId = ""
        }
    }

    fun getAiProviderConfig(): AiProviderConfig = AiProviderConfig(
        providerType = aiProviderType,
        apiUrl = apiUrl,
        model = model,
        apiKey = apiKey,
        customPrompt = customPrompt,
    )

    fun applyAiProviderConfig(config: AiProviderConfig) {
        aiProviderType = config.providerType
        apiUrl = config.apiUrl
        model = config.model
        apiKey = config.apiKey
        customPrompt = config.customPrompt
    }

    fun resetAiProviderDefaults() {
        apiUrl = aiProviderType.defaultUrl
        model = aiProviderType.defaultModel
    }

    companion object {
        private const val KEY_DEFAULT_WORK_MODE = "commitgpt.defaultWorkMode"
        private const val KEY_DEFAULT_PRESET_TEMPLATE = "commitgpt.defaultPresetTemplate"
        private const val KEY_DEFAULT_CUSTOM_TEMPLATE = "commitgpt.defaultCustomTemplate"
        private const val KEY_LAST_WORK_MODE = "commitgpt.lastWorkMode"
        private const val KEY_LAST_PRESET_TEMPLATE = "commitgpt.lastPresetTemplate"
        private const val KEY_LAST_CUSTOM_TEMPLATE = "commitgpt.lastCustomTemplate"
        private const val KEY_AI_PROVIDER = "commitgpt.aiProvider"
        private const val KEY_API_URL = "commitgpt.apiUrl"
        private const val KEY_MODEL = "commitgpt.model"
        private const val KEY_API_KEY = "commitgpt.apiKey"
        private const val KEY_CUSTOM_PROMPT = "commitgpt.customPrompt"
        private const val KEY_CUSTOM_TEMPLATES = "commitgpt.customTemplates"
    }
}
