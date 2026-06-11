package com.commitgpt.git

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

class GitDiffServiceException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Service(Service.Level.PROJECT)
class GitDiffService(private val project: Project) {
    private val log = Logger.getInstance(GitDiffService::class.java)

    fun getStagedDiff(): String {
        val repository = getGitRepository()
            ?: throw GitDiffServiceException("当前项目未找到 Git 仓库")

        val diff = getFullStagedDiff(repository)
        if (diff.isBlank()) {
            throw GitDiffServiceException("暂存区没有变更，请先 stage 文件后再生成 commit 消息")
        }
        return truncateDiff(diff)
    }

    fun getGitAuthor(): String {
        return try {
            val repository = getGitRepository() ?: return System.getProperty("user.name", "")
            val handler = GitLineHandler(project, repository.root, GitCommand.CONFIG)
            handler.addParameters("user.name")
            val result = Git.getInstance().runCommand(handler)
            if (result.success()) {
                result.outputAsJoinedString.trim().ifBlank { System.getProperty("user.name", "") }
            } else {
                System.getProperty("user.name", "")
            }
        } catch (e: Exception) {
            log.warn("Failed to get git author", e)
            System.getProperty("user.name", "")
        }
    }

    fun getCurrentBranch(): String {
        return getGitRepository()?.currentBranchName ?: "unknown"
    }

    fun hasStagedChanges(): Boolean {
        val repository = getGitRepository() ?: return false
        return getFullStagedDiff(repository).isNotBlank()
    }

    private fun getGitRepository(): GitRepository? {
        return GitRepositoryManager.getInstance(project).repositories.firstOrNull()
    }

    private fun getFullStagedDiff(repository: GitRepository): String {
        val handler = GitLineHandler(project, repository.root, GitCommand.DIFF)
        handler.addParameters("--cached")
        handler.setSilent(true)
        val result = Git.getInstance().runCommand(handler)
        return if (result.success()) result.outputAsJoinedString else ""
    }

    private fun truncateDiff(diff: String, maxLength: Int = MAX_DIFF_LENGTH): String {
        if (diff.length <= maxLength) return diff
        return diff.take(maxLength) + "\n\n... (diff truncated due to size limit)"
    }

    companion object {
        private const val MAX_DIFF_LENGTH = 50000
    }
}
