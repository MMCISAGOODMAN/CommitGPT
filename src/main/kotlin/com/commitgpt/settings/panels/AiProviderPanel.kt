package com.commitgpt.settings.panels

import com.commitgpt.ai.AiService
import com.commitgpt.ai.AiServiceException
import com.commitgpt.ai.model.AiProviderType
import com.commitgpt.settings.CommitGptSettings
import com.commitgpt.ui.CommitUi
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel

class AiProviderPanel(private val project: Project) : JPanel(BorderLayout()) {
    val providerCombo = JComboBox(AiProviderType.entries.toTypedArray())
    val apiUrlField = JBTextField()
    val modelField = JBTextField()
    val apiKeyField = JBPasswordField()
    val customPromptArea = JBTextArea(8, 40)
    private val testConnectionButton = CommitUi.primaryButton("测试连接")

    init {
        border = JBUI.Borders.empty(8, 16)

        providerCombo.renderer = object : javax.swing.DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: javax.swing.JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): java.awt.Component {
                val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is AiProviderType) text = value.displayName
                return c
            }
        }

        providerCombo.addActionListener { onProviderChanged() }

        customPromptArea.lineWrap = true
        customPromptArea.wrapStyleWord = true
        customPromptArea.font = CommitUi.monospacedAreaFont()

        CommitUi.comboBoxPreferredWidth(providerCombo, 280)
        apiUrlField.preferredSize = JBUI.size(420, apiUrlField.preferredSize.height)
        modelField.preferredSize = JBUI.size(280, modelField.preferredSize.height)
        apiKeyField.preferredSize = JBUI.size(420, apiKeyField.preferredSize.height)

        val intro = JBLabel(
            "<html><body style='width:520px'>选择 AI 提供商并填写连接信息。API Key 仅保存在本地，不会上传到插件作者服务器。</body></html>",
        )

        val connectionForm = FormBuilder.createFormBuilder()
            .addComponent(CommitUi.sectionTitle("连接配置"))
            .addLabeledComponent(JBLabel("AI 提供商:"), providerCombo)
            .addLabeledComponent(JBLabel("API 地址:"), apiUrlField)
            .addLabeledComponent(JBLabel("模型:"), modelField)
            .addLabeledComponent(JBLabel("API Key:"), apiKeyField)
            .panel

        val promptForm = FormBuilder.createFormBuilder()
            .addComponent(CommitUi.sectionTitle("Prompt 配置"))
            .addLabeledComponent(JBLabel("自定义 Prompt:"), JBScrollPane(customPromptArea))
            .addComponent(CommitUi.hintLabel("留空则使用内置 Prompt。自定义 Prompt 会覆盖默认行为。"))
            .panel

        val content = FormBuilder.createFormBuilder()
            .addComponent(intro)
            .addComponent(connectionForm)
            .addComponent(promptForm)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        testConnectionButton.icon = AllIcons.RunConfigurations.TestState.Run
        testConnectionButton.addActionListener { onTestConnection() }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(12)
            add(testConnectionButton)
        }

        add(JBScrollPane(content), BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun onProviderChanged() {
        val provider = providerCombo.selectedItem as AiProviderType
        if (provider != AiProviderType.CUSTOM) {
            apiUrlField.text = provider.defaultUrl
            modelField.text = provider.defaultModel
        }
        apiKeyField.isEnabled = provider != AiProviderType.OLLAMA
    }

    private fun onTestConnection() {
        val settings = project.service<CommitGptSettings>()
        applyToSettings(settings)

        testConnectionButton.isEnabled = false
        testConnectionButton.text = "测试中..."
        testConnectionButton.icon = AllIcons.Actions.Refresh

        Thread {
            try {
                val response = project.service<AiService>().testConnection(settings)
                javax.swing.SwingUtilities.invokeLater {
                    resetTestButton()
                    Messages.showInfoMessage(
                        project,
                        "连接成功！\n响应: ${response.take(100)}",
                        "CommitGPT Assistant",
                    )
                }
            } catch (e: AiServiceException) {
                javax.swing.SwingUtilities.invokeLater {
                    resetTestButton()
                    Messages.showErrorDialog(project, e.message ?: "连接失败", "CommitGPT Assistant")
                }
            } catch (e: Exception) {
                javax.swing.SwingUtilities.invokeLater {
                    resetTestButton()
                    Messages.showErrorDialog(project, "连接失败: ${e.message}", "CommitGPT Assistant")
                }
            }
        }.start()
    }

    private fun resetTestButton() {
        testConnectionButton.isEnabled = true
        testConnectionButton.text = "测试连接"
        testConnectionButton.icon = AllIcons.RunConfigurations.TestState.Run
    }

    fun applyToSettings(settings: CommitGptSettings) {
        val provider = providerCombo.selectedItem as AiProviderType
        settings.aiProviderType = provider
        settings.apiUrl = apiUrlField.text.trim()
        settings.model = modelField.text.trim()
        settings.apiKey = String(apiKeyField.password)
        settings.customPrompt = customPromptArea.text
    }

    fun loadFromSettings(settings: CommitGptSettings) {
        providerCombo.selectedItem = settings.aiProviderType
        apiUrlField.text = settings.apiUrl
        modelField.text = settings.model
        apiKeyField.text = settings.apiKey
        customPromptArea.text = settings.customPrompt
        onProviderChanged()
    }
}
