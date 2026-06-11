package com.commitgpt.template

import com.commitgpt.settings.CommitGptSettings

object TemplateResolver {
    fun resolve(settings: CommitGptSettings, workMode: WorkMode, templateId: String?): String? {
        return when (workMode) {
            WorkMode.PRESET_TEMPLATE -> {
                val id = templateId?.takeIf { it.isNotBlank() }
                    ?: settings.lastPresetTemplateId.takeIf { it.isNotBlank() }
                    ?: settings.defaultPresetTemplateId
                PresetTemplates.findById(id)?.content ?: PresetTemplates.default.content
            }
            WorkMode.CUSTOM_TEMPLATE -> {
                val templates = settings.getCustomTemplates()
                if (templates.isEmpty()) return null
                val id = templateId?.takeIf { it.isNotBlank() }
                    ?: settings.lastCustomTemplateId.takeIf { it.isNotBlank() }
                    ?: settings.defaultCustomTemplateId.takeIf { it.isNotBlank() }
                templates.find { it.id == id }?.content ?: templates.firstOrNull()?.content
            }
            WorkMode.NO_TEMPLATE -> null
        }
    }
}
