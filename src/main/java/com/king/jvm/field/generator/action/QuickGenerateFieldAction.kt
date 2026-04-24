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
package com.king.jvm.field.generator.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.king.jvm.field.generator.component.ConfigComponent
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

/**
 * 从剪贴板快速生成字段的动作
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
class QuickGenerateFieldAction : GenerateFieldAction() {
    private val logger = Logger.getInstance(QuickGenerateFieldAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val text = getClipboardText()
        val generationContext = resolveGenerationContext(e)
        val className = generationContext?.suggestedClassName
        if (!text.isNullOrBlank() && generationContext != null && !className.isNullOrBlank()) {
            generateField(generationContext, ConfigComponent.getInstance().resolveFieldParseConfig(), className, text, codeGenerator)
        }
    }

    private fun getClipboardText(): String? {
        val transferable = Toolkit.getDefaultToolkit().systemClipboard.getContents(null) ?: return null
        return if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                transferable.getTransferData(DataFlavor.stringFlavor) as? String
            } catch (e: Exception) {
                logger.warn("Failed to read text from clipboard", e)
                null
            }
        } else {
            null
        }
    }
}
