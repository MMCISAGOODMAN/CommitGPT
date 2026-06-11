package com.commitgpt.ui

import com.commitgpt.service.CommitGeneratorService
import com.commitgpt.settings.CommitGptSettings
import com.commitgpt.settings.CommitGptSettingsConfigurable
import com.commitgpt.template.PresetTemplates
import com.commitgpt.template.TemplateResolver
import com.commitgpt.template.WorkMode
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.SwingUtilities

private data class TemplateComboItem(val id: String, val label: String) {
    override fun toString(): String = label
}

fun interface CommitMessageSetter {
    fun setCommitMessage(message: String)
    fun getCommitMessage(): String = ""
}

class CommitGptCheckinHandlerFactory : com.intellij.openapi.vcs.checkin.CheckinHandlerFactory() {
    override fun createHandler(
        checkinEnvironment: com.intellij.openapi.vcs.CheckinProjectPanel,
        commitContext: com.intellij.openapi.vcs.changes.CommitContext,
    ): com.intellij.openapi.vcs.checkin.CheckinHandler {
        return CommitGptCheckinHandler(checkinEnvironment)
    }
}

class CommitGptCheckinHandler(
    private val checkinEnvironment: com.intellij.openapi.vcs.CheckinProjectPanel,
) : com.intellij.openapi.vcs.checkin.CheckinHandler() {

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent? {
        val project = checkinEnvironment.project
        val messageSetter = CommitMessageSetter { message ->
            checkinEnvironment.setCommitMessage(message)
        }
        return CommitAssistantRefreshableComponent(project, messageSetter)
    }
}

class CommitAssistantRefreshableComponent(
    private val project: Project,
    private val messageSetter: CommitMessageSetter,
) : RefreshableOnComponent {

    private val panel = CommitAssistantPanel(project, messageSetter)

    override fun getComponent(): JPanel = panel

    override fun refresh() {}

    override fun saveState() {}

    override fun restoreState() {}
}

