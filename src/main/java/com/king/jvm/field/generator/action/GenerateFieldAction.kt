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

import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPackage
import com.intellij.psi.util.PsiTreeUtil
import com.king.jvm.field.generator.ui.GenerateFieldDialog
import com.king.jvm.field.generator.component.ConfigComponent
import com.king.jvm.field.generator.codegen.CodeGeneratorImpl
import com.king.jvm.field.generator.codegen.CodeGenerator
import com.king.jvm.field.generator.model.FieldParseConfig
import com.king.jvm.field.generator.model.TargetLanguage
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.util.Locale.getDefault

/**
 * 字段生成主入口动作
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
open class GenerateFieldAction : AnAction() {

    private val logger = Logger.getInstance(GenerateFieldAction::class.java)
    private var generationContext: GenerationContext? = null
    var codeGenerator: CodeGenerator = CodeGeneratorImpl()

    private val clickListener = object : GenerateFieldDialog.OnClickListener {
        override fun onGenerate(fieldParseConfig: FieldParseConfig, className: String, text: String) {
            generationContext?.let { generateField(it, fieldParseConfig, className, text, codeGenerator) }
        }

        override fun onCancel() {
            // no-op
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = resolveGenerationContext(e) != null
    }

    protected fun resolveGenerationContext(e: AnActionEvent): GenerationContext? {
        val project = e.project ?: return null
        val selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        val targetClass = resolveTargetClass(e, selectedElement)
        val targetDirectory = resolveTargetDirectory(e, targetClass, selectedElement)

        if (targetClass == null && targetDirectory == null) {
            return null
        }

        return GenerationContext(
            project = project,
            targetElement = targetClass,
            targetDirectory = targetDirectory,
            suggestedClassName = extractClassName(targetClass)
        )
    }

    protected fun generateField(
        generationContext: GenerationContext,
        fieldParseConfig: FieldParseConfig,
        className: String,
        text: String,
        codeGenerator: CodeGenerator
    ) {
        val project = generationContext.project
        if (className.isBlank()) {
            Messages.showMessageDialog("Class Name can not be null!", "Error", Messages.getInformationIcon())
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            val targetClass = resolveOrCreateTargetClass(project, generationContext, className.trim(), fieldParseConfig)
            if (targetClass == null) {
                Messages.showMessageDialog("Class can not be null!", "Error", Messages.getInformationIcon())
                return@runWriteCommandAction
            }
            try {
                codeGenerator.generate(project, targetClass, fieldParseConfig, text)
                fieldParseConfig.isLastGeneratedKotlin = targetClass is KtClass
                ConfigComponent.getInstance().resolveFieldParseConfig().isLastGeneratedKotlin = targetClass is KtClass
            } catch (e: Exception) {
                logger.error("Generate field failed", e)
                Messages.showMessageDialog(
                    project,
                    e.message ?: "Generate field failed.",
                    "Error",
                    Messages.getErrorIcon()
                )
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        generationContext = resolveGenerationContext(e)
        if (generationContext == null) {
            Messages.showMessageDialog("Please select a class or package first.", "Error", Messages.getInformationIcon())
            return
        }

        val generateDialog = GenerateFieldDialog(generationContext?.suggestedClassName)
        generateDialog.setOnClickListener(clickListener)
        generateDialog.show()
    }

    private fun resolveTargetClass(e: AnActionEvent, selectedElement: PsiElement?): PsiElement? {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (psiFile != null && editor != null) {
            findTargetClass(psiFile.findElementAt(editor.caretModel.offset))?.let { return it }
        }

        when (selectedElement) {
            is KtClass -> if (!selectedElement.isInterface()) return selectedElement
            is PsiClass -> return selectedElement
            is PsiFile -> findTopLevelClass(selectedElement)?.let { return it }
        }

        return null
    }

    private fun resolveTargetDirectory(
        e: AnActionEvent,
        targetClass: PsiElement?,
        selectedElement: PsiElement?
    ): PsiDirectory? {
        targetClass?.containingFile?.containingDirectory?.let { return it }

        when (selectedElement) {
            is PsiDirectory -> return selectedElement
            is PsiPackage -> return selectedElement.directories.firstOrNull()
            is PsiFile -> return selectedElement.containingDirectory
        }

        val ideView = e.getData(LangDataKeys.IDE_VIEW)
        if (ideView is IdeView) {
            return ideView.directories.firstOrNull()
        }

        return null
    }

    private fun resolveOrCreateTargetClass(
        project: Project,
        generationContext: GenerationContext,
        className: String,
        fieldParseConfig: FieldParseConfig
    ): PsiElement? {
        val currentTarget = generationContext.targetElement
        if (extractClassName(currentTarget).equals(className, ignoreCase = false)) {
            return currentTarget
        }

        val targetDirectory = generationContext.targetDirectory ?: currentTarget?.containingFile?.containingDirectory
        if (targetDirectory == null) {
            return null
        }

        findClassInDirectory(targetDirectory, className)?.let { return it }
        return createClassInDirectory(project, targetDirectory, className, fieldParseConfig)
    }

    private fun findTargetClass(psiElement: PsiElement?): PsiElement? {
        if (psiElement == null) {
            return null
        }
        val kotlinClass = PsiTreeUtil.getParentOfType(psiElement, KtClass::class.java)
        if (kotlinClass != null && !kotlinClass.isInterface()) {
            return kotlinClass
        }
        return PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java)
    }

    private fun findTopLevelClass(psiFile: PsiFile): PsiElement? {
        if (psiFile is PsiJavaFile) {
            return psiFile.classes.firstOrNull()
        }
        if (psiFile is KtFile) {
            return psiFile.declarations.filterIsInstance<KtClass>().firstOrNull { !it.isInterface() }
        }
        return null
    }

    private fun findClassInDirectory(directory: PsiDirectory, className: String): PsiElement? {
        val javaFile = directory.findFile("$className.java") as? PsiJavaFile
        javaFile?.classes?.firstOrNull { it.name == className }?.let { return it }

        val kotlinFile = directory.findFile("$className.kt") as? KtFile
        kotlinFile?.declarations?.filterIsInstance<KtClass>()?.firstOrNull { it.name == className }?.let { return it }

        return null
    }

    private fun createClassInDirectory(
        project: Project,
        directory: PsiDirectory,
        className: String,
        fieldParseConfig: FieldParseConfig
    ): PsiElement {
        return when (resolveTargetLanguage(directory, fieldParseConfig)) {
            TargetLanguage.KOTLIN -> createKotlinClass(project, directory, className)
            else -> JavaDirectoryService.getInstance().createClass(directory, className)
        }
    }

    private fun resolveTargetLanguage(directory: PsiDirectory, fieldParseConfig: FieldParseConfig): TargetLanguage {
        return when (fieldParseConfig.targetLanguage) {
            TargetLanguage.JAVA -> TargetLanguage.JAVA
            TargetLanguage.KOTLIN -> TargetLanguage.KOTLIN
            TargetLanguage.AUTO -> resolveAutoTargetLanguage(directory, fieldParseConfig)
        }
    }

    private fun resolveAutoTargetLanguage(directory: PsiDirectory, fieldParseConfig: FieldParseConfig): TargetLanguage {
        return when (fieldParseConfig.isLastGeneratedKotlin) {
            true -> TargetLanguage.KOTLIN
            false -> TargetLanguage.JAVA
            null -> detectDirectoryLanguage(directory)
        }
    }

    private fun detectDirectoryLanguage(directory: PsiDirectory): TargetLanguage {
        val children = directory.files
        val hasKotlin = children.any { it is KtFile || it.virtualFile.extension == "kt" }
        val hasJava = children.any { it is PsiJavaFile || it.virtualFile.extension == "java" }
        if (hasKotlin && !hasJava) {
            return TargetLanguage.KOTLIN
        }
        val directoryPath = directory.virtualFile.path.lowercase(getDefault())
        return if (directoryPath.contains("/kotlin/") || directoryPath.endsWith("/kotlin")) {
            TargetLanguage.KOTLIN
        } else {
            TargetLanguage.JAVA
        }
    }

    private fun createKotlinClass(project: Project, directory: PsiDirectory, className: String): KtClass {
        val factory = KtPsiFactory(project, false)
        val file = factory.createFile("$className.kt", "class $className {\n}")
        val addedFile = directory.add(file) as KtFile
        return addedFile.declarations.filterIsInstance<KtClass>().firstOrNull { it.name == className }
            ?: error("Failed to create Kotlin class: $className")
    }

    private fun extractClassName(targetClass: PsiElement?): String? {
        return when (targetClass) {
            is PsiClass -> targetClass.name
            is KtClass -> targetClass.name
            else -> null
        }
    }

    protected data class GenerationContext(
        val project: Project,
        val targetElement: PsiElement?,
        val targetDirectory: PsiDirectory?,
        val suggestedClassName: String?
    )
}
