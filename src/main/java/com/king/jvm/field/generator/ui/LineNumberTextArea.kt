package com.king.jvm.field.generator.ui

import java.awt.Color
import java.awt.ComponentOrientation
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.UIManager
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
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

    fun wrap(textArea: JTextArea): JScrollPane {
        val scrollPane = JScrollPane(textArea)
        install(scrollPane, textArea)
        return scrollPane
    }

    fun install(scrollPane: JScrollPane, textArea: JTextArea) {
        val lineNumberArea = LineNumberArea(textArea)
        scrollPane.setRowHeaderView(lineNumberArea)
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, createCorner(lineNumberArea))
    }

    private fun createCorner(lineNumberArea: JTextArea): JComponent {
        val panel = JPanel()
        panel.background = lineNumberArea.background
        panel.border = lineNumberArea.border
        return panel
    }

    private class LineNumberArea(private val source: JTextArea) : JTextArea(), DocumentListener, PropertyChangeListener {

        private var currentDocument: Document? = null

        init {
            isEditable = false
            isFocusable = false
            isOpaque = true
            lineWrap = false
            wrapStyleWord = false
            highlighter = null
            border = DEFAULT_BORDER
            syncStyle()
            updateLineNumbers()
            currentDocument = source.document
            currentDocument?.addDocumentListener(this)
            source.addPropertyChangeListener(this)
        }

        override fun insertUpdate(e: DocumentEvent) {
            updateLineNumbers()
        }

        override fun removeUpdate(e: DocumentEvent) {
            updateLineNumbers()
        }

        override fun changedUpdate(e: DocumentEvent) {
            updateLineNumbers()
        }

        override fun propertyChange(evt: PropertyChangeEvent) {
            val propertyName = evt.propertyName
            if ("document" == propertyName) {
                currentDocument?.removeDocumentListener(this)
                currentDocument = source.document
                currentDocument?.addDocumentListener(this)
                syncStyle()
                updateLineNumbers()
                return
            }
            if ("font" == propertyName || "margin" == propertyName) {
                syncStyle()
                updateLineNumbers()
            }
        }

        private fun syncStyle() {
            font = source.font
            margin = source.margin
            var background = UIManager.getColor("Panel.background")
            if (background == null) {
                background = source.background
            }
            this.background = background
            var foreground = UIManager.getColor("Label.disabledForeground")
            if (foreground == null) {
                foreground = source.foreground.darker()
            }
            this.foreground = foreground
            componentOrientation = ComponentOrientation.LEFT_TO_RIGHT
        }

        private fun updateLineNumbers() {
            val lineCount = maxOf(1, source.lineCount)
            val builder = StringBuilder(lineCount * 3)
            for (i in 1..lineCount) {
                builder.append(i).append(System.lineSeparator())
            }
            text = builder.toString()
            columns = maxOf(3, lineCount.toString().length + 1)
            rows = lineCount
            caretPosition = 0
            revalidate()
            repaint()
        }

        companion object {
            private val DEFAULT_BORDER: Border = CompoundBorder(
                MatteBorder(
                    0,
                    0,
                    0,
                    1,
                    UIManager.getColor("Component.borderColor") ?: UI.Color.BORDER_FALLBACK
                ),
                EmptyBorder(0, 8, 0, 8)
            )
        }
    }

    private class HintTextArea(private val hint: String?) : JTextArea() {
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
}
