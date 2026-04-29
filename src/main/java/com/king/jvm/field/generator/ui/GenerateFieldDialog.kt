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

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.JBUI
import com.king.jvm.field.generator.component.ConfigComponent
import com.king.jvm.field.generator.model.FieldParseConfig
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.regex.Pattern
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * 用于生成字段的主对话框
 *
 * @author <a href="mailto:yujinlin@mail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
class GenerateFieldDialog(initialClassName: String?) : DialogWrapper(true) {

    private val panel: JPanel
    private lateinit var jTextAreaInput: JTextArea
    private lateinit var tfClassName: JTextField
    private lateinit var chkRegexValidation: JCheckBox
    private lateinit var btnCancel: JButton
    private lateinit var btnGenerate: JButton
    private lateinit var btnSetting: JButton
    private var onClickListener: OnClickListener? = null

    private var fieldParseConfig: FieldParseConfig = ConfigComponent.getInstance().resolveFieldParseConfig()

    init {
        panel = initUiComponents(initialClassName)
        title = "JvmFieldGenerator - Generate Fields"
        init()
        registerEscapeAction()
        setupActions()
        applyPrimaryButtonStyle()
    }

    override fun createCenterPanel(): JComponent = panel

    override fun createActions(): Array<javax.swing.Action> = emptyArray()

    private fun setupActions() {
        btnGenerate.addActionListener {
            val className = tfClassName.text
            val inputText = jTextAreaInput.text
            if (className.isNullOrBlank()) {
                Messages.showMessageDialog("Class name cannot be empty.", "Error", Messages.getInformationIcon())
                return@addActionListener
            }
            if (inputText.isNullOrBlank()) {
                Messages.showMessageDialog("Input text cannot be empty.", "Error", Messages.getInformationIcon())
                return@addActionListener
            }
            val validationError = getInputValidationError()
            if (validationError != null) {
                Messages.showMessageDialog(validationError, "Error", Messages.getInformationIcon())
                return@addActionListener
            }
            onClickListener?.onGenerate(fieldParseConfig, className.trim(), inputText)
            close(OK_EXIT_CODE)
        }
        btnCancel.addActionListener {
            onClickListener?.onCancel()
            close(CANCEL_EXIT_CODE)
        }
        btnSetting.addActionListener {
            try {
                val settingsDialog = GenerateFieldSettingsDialog()
                settingsDialog.show()
                fieldParseConfig = ConfigComponent.getInstance().resolveFieldParseConfig()
                updateGenerateButtonState()
            } catch (ex: Exception) {
                LOG.error("Open settings dialog failed", ex)
                Messages.showErrorDialog(contentPanel, ex.message ?: "Failed to open settings.", "Settings")
            }
        }
    }

    private fun initUiComponents(initialClassName: String?): JPanel {
        val container = JPanel(BorderLayout(0, JBUI.scale(UI.Spacing.DIALOG_CONTENT_GAP)))
        container.preferredSize = DIALOG_SIZE
        container.border = JBUI.Borders.empty(
            UI.Spacing.DIALOG_TOP_PADDING,
            UI.Spacing.DIALOG_HORIZONTAL_PADDING,
            UI.Spacing.DIALOG_BOTTOM_PADDING,
            UI.Spacing.DIALOG_HORIZONTAL_PADDING
        )

        container.add(createHeaderPanel(), BorderLayout.NORTH)

        jTextAreaInput = JTextArea().apply {
            font = font.deriveFont(font.size2D + UI.FontSize.INPUT_TEXT)
        }
        val scrollPane: JScrollPane = LineNumberTextArea.wrap(jTextAreaInput)
        scrollPane.preferredSize = JBUI.size(UI.Size.MAIN_INPUT_WIDTH, UI.Size.MAIN_INPUT_HEIGHT)

        val centerPanel = JPanel(BorderLayout(0, JBUI.scale(UI.Spacing.DIALOG_CONTENT_GAP)))
        centerPanel.add(scrollPane, BorderLayout.CENTER)
        centerPanel.add(createInputOptionsPanel(initialClassName), BorderLayout.SOUTH)
        container.add(centerPanel, BorderLayout.CENTER)

        container.add(createButtonPanel(), BorderLayout.SOUTH)
        bindValidation()
        updateGenerateButtonState()
        return container
    }