class CommitAssistantPanel(
    private val project: Project,
    private val messageSetter: CommitMessageSetter,
) : JPanel(BorderLayout()) {

    private val settings: CommitGptSettings = project.service()
    private val generatorService = CommitGeneratorService(project)

    private val workModeCombo = JComboBox(WorkMode.entries.toTypedArray())
    private val templateCombo = JComboBox<TemplateComboItem>()
    private val templatePreviewLabel = JBLabel("")
    private val generateButton = CommitUi.primaryButton("分析变更并生成")
    private val settingsButton = JButton("设置", AllIcons.General.Settings)

    init {
        border = JBUI.Borders.empty(0, 0, 8, 0)
        isOpaque = false

        val card = CommitUi.cardPanel().apply {
            layout = BorderLayout()
        }

        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(10)
            add(
                JBLabel("CommitGPT Assistant").apply {
                    icon = AllIcons.General.Modified
                    font = font.deriveFont(java.awt.Font.BOLD, font.size + 1f)
                },
                BorderLayout.WEST,
            )
            add(
                CommitUi.hintLabel("根据暂存区 diff 智能生成 commit 消息"),
                BorderLayout.EAST,
            )
        }

        workModeCombo.renderer = WorkModeRenderer()
        workModeCombo.selectedItem = settings.lastWorkMode
        CommitUi.comboBoxPreferredWidth(workModeCombo, 200)
        CommitUi.comboBoxPreferredWidth(templateCombo, 280)

        val formPanel = JPanel(GridBagLayout()).apply {
            isOpaque = false
        }
        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(0, 0, 0, 16)
        }

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.4
        formPanel.add(buildFieldGroup("工作模式", workModeCombo), gbc)

        gbc.gridx = 1
        gbc.weightx = 0.6
        gbc.insets = JBUI.emptyInsets()
        formPanel.add(buildFieldGroup("模板选择", templateCombo), gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 2
        gbc.insets = JBUI.insets(4, 0, 0, 0)
        templatePreviewLabel.font = JBUI.Fonts.smallFont()
        templatePreviewLabel.foreground = com.intellij.ui.JBColor.GRAY
        formPanel.add(templatePreviewLabel, gbc)

        workModeCombo.addActionListener {
            updateTemplateCombo()
            updateTemplatePreview()
        }
        templateCombo.addActionListener { updateTemplatePreview() }

        val buttonBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(12)
            generateButton.icon = AllIcons.Actions.Execute
            generateButton.preferredSize = Dimension(160, generateButton.preferredSize.height)
            settingsButton.isBorderPainted = false
            settingsButton.isContentAreaFilled = false
            settingsButton.toolTipText = "打开 CommitGPT Assistant 设置"
            generateButton.addActionListener { onGenerateClicked() }
            settingsButton.addActionListener { openSettings() }
            add(generateButton)
            add(settingsButton)
        }

        val content = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(header, BorderLayout.NORTH)
            add(formPanel, BorderLayout.CENTER)
            add(buttonBar, BorderLayout.SOUTH)
        }
        card.add(content, BorderLayout.CENTER)
        add(card, BorderLayout.CENTER)

        updateTemplateCombo()
        updateTemplatePreview()
    }

    private fun buildFieldGroup(label: String, field: JComboBox<*>): JPanel {
        return JPanel(BorderLayout(0, 4)).apply {
            isOpaque = false
            add(CommitUi.hintLabel(label), BorderLayout.NORTH)
            add(field, BorderLayout.CENTER)
        }
    }

    private fun updateTemplatePreview() {
        val mode = workModeCombo.selectedItem as? WorkMode ?: return
        val templateId = getSelectedTemplateId()
        val content = TemplateResolver.resolve(settings, mode, templateId)

        templatePreviewLabel.text = when {
            mode == WorkMode.NO_TEMPLATE -> "当前模式：AI 自由生成，不使用模板"
            content.isNullOrBlank() -> "未选择有效模板"
            else -> "当前模板：$content"
        }

        if (mode == WorkMode.PRESET_TEMPLATE && content?.contains("{fixed:") != true) {
            val hasCustomFixed = settings.getCustomTemplates().any { it.content.contains("{fixed:") }
            if (hasCustomFixed) {
                templatePreviewLabel.text = (templatePreviewLabel.text ?: "") +
                    "  （含 {fixed:...} 的模板请在「自定义模板」模式下使用）"
            }
        }
    }

    private fun updateTemplateCombo() {
        val mode = workModeCombo.selectedItem as WorkMode
        templateCombo.removeAllItems()

        when (mode) {
            WorkMode.PRESET_TEMPLATE -> {
                templateCombo.isEnabled = true
                PresetTemplates.all.forEach { template ->
                    templateCombo.addItem(TemplateComboItem(template.id, template.name))
                }
                val lastId = settings.lastPresetTemplateId
                selectTemplateById(lastId, PresetTemplates.default.id)
            }
            WorkMode.CUSTOM_TEMPLATE -> {
                val customTemplates = settings.getCustomTemplates()
                if (customTemplates.isEmpty()) {
                    templateCombo.isEnabled = false
                    templateCombo.addItem(TemplateComboItem("", "(无自定义模板，请先在设置中创建)"))
                } else {
                    templateCombo.isEnabled = true
                    customTemplates.forEach { template ->
                        templateCombo.addItem(TemplateComboItem(template.id, template.name))
                    }
                    val lastId = settings.lastCustomTemplateId.ifBlank { settings.defaultCustomTemplateId }
                    selectTemplateById(lastId, customTemplates.first().id)
                }
            }
            WorkMode.NO_TEMPLATE -> {
                templateCombo.isEnabled = false
                templateCombo.addItem(TemplateComboItem("", "(无模板模式)"))
            }
        }
    }

    private fun selectTemplateById(preferredId: String, fallbackId: String) {
        val targetId = preferredId.ifBlank { fallbackId }
        for (i in 0 until templateCombo.itemCount) {
            if (templateCombo.getItemAt(i)?.id == targetId) {
                templateCombo.selectedIndex = i
                return
            }
        }
        if (templateCombo.itemCount > 0) {
            templateCombo.selectedIndex = 0
        }
        updateTemplatePreview()
    }

    private fun getSelectedTemplateId(): String? {
        val mode = workModeCombo.selectedItem as WorkMode
        val selected = templateCombo.selectedItem as? TemplateComboItem ?: return null
        if (selected.id.isBlank()) return null
        return when (mode) {
            WorkMode.PRESET_TEMPLATE -> selected.id
            WorkMode.CUSTOM_TEMPLATE -> selected.id
            WorkMode.NO_TEMPLATE -> null
        }
    }

    fun triggerGenerate() {
        onGenerateClicked()
    }

    private fun onGenerateClicked() {
        val mode = workModeCombo.selectedItem as WorkMode
        if (mode == WorkMode.CUSTOM_TEMPLATE && settings.getCustomTemplates().isEmpty()) {
            Messages.showWarningDialog(
                project,
                "请先在设置中创建自定义模板。",
                "CommitGPT Assistant",
            )
            return
        }

        if (mode == WorkMode.PRESET_TEMPLATE) {
            val hasCustomFixed = settings.getCustomTemplates().any { it.content.contains("{fixed:") }
            if (hasCustomFixed) {
                val result = Messages.showYesNoDialog(
                    project,
                    "你创建了含 {fixed:...} 的自定义模板，但当前是「预设模板」模式，将使用 Conventional 格式。\n\n是否切换到「自定义模板」模式？",
                    "CommitGPT Assistant",
                    Messages.getQuestionIcon(),
                )
                if (result == Messages.YES) {
                    workModeCombo.selectedItem = WorkMode.CUSTOM_TEMPLATE
                    updateTemplateCombo()
                    updateTemplatePreview()
                    return
                }
            }
        }

        generateButton.isEnabled = false
        generateButton.text = "生成中..."
        generateButton.icon = AllIcons.Actions.Refresh

        updateTemplateCombo()

        generatorService.generateCommitMessage(
            workMode = mode,
            templateId = getSelectedTemplateId(),
            onSuccess = { message ->
                SwingUtilities.invokeLater {
                    resetGenerateButton()
                    showPreviewDialog(message)
                }
            },
            onError = { error ->
                SwingUtilities.invokeLater {
                    resetGenerateButton()
                    Messages.showErrorDialog(project, error, "CommitGPT Assistant 错误")
                }
            },
        )
    }

    private fun resetGenerateButton() {
        generateButton.isEnabled = true
        generateButton.text = "分析变更并生成"
        generateButton.icon = AllIcons.Actions.Execute
    }

    private fun showPreviewDialog(message: String) {
        val dialog = CommitPreviewDialog(project, message)
        if (dialog.showAndGet()) {
            messageSetter.setCommitMessage(dialog.getEditedMessage())
        }
    }

    private fun openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, CommitGptSettingsConfigurable::class.java)
        updateTemplateCombo()
        updateTemplatePreview()
    }

    private class WorkModeRenderer : javax.swing.DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: javax.swing.JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): java.awt.Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is WorkMode) {
                text = value.displayName
            }
            return component
        }
    }
}

class CommitPreviewDialog(
    project: Project,
    private val generatedMessage: String,
) : DialogWrapper(project, true) {

    private val messageArea = JBTextArea(generatedMessage, 12, 72)

    init {
        title = "CommitGPT Assistant — 预览生成结果"
        init()
    }

    override fun createCenterPanel(): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            preferredSize = JBUI.size(560, 340)
        }

        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(8)
            add(
                JBLabel("确认后将填入 Commit 消息输入框，你可以继续编辑。").apply {
                    icon = AllIcons.General.Information
                },
                BorderLayout.CENTER,
            )
        }

        messageArea.lineWrap = true
        messageArea.wrapStyleWord = true
        messageArea.font = CommitUi.monospacedAreaFont()
        messageArea.border = JBUI.Borders.empty(8)

        val editorWrapper = CommitUi.cardPanel().apply {
            layout = BorderLayout()
            add(JBScrollPane(messageArea), BorderLayout.CENTER)
        }

        panel.add(header, BorderLayout.NORTH)
        panel.add(editorWrapper, BorderLayout.CENTER)
        return panel
    }

    fun getEditedMessage(): String = messageArea.text.trim()
}
