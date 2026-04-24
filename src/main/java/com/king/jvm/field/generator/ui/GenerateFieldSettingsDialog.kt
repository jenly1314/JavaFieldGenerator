/*
 * Copyright (C) 2020 Jenly Yu, https://github.com/jenly1314/JvmFieldGenerator
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.king.jvm.field.generator.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import com.king.jvm.field.generator.component.ConfigComponent
import com.king.jvm.field.generator.model.AnnotationType
import com.king.jvm.field.generator.model.ExistingFieldPolicy
import com.king.jvm.field.generator.model.FieldNameStyle
import com.king.jvm.field.generator.model.FieldParseConfig
import com.king.jvm.field.generator.model.FieldSortStyle
import com.king.jvm.field.generator.model.KotlinPropertyKeyword
import com.king.jvm.field.generator.model.Modifier
import com.king.jvm.field.generator.model.NullabilityMode
import com.king.jvm.field.generator.model.TargetLanguage
import org.apache.commons.lang.StringUtils
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Rectangle
import java.util.LinkedHashMap
import javax.swing.AbstractButton
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JSpinner
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ScrollPaneConstants
import javax.swing.Scrollable
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.border.EmptyBorder

/**
 * 字段生成设置对话框
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
class GenerateFieldSettingsDialog : DialogWrapper(true) {

    private lateinit var contentPane: JPanel
    private lateinit var buttonOK: javax.swing.JButton
    private lateinit var buttonCancel: javax.swing.JButton
    private lateinit var tabbedPane: JTabbedPane

    private lateinit var javaOptionsPanel: JPanel
    private lateinit var kotlinOptionsPanel: JPanel

    private lateinit var spFieldColumn: JSpinner
    private lateinit var spFieldTypeColumn: JSpinner
    private lateinit var spCommentColumn: JSpinner
    private lateinit var spNotNullColumn: JSpinner
    private lateinit var tfNotNullKeywords: JTextField

    private lateinit var rbNameStyleNone: JRadioButton
    private lateinit var rbNameStyleCamel: JRadioButton
    private lateinit var rbNameStyleSnake: JRadioButton
    private lateinit var rbFieldSortDefault: JRadioButton
    private lateinit var rbFieldSortNameLocal: JRadioButton
    private lateinit var rbFieldSortNameGlobal: JRadioButton
    private lateinit var rbExistingFieldIgnore: JRadioButton
    private lateinit var rbExistingFieldOverwrite: JRadioButton

    private lateinit var rbModifierPrivate: JRadioButton
    private lateinit var rbModifierProtected: JRadioButton
    private lateinit var rbModifierPublic: JRadioButton
    private lateinit var rbModifierDefault: JRadioButton

    private lateinit var rbNullabilityAuto: JRadioButton
    private lateinit var rbNullabilityNotNull: JRadioButton
    private lateinit var rbNullabilityNullable: JRadioButton

    private lateinit var rbTargetLanguageAuto: JRadioButton
    private lateinit var rbTargetLanguageJava: JRadioButton
    private lateinit var rbTargetLanguageKotlin: JRadioButton

    private lateinit var rbKotlinKeywordVar: JRadioButton
    private lateinit var rbKotlinKeywordVal: JRadioButton

    private lateinit var chkGenerateGetterSetter: JCheckBox
    private lateinit var chkGenerateToString: JCheckBox
    private lateinit var chkUseDataClass: JCheckBox
    private lateinit var lblNotNullColumn: JLabel
    private lateinit var lblNotNullKeywords: JLabel
    private lateinit var lblNullabilityTip: JLabel
    private lateinit var lblNullabilityKeywordsHint: JLabel
    private lateinit var panelNullabilityAutoConfig: JPanel

    private lateinit var rbAnnoNone: JRadioButton
    private lateinit var rbAnnoGson: JRadioButton
    private lateinit var rbAnnoMoshi: JRadioButton
    private lateinit var rbAnnoJackson: JRadioButton
    private lateinit var rbAnnoFastJson: JRadioButton
    private lateinit var rbAnnoKotlinSerial: JRadioButton
    private lateinit var rbAnnoCustom: JRadioButton
    private lateinit var taAnnoImport: JTextArea
    private lateinit var taAnnoClassFormat: JTextArea
    private lateinit var taAnnoPropFormat: JTextArea

    private lateinit var taTypeMapping: JTextArea
    private lateinit var chkArrayToList: JCheckBox

    init {
        initUiComponents()
        title = "JvmFieldGenerator - Field Generation Settings"
        init()
        applyPrimaryButtonStyle()

        loadConfiguration()

        buttonOK.addActionListener { onOK() }
        buttonCancel.addActionListener { onCancel() }

        contentPane.registerKeyboardAction(
            { onCancel() },
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )

        val targetLanguageListener = java.awt.event.ActionListener { updateLanguageState() }
        rbTargetLanguageAuto.addActionListener(targetLanguageListener)
        rbTargetLanguageJava.addActionListener(targetLanguageListener)
        rbTargetLanguageKotlin.addActionListener(targetLanguageListener)

        val nullabilityListener = java.awt.event.ActionListener { updateNullabilityState() }
        rbNullabilityAuto.addActionListener(nullabilityListener)
        rbNullabilityNotNull.addActionListener(nullabilityListener)
        rbNullabilityNullable.addActionListener(nullabilityListener)

        val annoListener = java.awt.event.ActionListener { updateAnnotationState() }
        rbAnnoNone.addActionListener(annoListener)
        rbAnnoGson.addActionListener(annoListener)
        rbAnnoMoshi.addActionListener(annoListener)
        rbAnnoJackson.addActionListener(annoListener)
        rbAnnoFastJson.addActionListener(annoListener)
        rbAnnoKotlinSerial.addActionListener(annoListener)
        rbAnnoCustom.addActionListener(annoListener)

        updateLanguageState()
        updateAnnotationState()
        updateNullabilityState()
    }

    override fun createCenterPanel(): JComponent = contentPane

    override fun createActions(): Array<javax.swing.Action> = emptyArray()

    private fun initUiComponents() {
        contentPane = JPanel(BorderLayout(0, JBUI.scale(UI.Spacing.DIALOG_CONTENT_GAP)))
        contentPane.border = JBUI.Borders.empty(
            UI.Spacing.DIALOG_TOP_PADDING,
            UI.Spacing.DIALOG_HORIZONTAL_PADDING,
            UI.Spacing.DIALOG_BOTTOM_PADDING,
            UI.Spacing.DIALOG_HORIZONTAL_PADDING
        )
        contentPane.preferredSize = DIALOG_SIZE

        tabbedPane = JTabbedPane()
        tabbedPane.tabLayoutPolicy = JTabbedPane.WRAP_TAB_LAYOUT
        tabbedPane.addTab("General", jScrollPanel(createPropertyPanel()))
        tabbedPane.addTab("Annotation", jScrollPanel(createAnnotationPanel()))

        contentPane.add(tabbedPane, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(UI.Spacing.BUTTON_GAP), 0))
        buttonOK = javax.swing.JButton("OK")
        buttonCancel = javax.swing.JButton("Cancel")
        buttonPanel.add(buttonOK)
        buttonPanel.add(buttonCancel)
        contentPane.add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun applyPrimaryButtonStyle() {
        buttonOK.putClientProperty("JButton.buttonType", "default")
        rootPane.defaultButton = buttonOK
    }

    private fun jScrollPanel(content: JComponent): JScrollPane {
        val scrollPane = JScrollPane(wrapViewportWidthContent(content))
        scrollPane.border = JBUI.Borders.empty()
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        scrollPane.verticalScrollBar.unitIncrement = JBUI.scale(UI.Spacing.SCROLL_UNIT_INCREMENT)
        return scrollPane
    }

    private fun wrapViewportWidthContent(content: JComponent): JComponent = ViewportWidthPanel(content)

    private class ViewportWidthPanel(content: JComponent) : JPanel(BorderLayout()), Scrollable {
        init {
            isOpaque = false
            add(content, BorderLayout.NORTH)
        }

        override fun getPreferredScrollableViewportSize(): Dimension = preferredSize

        override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int {
            return JBUI.scale(UI.Spacing.SCROLL_UNIT_INCREMENT)
        }

        override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int {
            val unitIncrement = JBUI.scale(UI.Spacing.SCROLL_UNIT_INCREMENT)
            return maxOf(unitIncrement, visibleRect.height - unitIncrement)
        }

        override fun getScrollableTracksViewportWidth(): Boolean = true
        override fun getScrollableTracksViewportHeight(): Boolean = false
    }

    private fun createPropertyPanel(): JPanel {
        val panel = createVerticalPanel()

        val generalSettingsPanel = createCategoryPanel("General Settings")
        generalSettingsPanel.add(createTargetLanguageSection())
        generalSettingsPanel.add(createSectionSpacing())
        generalSettingsPanel.add(createColumnsSection())
        generalSettingsPanel.add(createSectionSpacing())
        generalSettingsPanel.add(createNameStyleSection())
        generalSettingsPanel.add(createSectionSpacing())
        generalSettingsPanel.add(createFieldSortSection())
        generalSettingsPanel.add(createSectionSpacing())
        generalSettingsPanel.add(createExistingFieldSection())
        generalSettingsPanel.add(createSectionSpacing())
        generalSettingsPanel.add(createTypeConversionSection())
        panel.add(generalSettingsPanel)
        panel.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.SECTION_GAP)))

        javaOptionsPanel = createJavaOptionsSection()
        panel.add(javaOptionsPanel)
        panel.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.SECTION_GAP)))

        kotlinOptionsPanel = createKotlinOptionsSection()
        panel.add(kotlinOptionsPanel)
        panel.add(Box.createVerticalGlue())
        return panel
    }

    private fun createTargetLanguageSection(): JPanel {
        val row = leftRow(UI.Spacing.ROW_GAP)
        rbTargetLanguageAuto = createEnumRadioButton(TargetLanguage.AUTO)
        rbTargetLanguageJava = createEnumRadioButton(TargetLanguage.JAVA)
        rbTargetLanguageKotlin = createEnumRadioButton(TargetLanguage.KOTLIN)

        val group = ButtonGroup()
        group.add(rbTargetLanguageAuto)
        group.add(rbTargetLanguageJava)
        group.add(rbTargetLanguageKotlin)

        row.add(rbTargetLanguageAuto)
        row.add(rbTargetLanguageJava)
        row.add(rbTargetLanguageKotlin)
        return createSettingRow("Target Language", row, null)
    }

    private fun createColumnsSection(): JPanel {
        val row = leftRow(UI.Spacing.ROW_GAP_COMPACT)
        row.add(createInlineLabel("Field"))
        spFieldColumn = createCompactSpinner(FieldParseConfig.DEFAULT_FIELD_COLUMN, 0, Int.MAX_VALUE)
        row.add(spFieldColumn)

        row.add(createInlineLabel("Type"))
        spFieldTypeColumn = createCompactSpinner(FieldParseConfig.DEFAULT_FIELD_TYPE_COLUMN, 0, Int.MAX_VALUE)
        row.add(spFieldTypeColumn)

        row.add(createInlineLabel("Comment"))
        spCommentColumn = createCompactSpinner(FieldParseConfig.DEFAULT_FIELD_COMMENT_COLUMN, -1, Int.MAX_VALUE)
        row.add(spCommentColumn)

        return createSettingRow("Field Columns", row, null)
    }

    private fun createNameStyleSection(): JPanel {
        val row = leftRow(UI.Spacing.ROW_GAP)
        rbNameStyleNone = createEnumRadioButton(FieldNameStyle.NONE)
        rbNameStyleCamel = createEnumRadioButton(FieldNameStyle.CAMEL_CASE)
        rbNameStyleSnake = createEnumRadioButton(FieldNameStyle.SNAKE_CASE)

        val group = ButtonGroup()
        group.add(rbNameStyleNone)
        group.add(rbNameStyleCamel)
        group.add(rbNameStyleSnake)

        row.add(rbNameStyleNone)
        row.add(rbNameStyleCamel)
        row.add(rbNameStyleSnake)
        return createSettingRow("Field Name Style", row, null)
    }

    private fun createFieldSortSection(): JPanel {
        val row = leftRow(UI.Spacing.ROW_GAP)
        rbFieldSortDefault = createEnumRadioButton(FieldSortStyle.DEFAULT)
        rbFieldSortNameLocal = createEnumRadioButton(FieldSortStyle.FIELD_NAME_LOCAL)
        rbFieldSortNameGlobal = createEnumRadioButton(FieldSortStyle.FIELD_NAME_GLOBAL)

        val group = ButtonGroup()
        group.add(rbFieldSortDefault)
        group.add(rbFieldSortNameLocal)
        group.add(rbFieldSortNameGlobal)

        row.add(rbFieldSortDefault)
        row.add(rbFieldSortNameLocal)
        row.add(rbFieldSortNameGlobal)
        return createSettingRow("Field Sort", row, null)
    }

    private fun createExistingFieldSection(): JPanel {
        val row = leftRow(UI.Spacing.ROW_GAP)
        rbExistingFieldIgnore = createEnumRadioButton(ExistingFieldPolicy.IGNORE_NEW)
        rbExistingFieldOverwrite = createEnumRadioButton(ExistingFieldPolicy.OVERWRITE_OLD)

        val group = ButtonGroup()
        group.add(rbExistingFieldIgnore)
        group.add(rbExistingFieldOverwrite)

        row.add(rbExistingFieldIgnore)
        row.add(rbExistingFieldOverwrite)
        return createSettingRow("Existing Fields", row, null)
    }

    private fun createTypeConversionSection(): JPanel {
        val content = createStackPanel()

        val optionRow = leftRow(UI.Spacing.ROW_GAP)
        chkArrayToList = JCheckBox("Array to List (T[] -> List<T>)")
        optionRow.add(chkArrayToList)
        content.add(optionRow)
        content.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.ITEM_GAP)))

        taTypeMapping = jTextAreaInput(10)
        content.add(createAlignedTextAreaGroup("Field Type Mappings (format: source=target)", taTypeMapping, 10, 0))
        return createSettingRow("Type Conversion", content, null)
    }

    private fun createJavaOptionsSection(): JPanel {
        val section = createCategoryPanel("Java Options")
        section.add(createModifierSection())
        section.add(createSectionSpacing())
        section.add(createJavaMethodSection())
        return section
    }

    private fun createModifierSection(): JPanel {
        val row = leftRow(UI.Spacing.ROW_GAP)
        rbModifierPrivate = createEnumRadioButton(Modifier.PRIVATE)
        rbModifierProtected = createEnumRadioButton(Modifier.PROTECTED)
        rbModifierPublic = createEnumRadioButton(Modifier.PUBLIC)
        rbModifierDefault = createEnumRadioButton(Modifier.DEFAULT)

        val group = ButtonGroup()
        group.add(rbModifierPrivate)
        group.add(rbModifierProtected)
        group.add(rbModifierPublic)
        group.add(rbModifierDefault)

        row.add(rbModifierPrivate)
        row.add(rbModifierProtected)
        row.add(rbModifierPublic)
        row.add(rbModifierDefault)
        return createSettingRow("Modifier", row, null)
    }

    private fun createJavaMethodSection(): JPanel {
        val row = leftRow(UI.Spacing.ROW_GAP)
        chkGenerateGetterSetter = JCheckBox("Getter/Setter")
        chkGenerateToString = JCheckBox("toString")
        row.add(chkGenerateGetterSetter)
        row.add(chkGenerateToString)
        return createSettingRow("Java Methods", row, null)
    }

    private fun createKotlinOptionsSection(): JPanel {
        val section = createCategoryPanel("Kotlin Options")
        section.add(createKotlinClassStyleSection())
        section.add(createSectionSpacing())
        section.add(createKotlinKeywordSection())
        section.add(createSectionSpacing())
        section.add(createNullabilitySection())
        return section
    }

    private fun createNullabilitySection(): JPanel {
        val content = createStackPanel()

        rbNullabilityAuto = createEnumRadioButton(NullabilityMode.AUTO)
        rbNullabilityNotNull = createEnumRadioButton(NullabilityMode.NOT_NULL)
        rbNullabilityNullable = createEnumRadioButton(NullabilityMode.NULLABLE)

        val group = ButtonGroup()
        group.add(rbNullabilityAuto)
        group.add(rbNullabilityNotNull)
        group.add(rbNullabilityNullable)

        lblNullabilityTip = createHintLabel("Infer non-null fields from the configured column and keywords.")
        content.add(createOptionRow(rbNullabilityAuto, lblNullabilityTip))
        content.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.ITEM_GAP)))

        panelNullabilityAutoConfig = createStackPanel()
        panelNullabilityAutoConfig.isOpaque = false
        panelNullabilityAutoConfig.alignmentX = Component.LEFT_ALIGNMENT

        lblNotNullColumn = createInlineLabel("Not-Null Column")
        spNotNullColumn = createCompactSpinner(FieldParseConfig.DEFAULT_FIELD_NOTNULL_COLUMN, -1, Int.MAX_VALUE)
        val lblNotNullColumnHint = createHintLabel("Only used when Auto is selected.")
        panelNullabilityAutoConfig.add(createNullabilityAutoConfigRow(lblNotNullColumn, spNotNullColumn, lblNotNullColumnHint))
        panelNullabilityAutoConfig.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.ITEM_GAP)))

        lblNotNullKeywords = createInlineLabel("Keywords")
        tfNotNullKeywords = createInputTextField(FieldParseConfig.DEFAULT_NOT_NULL_KEYWORDS)
        setFixedWidth(tfNotNullKeywords, UI.Size.NULLABILITY_KEYWORDS_WIDTH, UI.Size.INPUT_HEIGHT)
        lblNullabilityKeywordsHint = createHintLabel("Comma-separated, for example: ${FieldParseConfig.DEFAULT_NOT_NULL_KEYWORDS}")
        panelNullabilityAutoConfig.add(createNullabilityAutoConfigRow(lblNotNullKeywords, tfNotNullKeywords, lblNullabilityKeywordsHint))
        panelNullabilityAutoConfig.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.ITEM_GAP)))
        content.add(wrapWithLeadingInset(panelNullabilityAutoConfig, getSelectableTextInset(rbNullabilityAuto) + NULLABILITY_AUTO_CHILD_INDENT_OFFSET))

        content.add(createOptionRow(rbNullabilityNotNull, null))
        content.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.ITEM_GAP)))
        content.add(createOptionRow(rbNullabilityNullable, null))

        return createSettingRow("Nullability", content, null)
    }

    private fun createKotlinKeywordSection(): JPanel {
        val row = leftRow(0)
        rbKotlinKeywordVal = createEnumRadioButton(KotlinPropertyKeyword.VAL)
        rbKotlinKeywordVar = createEnumRadioButton(KotlinPropertyKeyword.VAR)

        val group = ButtonGroup()
        group.add(rbKotlinKeywordVal)
        group.add(rbKotlinKeywordVar)

        row.add(rbKotlinKeywordVal)
        row.add(Box.createHorizontalStrut(JBUI.scale(UI.Spacing.ROW_GAP)))
        row.add(rbKotlinKeywordVar)
        return createSettingRow("Property Keyword", row, null)
    }

    private fun createKotlinClassStyleSection(): JPanel {
        chkUseDataClass = JCheckBox("Use data class")
        val row = createOptionRow(
            chkUseDataClass,
            createHintLabel("Checked: create or convert to a data class; unchecked: keep a regular class.")
        )
        return createSettingRow("Class Style", row, null)
    }

    private fun createAnnotationPanel(): JPanel {
        val panel = createVerticalPanel()

        val section = createGroupPanel("Annotation Type")
        val radioPanel = JPanel(GridBagLayout())
        radioPanel.alignmentX = Component.LEFT_ALIGNMENT
        rbAnnoNone = createEnumRadioButton(AnnotationType.NONE)
        rbAnnoGson = createAnnotationRadioButton(AnnotationType.GSON, "@SerializedName")
        rbAnnoMoshi = createAnnotationRadioButton(AnnotationType.MOSHI, "@Json")
        rbAnnoJackson = createAnnotationRadioButton(AnnotationType.JACKSON, "@JsonProperty")
        rbAnnoFastJson = createAnnotationRadioButton(AnnotationType.FASTJSON, "@JSONField")
        rbAnnoKotlinSerial = createAnnotationRadioButton(AnnotationType.KOTLIN_SERIALIZATION, "@SerialName")
        rbAnnoCustom = createEnumRadioButton(AnnotationType.CUSTOM)

        val annotationGroup = ButtonGroup()
        annotationGroup.add(rbAnnoNone)
        annotationGroup.add(rbAnnoGson)
        annotationGroup.add(rbAnnoMoshi)
        annotationGroup.add(rbAnnoJackson)
        annotationGroup.add(rbAnnoFastJson)
        annotationGroup.add(rbAnnoKotlinSerial)
        annotationGroup.add(rbAnnoCustom)

        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(
            UI.Spacing.GRID_ITEM_GAP_VERTICAL,
            0,
            UI.Spacing.GRID_ITEM_GAP_VERTICAL,
            UI.Spacing.GRID_ITEM_GAP_HORIZONTAL
        )
        addGridComponent(radioPanel, rbAnnoNone, gbc, 0, 0)
        addGridComponent(radioPanel, rbAnnoGson, gbc, 1, 0)
        addGridComponent(radioPanel, rbAnnoMoshi, gbc, 0, 1)
        addGridComponent(radioPanel, rbAnnoJackson, gbc, 1, 1)
        addGridComponent(radioPanel, rbAnnoFastJson, gbc, 0, 2)
        addGridComponent(radioPanel, rbAnnoKotlinSerial, gbc, 1, 2)
        addGridComponent(radioPanel, rbAnnoCustom, gbc, 0, 3)
        section.add(radioPanel)
        section.border = JBUI.Borders.empty(0, 0, UI.Spacing.SECTION_VERTICAL_PADDING, 0)
        panel.add(section)
        panel.add(jLine())

        val customSection = createSpacedGroupPanel("Custom Annotation Configuration")
        taAnnoImport = jTextAreaInput(2, "import kotlinx.serialization.*")
        taAnnoClassFormat = jTextAreaInput(2, "@Serializable")
        taAnnoPropFormat = jTextAreaInput(2, "@SerialName(\"%s\")")
        customSection.add(createTextAreaGroup("Import Statements", taAnnoImport, 2))
        customSection.add(createTextAreaGroup("Class Annotation Format", taAnnoClassFormat, 2))
        customSection.add(createTextAreaGroup("Property Annotation Format", taAnnoPropFormat, 2))
        panel.add(customSection)
        panel.add(Box.createVerticalGlue())
        return panel
    }

    private fun createVerticalPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(
            UI.Spacing.CONTENT_TOP_PADDING,
            UI.Spacing.CONTENT_HORIZONTAL_PADDING,
            UI.Spacing.CONTENT_BOTTOM_PADDING,
            UI.Spacing.CONTENT_HORIZONTAL_PADDING
        )
        panel.alignmentX = Component.LEFT_ALIGNMENT
        return panel
    }

    private fun createStackPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        return panel
    }

    private fun createGroupPanel(title: String): JPanel {
        val section = createStackPanel()
        section.add(jLabel(title))
        section.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.GROUP_TITLE_GAP)))
        section.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        return section
    }

    private fun createSpacedGroupPanel(title: String): JPanel {
        val section = createStackPanel()
        val titleLabel = jLabel(title)
        titleLabel.border = JBUI.Borders.empty(
            UI.Spacing.TITLE_VERTICAL_PADDING,
            0,
            UI.Spacing.TITLE_VERTICAL_PADDING,
            0
        )
        section.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.GROUP_TITLE_GAP_LARGE)))
        section.add(titleLabel)
        section.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        return section
    }

    private fun createCategoryPanel(title: String): JPanel {
        val section = createStackPanel()
        section.add(createCategoryHeader(title))
        section.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.CATEGORY_CONTENT_GAP)))
        section.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        return section
    }

    private fun createCategoryHeader(title: String): JPanel {
        val label = jLabel(title)
        label.font = label.font.deriveFont(Font.BOLD, label.font.size2D + UI.FontSize.CATEGORY_TITLE)

        val headerHeight = maxOf(JBUI.scale(UI.Size.CATEGORY_HEADER_MIN_HEIGHT), label.preferredSize.height)

        val header = JPanel(BorderLayout(JBUI.scale(UI.Spacing.CATEGORY_HEADER_GAP), 0))
        header.isOpaque = false
        header.alignmentX = Component.LEFT_ALIGNMENT
        header.maximumSize = Dimension(Int.MAX_VALUE, headerHeight)
        header.preferredSize = Dimension(0, headerHeight)
        header.add(label, BorderLayout.WEST)
        header.add(createCenteredSeparator(headerHeight), BorderLayout.CENTER)
        return header
    }

    private fun createSectionSpacing(): Component = Box.createVerticalStrut(JBUI.scale(UI.Spacing.SECTION_VERTICAL_PADDING))

    private fun createSettingRow(title: String, content: Component, hintLabel: JLabel?): JPanel {
        val row = createAlignedRowPanel()
        val firstLineHeight = resolveFirstLineHeight(content)

        var gbc = createRowConstraints()
        val titleLabel = createSettingTitleLabel(title)
        applyFirstLineLabelHeight(titleLabel, firstLineHeight)
        gbc.gridx = 0
        gbc.insets = JBUI.insets(0, 0, 0, UI.Spacing.TITLE_CONTENT_GAP)
        row.add(alignWithFirstLine(titleLabel, firstLineHeight), gbc)

        gbc = createRowConstraints()
        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        row.add(content, gbc)

        if (hintLabel != null) {
            gbc = createRowConstraints()
            gbc.gridx = 2
            gbc.insets = JBUI.insets(0, UI.Spacing.TITLE_CONTENT_GAP, 0, 0)
            row.add(alignWithFirstLine(hintLabel, firstLineHeight), gbc)
        }

        return row
    }

    private fun createAlignedRowPanel(): JPanel {
        val row = JPanel(GridBagLayout())
        row.alignmentX = Component.LEFT_ALIGNMENT
        row.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        row.border = JBUI.Borders.emptyBottom(UI.Spacing.ROW_GAP)
        return row
    }

    private fun createRowConstraints(): GridBagConstraints {
        val gbc = GridBagConstraints()
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.NORTHWEST
        return gbc
    }

    private fun createCenteredSeparator(height: Int): JComponent {
        val separator: JComponent = object : JComponent() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                g.color = resolveSeparatorColor()
                val y = maxOf(0, ((height - 1) / 2) + CATEGORY_SEPARATOR_OPTICAL_OFFSET)
                g.drawLine(0, y, maxOf(0, width - 1), y)
            }
        }
        separator.isOpaque = false
        separator.alignmentX = Component.LEFT_ALIGNMENT
        val separatorThickness = JBUI.scale(UI.Size.SEPARATOR_THICKNESS)
        separator.preferredSize = Dimension(separatorThickness, height)
        separator.minimumSize = Dimension(separatorThickness, height)
        separator.maximumSize = Dimension(Int.MAX_VALUE, height)
        return separator
    }

    private fun applyFirstLineLabelHeight(label: JLabel, firstLineHeight: Int) {
        if (firstLineHeight > 0) {
            setAdaptiveLabelHeight(label, firstLineHeight)
        }
    }

    private fun alignWithFirstLine(component: Component, firstLineHeight: Int): Component {
        val componentHeight = component.preferredSize.height
        val topInset = maxOf(0, (firstLineHeight - componentHeight) / 2)
        if (topInset == 0) {
            return component
        }
        val wrapper = JPanel(BorderLayout())
        wrapper.isOpaque = false
        wrapper.alignmentX = Component.LEFT_ALIGNMENT
        wrapper.border = EmptyBorder(topInset, 0, 0, 0)
        wrapper.add(component, BorderLayout.NORTH)
        return wrapper
    }

    private fun resolveFirstLineHeight(component: Component?): Int {
        if (component == null || !component.isVisible) return 0
        if (component is Box.Filler) return 0
        if (component is JLabel) {
            val text = component.text
            if (text != null && text.startsWith("<html>")) {
                return component.getFontMetrics(component.font).height
            }
        }
        if (component !is Container) return component.preferredSize.height
        for (child in component.components) {
            val childHeight = resolveFirstLineHeight(child)
            if (childHeight > 0) return childHeight
        }
        return component.preferredSize.height
    }

    private fun createNullabilityAutoConfigRow(titleLabel: JLabel, inputComponent: JComponent, hintLabel: JLabel): JPanel {
        val row = leftRow(0)
        titleLabel.alignmentY = Component.CENTER_ALIGNMENT
        inputComponent.alignmentY = Component.CENTER_ALIGNMENT
        hintLabel.alignmentY = Component.CENTER_ALIGNMENT
        row.add(titleLabel)
        row.add(Box.createHorizontalStrut(JBUI.scale(UI.Spacing.TITLE_CONTENT_GAP)))
        row.add(inputComponent)
        row.add(Box.createHorizontalStrut(JBUI.scale(UI.Spacing.HINT_GAP)))
        row.add(hintLabel)
        return row
    }

    private fun createOptionRow(button: AbstractButton, hintLabel: JLabel?): JPanel {
        val row = leftRow(0)
        row.add(button)
        if (hintLabel != null) {
            row.add(Box.createHorizontalStrut(JBUI.scale(UI.Spacing.HINT_GAP)))
            row.add(hintLabel)
        }
        return row
    }

    private fun leftRow(hgap: Int): JPanel {
        val row = object : JPanel() {
            override fun addImpl(comp: Component, constraints: Any?, index: Int) {
                if (hgap > 0 && componentCount > 0 && comp !is Box.Filler) {
                    super.addImpl(Box.createHorizontalStrut(JBUI.scale(hgap)), null, -1)
                }
                super.addImpl(comp, constraints, index)
            }
        }
        row.layout = BoxLayout(row, BoxLayout.X_AXIS)
        row.alignmentX = Component.LEFT_ALIGNMENT
        row.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        return row
    }

    private fun createEnumRadioButton(value: Any): JRadioButton = JRadioButton(value.toString())

    private fun createAnnotationRadioButton(type: AnnotationType, annotationName: String): JRadioButton {
        return JRadioButton("$type ($annotationName)")
    }

    private fun createSettingTitleLabel(text: String): JLabel {
        val label = jLabel(text)
        setFixedLabelSize(label, UI.Size.LABEL_WIDTH, UI.Size.CONTROL_HEIGHT)
        label.font = label.font.deriveFont(label.font.size2D + UI.FontSize.SETTING_TITLE)
        return label
    }

    private fun jLabel(text: String): JLabel {
        val label = JLabel(text)
        label.alignmentX = Component.LEFT_ALIGNMENT
        label.font = label.font.deriveFont(label.font.size2D + UI.FontSize.CONTENT_TEXT)
        return label
    }

    private fun createHintLabel(text: String): JLabel {
        val label = JLabel("<html>$text</html>")
        label.alignmentX = Component.LEFT_ALIGNMENT
        label.font = label.font.deriveFont(label.font.size2D + UI.FontSize.HINT_TEXT)
        label.foreground = resolveHintTextColor()
        return label
    }

    private fun createInlineLabel(text: String): JLabel {
        val label = jLabel(text)
        setAdaptiveLabelHeight(label, JBUI.scale(UI.Size.CONTROL_HEIGHT))
        return label
    }

    private fun createAlignedTextAreaGroup(title: String, textArea: JTextArea, rows: Int): JPanel {
        return createAlignedTextAreaGroup(title, textArea, rows, getSelectableTextInset())
    }

    private fun createAlignedTextAreaGroup(title: String, textArea: JTextArea, rows: Int, leftInset: Int): JPanel {
        val group = createStackPanel()
        group.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)

        val alignedContent = createStackPanel()
        alignedContent.isOpaque = false
        alignedContent.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        alignedContent.border = EmptyBorder(0, leftInset, 0, 0)
        alignedContent.add(jLabel(title))
        alignedContent.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.GROUP_TITLE_GAP)))
        alignedContent.add(wrapTextAreaInput(textArea, rows))
        group.add(alignedContent)
        return group
    }

    private fun wrapWithLeadingInset(content: JComponent, leftInset: Int): JPanel {
        val wrapper = JPanel(BorderLayout())
        wrapper.isOpaque = false
        wrapper.alignmentX = Component.LEFT_ALIGNMENT
        wrapper.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        wrapper.border = EmptyBorder(0, leftInset, 0, 0)
        wrapper.add(content, BorderLayout.CENTER)
        return wrapper
    }

    private fun resolveSeparatorColor(): Color = resolveHintTextColor()

    private fun resolveHintTextColor(): Color = UIManager.getColor("Label.disabledForeground") ?: UI.Color.BORDER_FALLBACK

    private fun setFixedLabelSize(label: JLabel, width: Int, height: Int) {
        val size = JBUI.size(width, height)
        label.preferredSize = size
        label.minimumSize = size
        label.maximumSize = size
    }

    private fun setAdaptiveLabelHeight(label: JLabel, height: Int) {
        val preferredSize = label.preferredSize
        val size = Dimension(preferredSize.width, height)
        label.preferredSize = size
        label.minimumSize = size
        label.maximumSize = size
        label.verticalAlignment = SwingConstants.CENTER
    }

    private fun setFixedWidth(component: JComponent, width: Int, height: Int) {
        val size = JBUI.size(width, height)
        component.preferredSize = size
        component.minimumSize = size
        component.maximumSize = size
    }

    private fun getSelectableTextInset(): Int {
        var inset = 0
        val margin = UIManager.getInsets("RadioButton.margin")
        if (margin != null) inset += margin.left
        val icon = UIManager.getIcon("RadioButton.icon")
        if (icon != null) inset += icon.iconWidth
        val textGap = UIManager.getInt("RadioButton.textIconGap")
        if (textGap > 0) inset += textGap
        if (inset > 0) return inset

        val probe = JRadioButton()
        val probeMargin = probe.margin
        if (probeMargin != null) inset += probeMargin.left
        val probeIcon = probe.icon
        if (probeIcon != null) inset += probeIcon.iconWidth
        inset += probe.iconTextGap
        return maxOf(inset, JBUI.scale(UI.Size.SELECTABLE_TEXT_MIN_INSET))
    }

    private fun getSelectableTextInset(button: AbstractButton): Int {
        var inset = 0
        val insets = button.insets
        if (insets != null) {
            inset += insets.left
        } else {
            val margin = button.margin
            if (margin != null) inset += margin.left
        }
        var icon: Icon? = button.icon
        if (icon == null) {
            icon = if (button is JCheckBox) UIManager.getIcon("CheckBox.icon") else UIManager.getIcon("RadioButton.icon")
        }
        if (icon != null) inset += icon.iconWidth
        inset += maxOf(0, button.iconTextGap)
        return maxOf(inset, JBUI.scale(UI.Size.SELECTABLE_TEXT_MIN_INSET))
    }

    private fun jLine(): Component {
        val separator = JSeparator()
        separator.alignmentX = Component.LEFT_ALIGNMENT
        separator.foreground = resolveSeparatorColor()
        val lineSize = Dimension(Int.MAX_VALUE, JBUI.scale(UI.Size.SEPARATOR_THICKNESS))
        separator.preferredSize = lineSize
        separator.minimumSize = Dimension(0, JBUI.scale(UI.Size.SEPARATOR_THICKNESS))
        separator.maximumSize = lineSize
        separator.border = JBUI.Borders.empty(
            UI.Spacing.SEPARATOR_VERTICAL_PADDING,
            0,
            UI.Spacing.SEPARATOR_VERTICAL_PADDING,
            0
        )
        return separator
    }

    private fun createCompactSpinner(value: Int, minimum: Int, maximum: Int): JSpinner {
        val spinner = JSpinner(SpinnerNumberModel(value, minimum, maximum, 1))
        val spinnerSize = JBUI.size(UI.Size.SPINNER_WIDTH, UI.Size.CONTROL_HEIGHT)
        spinner.preferredSize = spinnerSize
        spinner.minimumSize = spinnerSize
        spinner.maximumSize = spinnerSize
        val editor = spinner.editor
        if (editor is JSpinner.DefaultEditor) {
            val textField = editor.textField
            textField.columns = 3
            textField.horizontalAlignment = JTextField.CENTER
        }
        return spinner
    }

    private fun createInputTextField(value: String): JTextField {
        val textField = JTextField(value)
        textField.maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(UI.Size.INPUT_HEIGHT))
        textField.font = textField.font.deriveFont(textField.font.size2D + UI.FontSize.INPUT_TEXT)
        textField.margin = JBUI.insets(
            UI.Spacing.INPUT_VERTICAL_PADDING,
            UI.Spacing.INPUT_HORIZONTAL_PADDING,
            UI.Spacing.INPUT_VERTICAL_PADDING,
            UI.Spacing.INPUT_HORIZONTAL_PADDING
        )
        textField.alignmentX = Component.LEFT_ALIGNMENT
        return textField
    }

    private fun jTextAreaInput(rows: Int): JTextArea = jTextAreaInput(rows, null)

    private fun jTextAreaInput(rows: Int, hint: String?): JTextArea {
        val textArea = if (hint == null) JTextArea() else LineNumberTextArea.createTextArea(hint)
        textArea.rows = rows
        textArea.columns = 1
        textArea.alignmentX = Component.LEFT_ALIGNMENT
        textArea.font = textArea.font.deriveFont(textArea.font.size2D + UI.FontSize.INPUT_TEXT)
        return textArea
    }

    private fun createTextAreaGroup(title: String, textArea: JTextArea, rows: Int): JPanel {
        val group = createStackPanel()
        group.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        val titleLabel = jLabel(title)
        titleLabel.border = JBUI.Borders.empty(
            UI.Spacing.TITLE_VERTICAL_PADDING,
            0,
            UI.Spacing.TITLE_VERTICAL_PADDING,
            0
        )
        group.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.GROUP_TITLE_GAP_LARGE)))
        group.add(titleLabel)
        group.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.GROUP_TITLE_GAP_LARGE)))
        group.add(wrapTextAreaInput(textArea, rows))
        return group
    }

    private fun wrapTextAreaInput(textArea: JTextArea, rows: Int): JScrollPane {
        val scrollPane = LineNumberTextArea.wrap(textArea)
        val lineHeight = textArea.getFontMetrics(textArea.font).height
        val preferredHeight = maxOf(
            (lineHeight * rows) + JBUI.scale(UI.Size.TEXT_AREA_HEIGHT_PADDING),
            JBUI.scale(UI.Size.TEXT_AREA_MIN_HEIGHT)
        )
        scrollPane.alignmentX = Component.LEFT_ALIGNMENT
        scrollPane.viewport.minimumSize = Dimension(0, preferredHeight)
        scrollPane.viewport.preferredSize = Dimension(0, preferredHeight)
        scrollPane.minimumSize = Dimension(0, preferredHeight)
        scrollPane.preferredSize = Dimension(0, preferredHeight)
        scrollPane.maximumSize = Dimension(Int.MAX_VALUE, preferredHeight + JBUI.scale(UI.Size.TEXT_AREA_MAX_EXTRA_HEIGHT))
        return scrollPane
    }

    private fun addGridComponent(panel: JPanel, comp: Component, gbc: GridBagConstraints, x: Int, y: Int) {
        gbc.gridx = x
        gbc.gridy = y
        gbc.weightx = 0.5
        panel.add(comp, gbc)
    }

    private fun loadConfiguration() {
        val config = ConfigComponent.getInstance().resolveFieldParseConfig()

        val fieldColumn = normalizeSpinnerValue(spFieldColumn, config.fieldColumn, 0)
        val fieldTypeColumn = normalizeSpinnerValue(spFieldTypeColumn, config.fieldTypeColumn, 1)
        val commentColumn = normalizeSpinnerValue(spCommentColumn, config.fieldCommentColumn, FieldParseConfig.DEFAULT_FIELD_COMMENT_COLUMN)
        val notNullColumn = normalizeSpinnerValue(spNotNullColumn, config.fieldNotNullColumn, -1)

        spFieldColumn.value = fieldColumn
        spFieldTypeColumn.value = fieldTypeColumn
        spCommentColumn.value = commentColumn
        spNotNullColumn.value = notNullColumn

        config.fieldColumn = fieldColumn
        config.fieldTypeColumn = fieldTypeColumn
        config.fieldCommentColumn = commentColumn
        config.fieldNotNullColumn = notNullColumn
        tfNotNullKeywords.text = config.notNullKeywords

        selectModifier(config.fieldModifier)
        selectFieldNameStyle(config.fieldNameStyle)
        selectFieldSortStyle(config.fieldSortStyle)
        selectExistingFieldPolicy(config.existingFieldPolicy)
        selectNullabilityMode(config.nullabilityMode)
        selectTargetLanguage(config.targetLanguage)
        selectKotlinKeyword(config.kotlinPropertyKeyword)
        chkUseDataClass.isSelected = config.isUseDataClass

        chkGenerateGetterSetter.isSelected = config.isGenerateGetterAndSetter
        chkGenerateToString.isSelected = config.isGenerateToString
        when (config.annotationType) {
            AnnotationType.GSON -> rbAnnoGson.isSelected = true
            AnnotationType.MOSHI -> rbAnnoMoshi.isSelected = true
            AnnotationType.JACKSON -> rbAnnoJackson.isSelected = true
            AnnotationType.FASTJSON -> rbAnnoFastJson.isSelected = true
            AnnotationType.KOTLIN_SERIALIZATION -> rbAnnoKotlinSerial.isSelected = true
            AnnotationType.CUSTOM -> rbAnnoCustom.isSelected = true
            else -> rbAnnoNone.isSelected = true
        }

        taAnnoImport.text = config.customAnnotationImport
        taAnnoClassFormat.text = config.customClassAnnotation
        taAnnoPropFormat.text = config.customPropertyAnnotation

        chkArrayToList.isSelected = config.isConvertArrayToList
        val builder = StringBuilder()
        config.fieldTypeConvertMap?.forEach { (key, value) ->
            builder.append(key).append("=").append(value ?: "").append("\n")
        }
        taTypeMapping.text = builder.toString()

        updateLanguageState()
        updateAnnotationState()
        updateNullabilityState()
    }

    private fun normalizeSpinnerValue(spinner: JSpinner, value: Int, defaultValue: Int): Int {
        val model = spinner.model as SpinnerNumberModel
        val minimum = model.minimum as Number
        val maximum = model.maximum as Number
        if (value < minimum.toInt()) {
            return if (defaultValue < minimum.toInt()) minimum.toInt() else defaultValue
        }
        if (value > maximum.toInt()) {
            return maximum.toInt()
        }
        return value
    }

    private fun selectFieldNameStyle(style: FieldNameStyle) {
        when (style) {
            FieldNameStyle.CAMEL_CASE -> rbNameStyleCamel.isSelected = true
            FieldNameStyle.SNAKE_CASE -> rbNameStyleSnake.isSelected = true
            else -> rbNameStyleNone.isSelected = true
        }
    }

    private fun getSelectedFieldNameStyle(): FieldNameStyle {
        if (rbNameStyleCamel.isSelected) return FieldNameStyle.CAMEL_CASE
        if (rbNameStyleSnake.isSelected) return FieldNameStyle.SNAKE_CASE
        return FieldNameStyle.NONE
    }

    private fun selectFieldSortStyle(style: FieldSortStyle) {
        when (style) {
            FieldSortStyle.FIELD_NAME_LOCAL -> rbFieldSortNameLocal.isSelected = true
            FieldSortStyle.FIELD_NAME_GLOBAL -> rbFieldSortNameGlobal.isSelected = true
            else -> rbFieldSortDefault.isSelected = true
        }
    }

    private fun getSelectedFieldSortStyle(): FieldSortStyle {
        if (rbFieldSortNameLocal.isSelected) return FieldSortStyle.FIELD_NAME_LOCAL
        if (rbFieldSortNameGlobal.isSelected) return FieldSortStyle.FIELD_NAME_GLOBAL
        return FieldSortStyle.DEFAULT
    }

    private fun selectExistingFieldPolicy(policy: ExistingFieldPolicy) {
        if (policy == ExistingFieldPolicy.OVERWRITE_OLD) {
            rbExistingFieldOverwrite.isSelected = true
        } else {
            rbExistingFieldIgnore.isSelected = true
        }
    }

    private fun getSelectedExistingFieldPolicy(): ExistingFieldPolicy {
        return if (rbExistingFieldOverwrite.isSelected) ExistingFieldPolicy.OVERWRITE_OLD else ExistingFieldPolicy.IGNORE_NEW
    }

    private fun selectModifier(modifier: Modifier) {
        when (modifier) {
            Modifier.PROTECTED -> rbModifierProtected.isSelected = true
            Modifier.PUBLIC -> rbModifierPublic.isSelected = true
            Modifier.DEFAULT -> rbModifierDefault.isSelected = true
            else -> rbModifierPrivate.isSelected = true
        }
    }

    private fun getSelectedModifier(): Modifier {
        if (rbModifierProtected.isSelected) return Modifier.PROTECTED
        if (rbModifierPublic.isSelected) return Modifier.PUBLIC
        if (rbModifierDefault.isSelected) return Modifier.DEFAULT
        return Modifier.PRIVATE
    }

    private fun selectNullabilityMode(mode: NullabilityMode) {
        when (mode) {
            NullabilityMode.NOT_NULL -> rbNullabilityNotNull.isSelected = true
            NullabilityMode.NULLABLE -> rbNullabilityNullable.isSelected = true
            else -> rbNullabilityAuto.isSelected = true
        }
    }

    private fun getSelectedNullabilityMode(): NullabilityMode {
        if (rbNullabilityNotNull.isSelected) return NullabilityMode.NOT_NULL
        if (rbNullabilityNullable.isSelected) return NullabilityMode.NULLABLE
        return NullabilityMode.AUTO
    }

    private fun selectTargetLanguage(language: TargetLanguage) {
        when (language) {
            TargetLanguage.JAVA -> rbTargetLanguageJava.isSelected = true
            TargetLanguage.KOTLIN -> rbTargetLanguageKotlin.isSelected = true
            else -> rbTargetLanguageAuto.isSelected = true
        }
    }

    private fun getSelectedTargetLanguage(): TargetLanguage {
        if (rbTargetLanguageJava.isSelected) return TargetLanguage.JAVA
        if (rbTargetLanguageKotlin.isSelected) return TargetLanguage.KOTLIN
        return TargetLanguage.AUTO
    }

    private fun selectKotlinKeyword(keyword: KotlinPropertyKeyword) {
        if (keyword == KotlinPropertyKeyword.VAL) {
            rbKotlinKeywordVal.isSelected = true
        } else {
            rbKotlinKeywordVar.isSelected = true
        }
    }

    private fun getSelectedKotlinKeyword(): KotlinPropertyKeyword {
        return if (rbKotlinKeywordVal.isSelected) KotlinPropertyKeyword.VAL else KotlinPropertyKeyword.VAR
    }

    private fun updateLanguageState() {
        val language = getSelectedTargetLanguage()
        val enableJava = TargetLanguage.JAVA == language || TargetLanguage.AUTO == language
        val enableKotlin = TargetLanguage.KOTLIN == language || TargetLanguage.AUTO == language

        setContainerEnabled(javaOptionsPanel, enableJava)
        setContainerEnabled(kotlinOptionsPanel, enableKotlin)
        updateNullabilityState()
    }

    private fun updateAnnotationState() {
        val isCustom = rbAnnoCustom.isSelected
        taAnnoImport.isEnabled = isCustom
        taAnnoClassFormat.isEnabled = isCustom
        taAnnoPropFormat.isEnabled = isCustom
    }

    private fun updateNullabilityState() {
        val isAuto = getSelectedNullabilityMode() == NullabilityMode.AUTO
        val enableKotlin = TargetLanguage.KOTLIN == getSelectedTargetLanguage() || TargetLanguage.AUTO == getSelectedTargetLanguage()
        val showAutoConfig = enableKotlin && isAuto

        spNotNullColumn.isEnabled = showAutoConfig
        tfNotNullKeywords.isEnabled = showAutoConfig
        lblNotNullColumn.isEnabled = showAutoConfig
        lblNotNullKeywords.isEnabled = showAutoConfig
        lblNullabilityTip.isEnabled = showAutoConfig
        lblNullabilityKeywordsHint.isEnabled = showAutoConfig
        lblNullabilityTip.isVisible = showAutoConfig
        panelNullabilityAutoConfig.isVisible = showAutoConfig
        rbNullabilityAuto.isEnabled = enableKotlin
        rbNullabilityNotNull.isEnabled = enableKotlin
        rbNullabilityNullable.isEnabled = enableKotlin
        kotlinOptionsPanel.revalidate()
        kotlinOptionsPanel.repaint()
    }

    private fun setContainerEnabled(container: Container?, enabled: Boolean) {
        if (container == null) return
        container.isEnabled = enabled
        for (component in container.components) {
            component.isEnabled = enabled
            if (component is Container) {
                setContainerEnabled(component, enabled)
            }
        }
    }

    private fun onOK() {
        val config = ConfigComponent.getInstance().resolveFieldParseConfig()
        config.fieldColumn = spFieldColumn.value as Int
        config.fieldTypeColumn = spFieldTypeColumn.value as Int
        config.fieldCommentColumn = spCommentColumn.value as Int
        config.fieldNotNullColumn = spNotNullColumn.value as Int
        config.fieldNameStyle = getSelectedFieldNameStyle()
        config.fieldSortStyle = getSelectedFieldSortStyle()
        config.existingFieldPolicy = getSelectedExistingFieldPolicy()
        config.notNullKeywords = tfNotNullKeywords.text
        config.nullabilityMode = getSelectedNullabilityMode()
        config.fieldModifier = getSelectedModifier()
        config.targetLanguage = getSelectedTargetLanguage()

        config.isGenerateGetterAndSetter = chkGenerateGetterSetter.isSelected
        config.isGenerateToString = chkGenerateToString.isSelected
        config.kotlinPropertyKeyword = getSelectedKotlinKeyword()
        config.isUseDataClass = chkUseDataClass.isSelected
        config.annotationType = when {
            rbAnnoGson.isSelected -> AnnotationType.GSON
            rbAnnoMoshi.isSelected -> AnnotationType.MOSHI
            rbAnnoJackson.isSelected -> AnnotationType.JACKSON
            rbAnnoFastJson.isSelected -> AnnotationType.FASTJSON
            rbAnnoKotlinSerial.isSelected -> AnnotationType.KOTLIN_SERIALIZATION
            rbAnnoCustom.isSelected -> AnnotationType.CUSTOM
            else -> AnnotationType.NONE
        }

        config.customAnnotationImport = taAnnoImport.text
        config.customClassAnnotation = taAnnoClassFormat.text
        config.customPropertyAnnotation = taAnnoPropFormat.text
        config.isConvertArrayToList = chkArrayToList.isSelected

        val convertMap = LinkedHashMap<String, String?>()
        val lines = taTypeMapping.text.split("\n")
        for (line in lines) {
            if (StringUtils.isBlank(line)) continue
            val kv = line.split("=", limit = 2)
            if (kv.size >= 2) {
                convertMap[kv[0].trim()] = kv[1].trim()
            } else {
                convertMap[kv[0].trim()] = null
            }
        }
        config.fieldTypeConvertMap = convertMap

        close(OK_EXIT_CODE)
    }

    private fun onCancel() {
        close(CANCEL_EXIT_CODE)
    }

    companion object {
        private val DIALOG_SIZE = JBUI.size(UI.Size.SETTINGS_DIALOG_WIDTH, UI.Size.SETTINGS_DIALOG_HEIGHT)
        private const val CATEGORY_SEPARATOR_OPTICAL_OFFSET = 1
        private const val NULLABILITY_AUTO_CHILD_INDENT_OFFSET = 4
    }
}
