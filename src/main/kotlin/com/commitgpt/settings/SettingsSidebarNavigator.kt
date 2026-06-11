package com.commitgpt.settings

import com.intellij.icons.AllIcons
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Font
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

enum class SettingsTab(val title: String, val icon: javax.swing.Icon) {
    TEMPLATES("模板管理", AllIcons.FileTypes.Text),
    AI("AI 配置", AllIcons.General.Web),
}

class SettingsSidebarNavigator(
    private val cards: JPanel,
    private val cardLayout: CardLayout,
) {
    private val listModel = DefaultListModel<SettingsTab>()
    private val navList = JBList(listModel)

    val component: JPanel = JPanel(BorderLayout())

    init {
        SettingsTab.entries.forEach { listModel.addElement(it) }

        navList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        navList.selectedIndex = 0
        navList.fixedCellHeight = JBUI.scale(32)
        navList.cellRenderer = SettingsTabRenderer()
        navList.border = JBUI.Borders.empty(4, 0)

        navList.addListSelectionListener {
            if (it.valueIsAdjusting) return@addListSelectionListener
            val tab = navList.selectedValue ?: return@addListSelectionListener
            cardLayout.show(cards, tab.name)
        }

        val navWrapper = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 8, 8, 0)
            add(JBScrollPane(navList).apply {
                border = JBUI.Borders.empty()
            }, BorderLayout.CENTER)
        }

        val splitter = OnePixelSplitter(false, "CommitGPT.Settings.Nav", 0.18f).apply {
            firstComponent = navWrapper
            secondComponent = cards
            dividerWidth = 1
        }

        component.add(splitter, BorderLayout.CENTER)
        cardLayout.show(cards, SettingsTab.TEMPLATES.name)
    }

    private class SettingsTabRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is SettingsTab) {
                text = "  ${value.title}"
                icon = value.icon
                horizontalTextPosition = javax.swing.SwingConstants.RIGHT
                iconTextGap = JBUI.scale(8)
                border = JBUI.Borders.empty(4, 8)
                if (isSelected) {
                    font = font.deriveFont(Font.BOLD)
                    background = UIUtil.getListSelectionBackground(true)
                    foreground = UIUtil.getListSelectionForeground(true)
                } else {
                    font = font.deriveFont(Font.PLAIN)
                    background = UIUtil.getListBackground()
                    foreground = UIUtil.getListForeground()
                }
                isOpaque = true
            }
            return component
        }
    }
}
