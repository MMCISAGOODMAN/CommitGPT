package com.commitgpt.service

import com.commitgpt.ai.AiService
import com.commitgpt.ai.AiServiceException
import com.commitgpt.git.GitDiffService
import com.commitgpt.git.GitDiffServiceException
import com.commitgpt.settings.CommitGptSettings
import com.commitgpt.template.TemplateResolver
import com.commitgpt.template.WorkMode
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class CommitGeneratorService(private val project: Project) {
    private val settings: CommitGptSettings get() = project.service()
    private val gitDiffService: GitDiffService get() = project.service()
    private val aiService: AiService get() = project.service()

    fun generateCommitMessage(
        workMode: WorkMode,
        templateId: String?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "CommitGPT Assistant: 正在分析变更并生成 commit 消息...",
            true,
        ) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "读取暂存区 diff..."
                    val diff = gitDiffService.getStagedDiff()

                    indicator.text = "调用 AI 生成 commit 消息..."
                    val templateContent = TemplateResolver.resolve(settings, workMode, templateId)
                    if (workMode != WorkMode.NO_TEMPLATE && templateContent.isNullOrBlank()) {
                        onError("未找到可用模板，请先在设置中创建或选择模板。")
                        return
                    }

                    val message = aiService.generateCommitMessage(
                        diff = diff,
                        workMode = workMode,
                        templateContent = templateContent,
                        author = gitDiffService.getGitAuthor(),
                        branch = gitDiffService.getCurrentBranch(),
                        settings = settings,
                    )

                    settings.lastWorkMode = workMode
                    when (workMode) {
                        WorkMode.PRESET_TEMPLATE -> settings.lastPresetTemplateId = templateId ?: ""
                        WorkMode.CUSTOM_TEMPLATE -> settings.lastCustomTemplateId = templateId ?: ""
                        WorkMode.NO_TEMPLATE -> {}
                    }

                    onSuccess(message)
                } catch (e: GitDiffServiceException) {
                    onError(e.message ?: "Git diff 读取失败")
                } catch (e: AiServiceException) {
                    onError(e.message ?: "AI 调用失败")
                } catch (e: Exception) {
                    onError("生成失败: ${e.message}")
                }
            }
        })
    }
}
