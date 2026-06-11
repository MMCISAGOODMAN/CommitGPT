package com.commitgpt.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class CommitGptSettingsConfigurable(private val project: Project) : Configurable {
    private var component: CommitGptSettingsComponent? = null

    override fun getDisplayName(): String = "CommitGPT"

    override fun createComponent(): JComponent {
        component = CommitGptSettingsComponent(project).also {
            it.loadSettings(project.getService(CommitGptSettings::class.java))
        }
        return component!!
    }

    override fun isModified(): Boolean {
        val comp = component ?: return false
        return comp.isModified(project.getService(CommitGptSettings::class.java))
    }

    override fun apply() {
        component?.applySettings(project.getService(CommitGptSettings::class.java))
    }

    override fun reset() {
        component?.loadSettings(project.getService(CommitGptSettings::class.java))
    }

    override fun disposeUIResources() {
        component = null
    }
}
