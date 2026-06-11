package com.commitgpt.ui

import com.intellij.ui.JBColor
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.TitledBorder

object CommitUi {

    fun hintLabel(text: String): JBLabel = JBLabel(text).apply {
        foreground = JBUI.CurrentTheme.Label.disabledForeground()
        font = JBUI.Fonts.smallFont()
    }

    fun sectionTitle(text: String): TitledSeparator = TitledSeparator(text)

    fun cardPanel(): JPanel = JPanel().apply {
        border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(JBColor.border()),
            JBUI.Borders.empty(12),
        )
        background = UIUtil.getPanelBackground()
        isOpaque = true
    }

    fun primaryButton(text: String): JButton = JButton(text).apply {
        putClientProperty("JButton.buttonType", "default")
    }

    fun comboBoxPreferredWidth(combo: JComboBox<*>, width: Int = 240) {
        combo.preferredSize = Dimension(width, combo.preferredSize.height)
        combo.maximumSize = Dimension(width, combo.preferredSize.height)
    }

    fun monospacedAreaFont(): Font {
        val base = UIUtil.getLabelFont()
        return Font(Font.MONOSPACED, Font.PLAIN, base.size)
    }

    fun formRow(
        label: String,
        field: JComponent,
        labelWidth: Int = 120,
    ): JPanel {
        val panel = JPanel(GridBagLayout())
        val labelComponent = JBLabel(label).apply {
            preferredSize = Dimension(labelWidth, preferredSize.height)
        }
        panel.add(
            labelComponent,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                insets = JBUI.insets(0, 0, 6, 12)
            },
        )
        panel.add(
            field,
            GridBagConstraints().apply {
                gridx = 1
                gridy = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = JBUI.insets(0, 0, 6, 0)
            },
        )
        return panel
    }

    fun twoColumnForm(vararg rows: Pair<String, JComponent>): JPanel {
        val panel = JPanel(GridBagLayout())
        rows.forEachIndexed { index, (label, field) ->
            val labelComponent = JBLabel(label)
            panel.add(
                labelComponent,
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = index
                    anchor = GridBagConstraints.WEST
                    insets = JBUI.insets(0, 0, 8, 12)
                },
            )
            panel.add(
                field,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = index
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = JBUI.insets(0, 0, 8, 0)
                },
            )
        }
        return panel
    }

    fun toolbarPanel(vararg buttons: JComponent): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
        isOpaque = false
        border = JBUI.Borders.emptyTop(8)
        buttons.forEach { add(it) }
    }

    fun titledBorder(title: String): TitledBorder =
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1, true),
            title,
        )

    fun addVerticalGap(parent: JPanel, height: Int = 8) {
        parent.add(
            JPanel().apply { preferredSize = Dimension(0, height) },
            BorderLayout.NORTH,
        )
    }

    fun variableHintHtml(): String = """
        <html><body style='color:#808080;font-size:11px'>
        <b>AI 变量</b>（由 AI 分析 diff 生成）：
        <code>{type}</code> <code>{scope}</code> <code>{subject}</code>
        <code>{body}</code> <code>{footer}</code><br>
        <b>固定文本</b>（原样保留，不交给 AI）：
        <code>{fixed:bugfix}</code> <code>{fixed:feat:}</code> 等<br>
        <b>本地变量</b>（插件自动填充）：
        <code>{emoji}</code> <code>{author}</code> <code>{branch}</code> <code>{date}</code><br>
        示例：<code>{fixed:bugfix}: {subject}</code> → <code>bugfix: fix login error</code>
        </body></html>
    """.trimIndent()

    @Suppress("DEPRECATION")
    fun gridInsets(): Insets = JBUI.insets(4)
}
