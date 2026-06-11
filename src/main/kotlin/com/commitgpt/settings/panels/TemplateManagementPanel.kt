package com.commitgpt.settings.panels

import com.commitgpt.ai.model.CommitJsonResponse
import com.commitgpt.template.CustomTemplate
import com.commitgpt.template.PresetTemplates
import com.commitgpt.template.TemplateEngine
import com.commitgpt.ui.CommitUi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener

class TemplateManagementPanel : JPanel(BorderLayout()) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val presetListModel = DefaultListModel<PresetItem>()
    private val customListModel = DefaultListModel<CustomTemplate>()
    private val customList = JList(customListModel)

    val nameField = JBTextField()
    val contentArea = JBTextArea(8, 40)
    val testJsonArea = JBTextArea(6, 40)
    val previewArea = JBTextArea(6, 40)

    private val addButton = JButton("添加", AllIcons.General.Add)
    private val saveButton = JButton("保存", AllIcons.Actions.MenuSaveall)
    private val deleteButton = JButton("删除", AllIcons.General.Remove)
    private val setDefaultButton = JButton("设为默认", AllIcons.Actions.Checked)
    private val previewButton = CommitUi.primaryButton("预览填充结果")

    var onDefaultChanged: ((String) -> Unit)? = null
    var onTemplatesChanged: (() -> Unit)? = null

    init {
        border = JBUI.Borders.empty(8)

        PresetTemplates.all.forEach { preset ->
            presetListModel.addElement(PresetItem(preset.name, preset.content))
        }

        customList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        customList.cellRenderer = CustomTemplateRenderer()
        customList.addListSelectionListener(ListSelectionListener {
            if (!it.valueIsAdjusting) loadSelectedTemplate()
        })

        val leftPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyRight(8)
        }

        val presetPanel = JPanel(BorderLayout()).apply {
            border = CommitUi.titledBorder("预设模板（只读）")
            add(
                JBScrollPane(JList(presetListModel).apply {
                    isEnabled = false
                    cellRenderer = PresetTemplateRenderer()
                    fixedCellHeight = 44
                }),
                BorderLayout.CENTER,
            )
        }

        val customPanel = JPanel(BorderLayout()).apply {
            border = CommitUi.titledBorder("自定义模板")
            add(JBScrollPane(customList), BorderLayout.CENTER)
            add(
                CommitUi.toolbarPanel(addButton, deleteButton),
                BorderLayout.SOUTH,
            )
        }

        val splitLeft = JSplitPane(JSplitPane.VERTICAL_SPLIT, presetPanel, customPanel).apply {
            resizeWeight = 0.42
            dividerSize = JBUI.scale(6)
        }
        leftPanel.add(splitLeft, BorderLayout.CENTER)

        contentArea.lineWrap = true
        contentArea.wrapStyleWord = true
        contentArea.font = CommitUi.monospacedAreaFont()
        testJsonArea.lineWrap = true
        testJsonArea.font = CommitUi.monospacedAreaFont()
        previewArea.lineWrap = true
        previewArea.font = CommitUi.monospacedAreaFont()
        previewArea.isEditable = false
        previewArea.background = UIUtil.getToolTipBackground()

        nameField.preferredSize = JBUI.size(320, nameField.preferredSize.height)

        val variableHint = JBLabel(CommitUi.variableHintHtml())

        val editorPanel = FormBuilder.createFormBuilder()
            .addComponent(CommitUi.sectionTitle("模板编辑"))
            .addLabeledComponent(JBLabel("模板名称:"), nameField)
            .addLabeledComponent(JBLabel("模板内容:"), JBScrollPane(contentArea))
            .addComponent(variableHint)
            .addComponent(CommitUi.sectionTitle("测试与预览"))
            .addLabeledComponent(JBLabel("测试 JSON:"), JBScrollPane(testJsonArea))
            .addLabeledComponent(JBLabel("预览结果:"), JBScrollPane(previewArea))
            .panel

        val editorButtons = CommitUi.toolbarPanel(saveButton, setDefaultButton, previewButton)

        val rightPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyLeft(8)
            add(editorPanel, BorderLayout.CENTER)
            add(editorButtons, BorderLayout.SOUTH)
        }

        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel).apply {
            resizeWeight = 0.34
            dividerSize = JBUI.scale(6)
        }
        add(split, BorderLayout.CENTER)

        testJsonArea.text = gson.toJson(
            CommitJsonResponse(
                type = "feat",
                scope = "user-service",
                subject = "add phone number validation",
                body = "Implemented regex validation for phone numbers",
                footer = "Closes #123",
            ),
        )

        addButton.addActionListener { onAddClicked() }
        saveButton.addActionListener { onSaveClicked() }
        deleteButton.addActionListener { onDeleteClicked() }
        setDefaultButton.addActionListener { onSetDefaultClicked() }
        previewButton.addActionListener { onPreviewClicked() }

        updateEditorEnabled(false)
    }

    fun setCustomTemplates(templates: List<CustomTemplate>) {
        customListModel.clear()
        templates.forEach { customListModel.addElement(it) }
        if (templates.isNotEmpty()) {
            customList.selectedIndex = 0
        } else {
            clearEditor()
        }
    }

    fun getCustomTemplates(): List<CustomTemplate> {
        val result = mutableListOf<CustomTemplate>()
        for (i in 0 until customListModel.size) {
            result.add(customListModel.getElementAt(i))
        }
        return result
    }

    private fun loadSelectedTemplate() {
        val selected = customList.selectedValue
        if (selected != null) {
            nameField.text = selected.name
            contentArea.text = selected.content
            updateEditorEnabled(true)
        } else {
            clearEditor()
        }
    }

    private fun clearEditor() {
        nameField.text = ""
        contentArea.text = ""
        previewArea.text = ""
        updateEditorEnabled(false)
    }

    private fun updateEditorEnabled(enabled: Boolean) {
        nameField.isEnabled = enabled
        contentArea.isEnabled = enabled
        saveButton.isEnabled = enabled
        deleteButton.isEnabled = enabled
        setDefaultButton.isEnabled = enabled
    }

    private fun onAddClicked() {
        val template = CustomTemplate(
            id = java.util.UUID.randomUUID().toString(),
            name = "新模板",
            content = "{fixed:bugfix}: {subject}",
        )
        customListModel.addElement(template)
        customList.selectedIndex = customListModel.size - 1
        onTemplatesChanged?.invoke()
    }

    private fun onSaveClicked() {
        val selected = customList.selectedValue ?: return
        val updated = selected.copy(
            name = nameField.text.trim().ifBlank { "未命名模板" },
            content = contentArea.text,
        )
        val index = customList.selectedIndex
        customListModel.set(index, updated)
        onTemplatesChanged?.invoke()
    }

    private fun onDeleteClicked() {
        val index = customList.selectedIndex
        if (index < 0) return
        customListModel.remove(index)
        if (customListModel.isEmpty) {
            clearEditor()
        } else {
            customList.selectedIndex = minOf(index, customListModel.size - 1)
        }
        onTemplatesChanged?.invoke()
        onDefaultChanged?.invoke("")
    }

    private fun onSetDefaultClicked() {
        val selected = customList.selectedValue ?: return
        onDefaultChanged?.invoke(selected.id)
    }

    private fun onPreviewClicked() {
        try {
            val json = gson.fromJson(testJsonArea.text.trim(), CommitJsonResponse::class.java)
            previewArea.text = TemplateEngine.fillWithTestJson(contentArea.text, json)
        } catch (e: Exception) {
            previewArea.text = "JSON 解析失败: ${e.message}"
        }
    }

    private data class PresetItem(val name: String, val content: String)

    private class PresetTemplateRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: javax.swing.JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val panel = JPanel(BorderLayout()).apply {
                border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
                isOpaque = true
                if (isSelected) {
                    background = list?.selectionBackground
                } else {
                    background = list?.background
                }
            }
            if (value is PresetItem) {
                val nameLabel = JBLabel(value.name).apply {
                    font = font.deriveFont(Font.BOLD)
                    foreground = if (isSelected) list?.selectionForeground else list?.foreground
                }
                val contentLabel = JBLabel(truncate(value.content, 48)).apply {
                    font = JBUI.Fonts.smallFont()
                    foreground = if (isSelected) list?.selectionForeground else JBColor.GRAY
                }
                panel.add(nameLabel, BorderLayout.NORTH)
                panel.add(contentLabel, BorderLayout.CENTER)
            }
            return panel
        }

        private fun truncate(text: String, max: Int): String =
            if (text.length <= max) text else text.take(max) + "…"
    }

    private class CustomTemplateRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: javax.swing.JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val panel = JPanel(BorderLayout()).apply {
                border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
                isOpaque = true
                if (isSelected) {
                    background = list?.selectionBackground
                } else {
                    background = list?.background
                }
            }
            if (value is CustomTemplate) {
                val nameLabel = JBLabel(value.name).apply {
                    font = font.deriveFont(Font.BOLD)
                    foreground = if (isSelected) list?.selectionForeground else list?.foreground
                }
                val contentLabel = JBLabel(truncate(value.content, 52)).apply {
                    font = JBUI.Fonts.smallFont()
                    foreground = if (isSelected) list?.selectionForeground else JBColor.GRAY
                }
                panel.add(nameLabel, BorderLayout.NORTH)
                panel.add(contentLabel, BorderLayout.CENTER)
            }
            return panel
        }

        private fun truncate(text: String, max: Int): String =
            if (text.length <= max) text else text.take(max) + "…"
    }
}
