package com.king.jvm.field.generator.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.editor.ex.EditorEx
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.Document

/**
 * 为文本输入区域提供行号侧栏
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
object LineNumberTextArea {

    fun createTextArea(hint: String?): JTextArea = HintTextArea(hint)

    fun wrap(textArea: JTextArea): JComponent {
        return getOrCreateController(textArea).component
    }

    fun dispose(textArea: JTextArea) {
        val controller = textArea.getClientProperty(CONTROLLER_KEY) as? EditorTextAreaController ?: return
        controller.dispose()
        textArea.putClientProperty(CONTROLLER_KEY, null)
    }

    private fun getOrCreateController(textArea: JTextArea): EditorTextAreaController {
        val existing = textArea.getClientProperty(CONTROLLER_KEY) as? EditorTextAreaController
        if (existing != null) {
            return existing
        }
        val controller = EditorTextAreaController(textArea)
        textArea.putClientProperty(CONTROLLER_KEY, controller)
        return controller
    }

    private class EditorTextAreaController(private val source: JTextArea) : PropertyChangeListener {

        private val editorDocument = EditorFactory.getInstance().createDocument(source.text)
        private val editor = EditorFactory.getInstance().createEditor(editorDocument) as EditorEx
        private var sourceDocument: Document? = null
        private var syncingFromSource = false
        private var syncingFromEditor = false
        private var disposed = false
        private val lineNumberGutter = LineNumberGutter()
        private val visibleAreaListener = VisibleAreaListener {
            lineNumberGutter.repaint()
        }
        private val caretListener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                lineNumberGutter.repaint()
            }
        }

        val component: JComponent = JPanel(BorderLayout()).apply {
            isOpaque = true
            add(lineNumberGutter, BorderLayout.WEST)
            add(editor.component, BorderLayout.CENTER)
        }

        private val sourceListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = syncFromSource()
            override fun removeUpdate(e: DocumentEvent) = syncFromSource()
            override fun changedUpdate(e: DocumentEvent) = syncFromSource()
        }

        private val editorListener = object : com.intellij.openapi.editor.event.DocumentListener {
            override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                syncFromEditor()
            }
        }

        init {
            configureEditor()
            attachSourceDocument(source.document)
            editorDocument.addDocumentListener(editorListener)
            editor.scrollingModel.addVisibleAreaListener(visibleAreaListener)
            editor.caretModel.addCaretListener(caretListener)
            source.addPropertyChangeListener(this)
            syncStyle()
        }

        override fun propertyChange(evt: PropertyChangeEvent) {
            when (evt.propertyName) {
                "document" -> {
                    attachSourceDocument(source.document)
                    syncFromSource()
                }

                "font", "margin", "background", "foreground", "enabled", "editable", "alignmentX" -> syncStyle()
            }
        }

        fun dispose() {
            if (disposed) {
                return
            }
            disposed = true
            source.removePropertyChangeListener(this)
            sourceDocument?.removeDocumentListener(sourceListener)
            editorDocument.removeDocumentListener(editorListener)
            editor.scrollingModel.removeVisibleAreaListener(visibleAreaListener)
            editor.caretModel.removeCaretListener(caretListener)
            EditorFactory.getInstance().releaseEditor(editor)
        }

        private fun configureEditor() {
            editor.settings.apply {
                isLineNumbersShown = false
                isFoldingOutlineShown = false
                isLineMarkerAreaShown = false
                isRightMarginShown = false
                isWhitespacesShown = false
                isIndentGuidesShown = false
                additionalColumnsCount = 1
                additionalLinesCount = 0
            }
            editor.setHorizontalScrollbarVisible(true)
            editor.setVerticalScrollbarVisible(true)
        }

        private fun attachSourceDocument(document: Document?) {
            if (sourceDocument === document) {
                return
            }
            sourceDocument?.removeDocumentListener(sourceListener)
            sourceDocument = document
            sourceDocument?.addDocumentListener(sourceListener)
        }

        private fun syncStyle() {
            val editorBackground = resolveEditorBackground()
            component.alignmentX = source.alignmentX
            component.border = resolveContainerBorder()
            component.background = editorBackground
            editor.component.border = EmptyBorder(0, 0, 0, 0)
            editor.component.background = editorBackground
            editor.component.isEnabled = source.isEnabled
            editor.contentComponent.isEnabled = source.isEnabled
            editor.contentComponent.isFocusable = source.isEnabled
            editor.contentComponent.background = editorBackground
            editor.backgroundColor = editorBackground
            lineNumberGutter.background = editorBackground
            lineNumberGutter.foreground = resolveLineNumberForeground()
            applyMinimumLineNumberGutterWidth()
            applyHint()
        }

        private fun resolveEditorBackground(): Color {
            return editor.colorsScheme.defaultBackground
        }

        private fun applyMinimumLineNumberGutterWidth(minDigits: Int = MIN_LINE_NUMBER_DIGITS) {
            val metrics = editor.contentComponent.getFontMetrics(editor.contentComponent.font)
            val digitWidth = maxOf(1, metrics.charWidth('0'))
            val lineCountDigits = maxOf(1, editorDocument.lineCount.toString().length)
            val digits = maxOf(minDigits, lineCountDigits)
            val width = (digitWidth * digits) + LINE_NUMBER_LEFT_PADDING + LINE_NUMBER_RIGHT_PADDING
            lineNumberGutter.preferredSize = Dimension(width, 0)
            lineNumberGutter.minimumSize = Dimension(width, 0)
            lineNumberGutter.revalidate()
            lineNumberGutter.repaint()
        }

        private fun resolveLineNumberForeground(): Color {
            return resolveSchemeColor(EditorColors.LINE_NUMBERS_COLOR)
                ?: editor.gutterComponentEx.foreground
                ?: editor.contentComponent.foreground
                ?: UIManager.getColor("Editor.lineNumberColor")
                ?: UIManager.getColor("Label.disabledForeground")
                ?: UIManager.getColor("Label.foreground")
                ?: Color.GRAY
        }

        private fun resolveCaretLineNumberForeground(defaultColor: Color): Color {
            return resolveSchemeColor(EditorColors.LINE_NUMBER_ON_CARET_ROW_COLOR)
                ?: UIManager.getColor("Editor.lineNumberOnCaretRowColor")
                ?: UIManager.getColor("Editor.lineNumberColor")
                ?: resolveSchemeColor(EditorColors.LINE_NUMBERS_COLOR)
                ?: editor.gutterComponentEx.foreground
                ?: defaultColor
        }

        private fun resolveSchemeColor(colorKey: ColorKey): Color? {
            return runCatching { editor.colorsScheme.getColor(colorKey) }.getOrNull()
        }

        private fun applyHint() {
            val hint = (source as? HintTextArea)?.hint
            editor.setPlaceholder(hint)
        }

        private fun syncFromSource() {
            if (disposed || syncingFromEditor) {
                return
            }
            val newText = source.text
            if (editorDocument.text == newText) {
                return
            }
            syncingFromSource = true
            try {
                updateEditorDocument(newText)
            } finally {
                syncingFromSource = false
            }
            applyMinimumLineNumberGutterWidth()
        }

        private fun syncFromEditor() {
            if (disposed || syncingFromSource) {
                return
            }
            val newText = editorDocument.text
            if (source.text == newText) {
                return
            }
            syncingFromEditor = true
            try {
                source.text = newText
            } finally {
                syncingFromEditor = false
            }
            applyMinimumLineNumberGutterWidth()
        }

        private inner class LineNumberGutter : JComponent() {
            init {
                isOpaque = true
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2 = g.create() as Graphics2D
                try {
                    g2.color = background
                    g2.fillRect(0, 0, width, height)

                    val visibleArea = editor.scrollingModel.visibleArea
                    val lineHeight = editor.lineHeight
                    val lineCount = maxOf(1, editorDocument.lineCount)
                    if (lineHeight <= 0) {
                        paintDivider(g2)
                        return
                    }

                    val metrics = editor.contentComponent.getFontMetrics(editor.contentComponent.font)
                    val startLine = maxOf(0, visibleArea.y / lineHeight)
                    val endLine = minOf(lineCount - 1, (visibleArea.y + visibleArea.height) / lineHeight + 1)
                    val caretLine = editor.caretModel.logicalPosition.line
                    val defaultForeground = foreground
                    val caretForeground = resolveCaretLineNumberForeground(defaultForeground)

                    g2.font = editor.contentComponent.font
                    for (line in startLine..endLine) {
                        val lineNumber = (line + 1).toString()
                        val textWidth = metrics.stringWidth(lineNumber)
                        val x = width - LINE_NUMBER_RIGHT_PADDING - textWidth
                        val lineTop = (line * lineHeight) - visibleArea.y
                        val y = lineTop + ((lineHeight - metrics.height) / 2) + metrics.ascent
                        g2.color = if (line == caretLine) caretForeground else defaultForeground
                        g2.drawString(lineNumber, x, y)
                    }

                    paintDivider(g2)
                } finally {
                    g2.dispose()
                }
            }

            private fun paintDivider(g: Graphics2D) {
                g.color = resolveThemeBorderColor()
                g.drawLine(width - 1, 0, width - 1, height)
            }
        }

        private fun updateEditorDocument(text: String) {
            if (disposed) {
                return
            }
            val update = {
                if (!disposed && editorDocument.text != text) {
                    editorDocument.setText(text)
                }
            }
            val application = ApplicationManager.getApplication()
            if (application == null) {
                update()
                return
            }
            if (!application.isDispatchThread) {
                SwingUtilities.invokeLater { updateEditorDocument(text) }
                return
            }
            if (application.isWriteAccessAllowed) {
                update()
            } else {
                application.runWriteAction(update)
            }
        }
    }

    private fun resolveContainerBorder(): Border = MatteBorder(
        1,
        1,
        1,
        1,
        resolveThemeBorderColor()
    )


    private fun resolveThemeBorderColor(): Color {
        return UIManager.getColor("EditorGutter.borderColor")
            ?: UIManager.getColor("EditorPane.borderColor")
            ?: UIManager.getColor("TextField.borderColor")
            ?: UIManager.getColor("Component.borderColor")
            ?: UIManager.getColor("Separator.foreground")
            ?: UIManager.getColor("Label.disabledForeground")
            ?: Color.GRAY
    }

    private class HintTextArea(val hint: String?) : JTextArea() {
        init {
            isOpaque = true
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (text.isNotEmpty() || hint.isNullOrEmpty()) {
                return
            }

            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                var hintColor = UIManager.getColor("TextField.inactiveForeground")
                if (hintColor == null) {
                    hintColor = UIManager.getColor("Label.disabledForeground")
                }
                if (hintColor == null) {
                    hintColor = foreground.brighter()
                }
                val background = background
                if (background != null) {
                    hintColor = Color(
                        (hintColor.red + background.red) / 2,
                        (hintColor.green + background.green) / 2,
                        (hintColor.blue + background.blue) / 2
                    )
                }
                g2.color = hintColor
                g2.font = font
                val insets: Insets = insets
                val metrics: FontMetrics = g2.fontMetrics
                val x = insets.left + 2
                val y = insets.top + metrics.ascent
                g2.drawString(hint, x, y)
            } finally {
                g2.dispose()
            }
        }
    }

    private const val CONTROLLER_KEY = "LineNumberTextArea.Controller"
    private const val MIN_LINE_NUMBER_DIGITS = 4
    private const val LINE_NUMBER_LEFT_PADDING = 8
    private const val LINE_NUMBER_RIGHT_PADDING = 8
}