    private fun createInputOptionsPanel(initialClassName: String?): JPanel {
        val optionsPanel = JPanel()
        optionsPanel.isOpaque = false
        optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)
        optionsPanel.add(createInputValidationPanel())
        optionsPanel.add(Box.createVerticalStrut(JBUI.scale(UI.Spacing.DIALOG_CONTENT_GAP)))
        optionsPanel.add(createClassNamePanel(initialClassName))
        return optionsPanel
    }

    private fun createHeaderPanel(): JPanel {
        val headerPanel = JPanel(BorderLayout(JBUI.scale(UI.Spacing.ROW_GAP_COMPACT), 0))
        headerPanel.isOpaque = false

        val titleLabel = JLabel("Generate fields from text:")
        titleLabel.font = titleLabel.font.deriveFont(titleLabel.font.size2D + UI.FontSize.CONTENT_TEXT)
        headerPanel.add(titleLabel, BorderLayout.WEST)
        headerPanel.add(createRepositoryLinkLabel(), BorderLayout.EAST)
        return headerPanel
    }

    private fun createRepositoryLinkLabel(): JLabel {
        val linkLabel = JLabel("<html><a href=\"$REPOSITORY_URL\">$REPOSITORY_URL</a></html>")
        linkLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        linkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                BrowserUtil.browse(REPOSITORY_URL)
            }
        })
        return linkLabel
    }

    private fun createInputValidationPanel(): JPanel {
        val validationPanel = JPanel()
        validationPanel.isOpaque = false
        validationPanel.layout = BoxLayout(validationPanel, BoxLayout.Y_AXIS)
        validationPanel.alignmentX = JComponent.LEFT_ALIGNMENT

        chkRegexValidation = JCheckBox("Validate input with regex")
        chkRegexValidation.isSelected = fieldParseConfig.isEnableInputRegexValidation
        chkRegexValidation.alignmentX = JComponent.LEFT_ALIGNMENT
        validationPanel.add(chkRegexValidation)
        return validationPanel
    }

    private fun createClassNamePanel(initialClassName: String?): JPanel {
        val classNamePanel = JPanel(BorderLayout(JBUI.scale(UI.Spacing.ROW_GAP_COMPACT), 0))
        classNamePanel.isOpaque = false
        classNamePanel.alignmentX = JComponent.LEFT_ALIGNMENT
        classNamePanel.preferredSize = JBUI.size(0, UI.Size.INPUT_HEIGHT)
        classNamePanel.minimumSize = JBUI.size(0, UI.Size.INPUT_HEIGHT)

        val label = JLabel("Class Name")
        label.horizontalAlignment = SwingConstants.LEFT
        label.font = label.font.deriveFont(label.font.size2D + UI.FontSize.CONTENT_TEXT)
        classNamePanel.add(label, BorderLayout.WEST)

        tfClassName = JTextField(initialClassName.orEmpty())
        tfClassName.font = tfClassName.font.deriveFont(tfClassName.font.size2D + UI.FontSize.INPUT_TEXT)
        tfClassName.margin = JBUI.insets(
            UI.Spacing.INPUT_VERTICAL_PADDING,
            UI.Spacing.INPUT_HORIZONTAL_PADDING,
            UI.Spacing.INPUT_VERTICAL_PADDING,
            UI.Spacing.INPUT_HORIZONTAL_PADDING
        )
        val inputSize = Dimension(Int.MAX_VALUE, JBUI.scale(UI.Size.INPUT_HEIGHT))
        tfClassName.preferredSize = inputSize
        tfClassName.minimumSize = inputSize
        tfClassName.maximumSize = inputSize
        classNamePanel.add(tfClassName, BorderLayout.CENTER)
        return classNamePanel
    }

    private fun createButtonPanel(): JPanel {
        val buttonPanel = JPanel(BorderLayout())
        buttonPanel.isOpaque = false
        btnSetting = JButton("Settings")
        buttonPanel.add(btnSetting, BorderLayout.WEST)

        val actionButtonPanel = JPanel()
        actionButtonPanel.isOpaque = false
        actionButtonPanel.layout = BoxLayout(actionButtonPanel, BoxLayout.X_AXIS)
        btnGenerate = JButton("Generate")
        btnCancel = JButton("Cancel")
        actionButtonPanel.add(btnGenerate)
        actionButtonPanel.add(Box.createHorizontalStrut(JBUI.scale(UI.Spacing.BUTTON_GAP)))
        actionButtonPanel.add(btnCancel)
        buttonPanel.add(actionButtonPanel, BorderLayout.EAST)
        return buttonPanel
    }

    private fun applyPrimaryButtonStyle() {
        btnGenerate.putClientProperty("JButton.buttonType", "default")
        rootPane.defaultButton = btnGenerate
    }

    private fun bindValidation() {
        val listener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateGenerateButtonState()
            override fun removeUpdate(e: DocumentEvent) = updateGenerateButtonState()
            override fun changedUpdate(e: DocumentEvent) = updateGenerateButtonState()
        }
        jTextAreaInput.document.addDocumentListener(listener)
        tfClassName.document.addDocumentListener(listener)
        chkRegexValidation.addActionListener {
            fieldParseConfig.isEnableInputRegexValidation = chkRegexValidation.isSelected
            updateGenerateButtonState()
        }
    }

    private fun updateGenerateButtonState() {
        btnGenerate.isEnabled = !(
            tfClassName.text.isNullOrBlank() ||
                jTextAreaInput.text.isNullOrBlank() ||
                getInputValidationError() != null
            )

    }

    private fun getInputValidationError(): String? {
        if (!chkRegexValidation.isSelected) {
            return null
        }
        val inputText = jTextAreaInput.text
        for (line in inputText.split(Regex("\\r?\\n"))) {
            if (line.isBlank()) {
                continue
            }
            if (!INPUT_VALIDATION_PATTERN.matcher(line).matches()) {
                return "Input text does not match the validation regex: $INPUT_VALIDATION_REGEX"
            }
        }
        return null
    }

    private fun registerEscapeAction() {
        panel.registerKeyboardAction(
            {
                onClickListener?.onCancel()
                close(CANCEL_EXIT_CODE)
            },
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )
    }

    fun setOnClickListener(onClickListener: OnClickListener?) {
        this.onClickListener = onClickListener
    }

    interface OnClickListener {
        fun onGenerate(fieldParseConfig: FieldParseConfig, className: String, text: String)
        fun onCancel()
    }

    companion object {
        private val DIALOG_SIZE = JBUI.size(UI.Size.MAIN_DIALOG_WIDTH, UI.Size.MAIN_DIALOG_HEIGHT)
        private val LOG = Logger.getInstance(GenerateFieldDialog::class.java)
        private const val INPUT_VALIDATION_REGEX = "[^\\t]+\\t[^\\t]*(?:\\t[^\\t]*)*"
        private val INPUT_VALIDATION_PATTERN: Pattern = Pattern.compile(INPUT_VALIDATION_REGEX)
        private const val REPOSITORY_URL = "https://github.com/jenly1314/JvmFieldGenerator"
    }
}
