package com.commitgpt.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project

class CommitGptSettingsConfigurableProvider : com.intellij.openapi.options.ConfigurableProvider() {
    override fun createConfigurable(): Configurable? {
        val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
            ?: return null
        return CommitGptSettingsConfigurable(project)
    }
}
