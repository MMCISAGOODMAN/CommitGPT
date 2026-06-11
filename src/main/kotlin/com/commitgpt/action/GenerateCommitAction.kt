package com.commitgpt.action

import com.commitgpt.service.CommitGeneratorService
import com.commitgpt.settings.CommitGptSettings
import com.commitgpt.template.WorkMode
import com.commitgpt.ui.CommitPreviewDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.ui.Refreshable
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.SwingUtilities

class GenerateCommitAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val checkinPanel = e.getData(Refreshable.PANEL_KEY) as? CheckinProjectPanel

        if (checkinPanel == null) {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Commit")
            toolWindow?.show(null)
            Messages.showInfoMessage(
                project,
                "请在 Commit 窗口中使用 AI Commit Assistant 面板生成 commit 消息。\n快捷键: Ctrl+Alt+G",
                "CommitGPT Assistant",
            )
            return
        }

        val settings = project.service<CommitGptSettings>()
        val generator = CommitGeneratorService(project)

        generator.generateCommitMessage(
            workMode = settings.lastWorkMode,
            templateId = when (settings.lastWorkMode) {
                WorkMode.PRESET_TEMPLATE -> settings.lastPresetTemplateId
                WorkMode.CUSTOM_TEMPLATE -> settings.lastCustomTemplateId
                WorkMode.NO_TEMPLATE -> null
            },
            onSuccess = { message ->
                SwingUtilities.invokeLater {
                    val dialog = CommitPreviewDialog(project, message)
                    if (dialog.showAndGet()) {
                        checkinPanel.setCommitMessage(dialog.getEditedMessage())
                    }
                }
            },
            onError = { error ->
                SwingUtilities.invokeLater {
                    Messages.showErrorDialog(project, error, "CommitGPT Assistant 错误")
                }
            },
        )
    }
}
