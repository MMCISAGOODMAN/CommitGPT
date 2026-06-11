package com.commitgpt.settings

import com.commitgpt.settings.panels.AiProviderPanel
import com.commitgpt.settings.panels.TemplateManagementPanel
import com.commitgpt.template.WorkMode
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

class CommitGptSettingsComponent(private val project: Project) : JPanel(BorderLayout()) {
    private val templatePanel = TemplateManagementPanel()
    val aiProviderPanel = AiProviderPanel(project)

    init {
        border = JBUI.Borders.emptyTop(4)

        val cardLayout = CardLayout()
        val cards = JPanel(cardLayout).apply {
            add(templatePanel, SettingsTab.TEMPLATES.name)
            add(aiProviderPanel, SettingsTab.AI.name)
        }

        val navigator = SettingsSidebarNavigator(cards, cardLayout)
        add(navigator.component, BorderLayout.CENTER)

        templatePanel.onDefaultChanged = { id ->
            val settings = project.getService(CommitGptSettings::class.java)
            settings.defaultCustomTemplateId = id
            settings.defaultWorkMode = WorkMode.CUSTOM_TEMPLATE
            settings.lastWorkMode = WorkMode.CUSTOM_TEMPLATE
            settings.lastCustomTemplateId = id
        }

        templatePanel.onTemplatesChanged = {
            project.getService(CommitGptSettings::class.java)
                .setCustomTemplates(templatePanel.getCustomTemplates())
        }
    }

    fun loadSettings(settings: CommitGptSettings) {
        templatePanel.setCustomTemplates(settings.getCustomTemplates())
        aiProviderPanel.loadFromSettings(settings)
    }

    fun applySettings(settings: CommitGptSettings) {
        settings.setCustomTemplates(templatePanel.getCustomTemplates())
        aiProviderPanel.applyToSettings(settings)
    }

    fun isModified(settings: CommitGptSettings): Boolean {
        if (templatePanel.getCustomTemplates() != settings.getCustomTemplates()) return true

        val ai = aiProviderPanel
        if (ai.providerCombo.selectedItem != settings.aiProviderType) return true
        if (ai.apiUrlField.text.trim() != settings.apiUrl) return true
        if (ai.modelField.text.trim() != settings.model) return true
        if (String(ai.apiKeyField.password) != settings.apiKey) return true
        if (ai.customPromptArea.text != settings.customPrompt) return true

        return false
    }
}
