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
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.JavaPsiFacade
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
        val context = resolveGenerationContext(e)
        val isAvailable = context != null
        e.presentation.isVisible = isAvailable
        e.presentation.isEnabled = isAvailable
    }

    protected fun resolveGenerationContext(e: AnActionEvent): GenerationContext? {
        val project = e.project ?: return null
        val selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        val targetClass = resolveTargetClass(e, selectedElement)
        val targetFile = resolveTargetFile(e, selectedElement, targetClass)
        val targetDirectory = resolveTargetDirectory(e, targetClass, selectedElement)

        if (targetClass == null && targetDirectory == null) {
            return null
        }

        return GenerationContext(
            project = project,
            targetElement = targetClass,
            targetFile = targetFile,
            targetDirectory = targetDirectory,
            suggestedClassName = resolveSuggestedClassName(targetClass, targetFile),
            preferredLanguage = resolvePreferredLanguage(targetClass, targetFile),
            targetPackageName = resolveTargetPackageName(project, e, targetClass, targetDirectory, selectedElement)
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
        val parsedClassNameInput = parseClassNameInput(className)
        if (parsedClassNameInput.className.isBlank()) {
            Messages.showMessageDialog("Class Name can not be null!", "Error", Messages.getInformationIcon())
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val targetClass = resolveOrCreateTargetClass(
                    project = project,
                    generationContext = generationContext,
                    className = parsedClassNameInput.className,
                    fieldParseConfig = fieldParseConfig,
                    preferredLanguage = parsedClassNameInput.preferredLanguage ?: generationContext.preferredLanguage
                )
                if (targetClass == null) {
                    Messages.showMessageDialog("Class can not be null!", "Error", Messages.getInformationIcon())
                    return@runWriteCommandAction
                }
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
        when (selectedElement) {
            is KtClass -> if (!selectedElement.isInterface()) return selectedElement
            is PsiClass -> return selectedElement
            is PsiFile -> findTopLevelClass(selectedElement)?.let { return it }
            is PsiDirectory, is PsiPackage -> return null
        }
        if (isProjectViewContext(e, selectedElement)) {
            return null
        }

        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (psiFile != null && editor != null) {
            findTargetClass(psiFile.findElementAt(editor.caretModel.offset))?.let { return it }
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

    private fun resolveTargetFile(e: AnActionEvent, selectedElement: PsiElement?, targetClass: PsiElement?): PsiFile? {
        when (selectedElement) {
            is PsiFile -> return selectedElement
            is KtClass, is PsiClass -> return selectedElement.containingFile
        }
        return targetClass?.containingFile ?: e.getData(CommonDataKeys.PSI_FILE)
    }

    private fun resolveOrCreateTargetClass(
        project: Project,
        generationContext: GenerationContext,
        className: String,
        fieldParseConfig: FieldParseConfig,
        preferredLanguage: TargetLanguage?
    ): PsiElement? {
        val currentTarget = generationContext.targetElement
        if (extractClassName(currentTarget).equals(className, ignoreCase = false)) {
            return currentTarget
        }

        val targetDirectory = generationContext.targetDirectory ?: currentTarget?.containingFile?.containingDirectory
        if (targetDirectory == null) {
            return null
        }

        findOrCreateClassInTargetFile(
            project = project,
            targetFile = generationContext.targetFile,
            className = className,
            targetPackageName = generationContext.targetPackageName
        )?.let { return it }

        findClassInDirectory(targetDirectory, className)?.let { return it }
        return createClassInDirectory(
            project = project,
            directory = targetDirectory,
            className = className,
            fieldParseConfig = fieldParseConfig,
            targetPackageName = generationContext.targetPackageName,
            preferredLanguage = preferredLanguage
        )
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

    private fun findOrCreateClassInTargetFile(
        project: Project,
        targetFile: PsiFile?,
        className: String,
        targetPackageName: String?
    ): PsiElement? {
        if (targetFile == null || targetFile.virtualFile == null) {
            return null
        }
        val targetFileClassName = targetFile.virtualFile.nameWithoutExtension
        if (targetFileClassName != className) {
            return null
        }
        return when (targetFile) {
            is PsiJavaFile -> {
                targetFile.classes.firstOrNull { it.name == className }
                    ?: createJavaClassInExistingFile(project, targetFile, className, targetPackageName)
            }
            is KtFile -> {
                targetFile.declarations.filterIsInstance<KtClass>().firstOrNull { it.name == className }
                    ?: createKotlinClassInExistingFile(project, targetFile, className, targetPackageName)
            }
            else -> null
        }
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
        fieldParseConfig: FieldParseConfig,
        targetPackageName: String?,
        preferredLanguage: TargetLanguage?
    ): PsiElement {
        return when (preferredLanguage ?: resolveTargetLanguage(directory, fieldParseConfig)) {
            TargetLanguage.KOTLIN -> createKotlinClass(project, directory, className, targetPackageName)
            else -> createJavaClass(project, directory, className, targetPackageName)
        }
    }

    private fun parseClassNameInput(classNameInput: String): ParsedClassNameInput {
        val trimmed = classNameInput.trim()
        return when {
            trimmed.endsWith(".java", ignoreCase = true) -> ParsedClassNameInput(
                className = trimmed.dropLast(5).trim(),
                preferredLanguage = TargetLanguage.JAVA
            )
            trimmed.endsWith(".kt", ignoreCase = true) -> ParsedClassNameInput(
                className = trimmed.dropLast(3).trim(),
                preferredLanguage = TargetLanguage.KOTLIN
            )
            else -> ParsedClassNameInput(className = trimmed, preferredLanguage = null)
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
        val directoryPath = directory.virtualFile.path
            .replace('\\', '/')
            .lowercase(getDefault())
        return if (directoryPath.contains("/kotlin/") || directoryPath.endsWith("/kotlin")) {
            TargetLanguage.KOTLIN
        } else {
            TargetLanguage.JAVA
        }
    }

    private fun createJavaClass(project: Project, directory: PsiDirectory, className: String, targetPackageName: String?): PsiClass {
        val psiClass = JavaDirectoryService.getInstance().createClass(directory, className)
        val packageName = resolvePackageName(project, directory, targetPackageName)
        if (packageName.isNotBlank()) {
            (psiClass.containingFile as? PsiJavaFile)?.packageName = packageName
        }
        return psiClass
    }

    private fun createKotlinClass(project: Project, directory: PsiDirectory, className: String, targetPackageName: String?): KtClass {
        val packageName = resolvePackageName(project, directory, targetPackageName)
        val classText = buildString {
            if (packageName.isNotBlank()) {
                append("package ").append(packageName).append("\n\n")
            }
            append("class ").append(className).append(" {\n}")
        }
        val factory = KtPsiFactory(project, false)
        val file = factory.createFile("$className.kt", classText)
        val addedFile = directory.add(file) as KtFile
        return addedFile.declarations.filterIsInstance<KtClass>().firstOrNull { it.name == className }
            ?: error("Failed to create Kotlin class: $className")
    }

    private fun createJavaClassInExistingFile(
        project: Project,
        targetFile: PsiJavaFile,
        className: String,
        targetPackageName: String?
    ): PsiClass? {
        val containingDirectory = targetFile.containingDirectory ?: return null
        val packageName = resolvePackageName(project, containingDirectory, targetPackageName)
        if (targetFile.text.isBlank()) {
            val fileText = buildString {
                if (packageName.isNotBlank()) {
                    append("package ").append(packageName).append(";\n\n")
                }
                append("public class ").append(className).append(" {\n}")
            }
            if (!replaceFileText(project, targetFile, fileText)) {
                return null
            }
            return targetFile.classes.firstOrNull { it.name == className }
        }
        if (packageName.isNotBlank()) {
            targetFile.packageName = packageName
        }
        val packageStatement = targetFile.packageStatement
        if (targetFile.classes.isEmpty() && packageStatement != null) {
            val remainingText = targetFile.text.substring(packageStatement.textRange.endOffset).trim()
            if (remainingText.isEmpty()) {
                val classText = JavaPsiFacade.getElementFactory(project).createClass(className).text
                val fileText = buildString {
                    append(packageStatement.text).append("\n\n")
                    append(classText)
                }
                if (!replaceFileText(project, targetFile, fileText)) {
                    return null
                }
                return targetFile.classes.firstOrNull { it.name == className }
            }
        }
        val psiClass = JavaPsiFacade.getElementFactory(project).createClass(className)
        return targetFile.add(psiClass) as? PsiClass
    }

    private fun createKotlinClassInExistingFile(
        project: Project,
        targetFile: KtFile,
        className: String,
        targetPackageName: String?
    ): KtClass? {
        val containingDirectory = targetFile.containingDirectory ?: return null
        val packageName = resolvePackageName(project, containingDirectory, targetPackageName)
        if (targetFile.text.isBlank()) {
            val fileText = buildString {
                if (packageName.isNotBlank()) {
                    append("package ").append(packageName).append("\n\n")
                }
                append("class ").append(className).append(" {\n}")
            }
            if (!replaceFileText(project, targetFile, fileText)) {
                return null
            }
            return targetFile.declarations.filterIsInstance<KtClass>().firstOrNull { it.name == className }
        }
        val factory = KtPsiFactory(project, false)
        val addedClass = targetFile.add(factory.createClass("class $className {\n}")) as? KtClass
        return addedClass
    }

    private fun replaceFileText(project: Project, targetFile: PsiFile, text: String): Boolean {
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(targetFile) ?: return false
        document.setText(text)
        documentManager.commitDocument(document)
        return true
    }

    private fun resolveTargetPackageName(
        project: Project,
        e: AnActionEvent,
        targetClass: PsiElement?,
        targetDirectory: PsiDirectory?,
        selectedElement: PsiElement?
    ): String? {
        if (selectedElement is PsiPackage) {
            return selectedElement.qualifiedName
        }
        when (targetClass) {
            is PsiClass -> return (targetClass.containingFile as? PsiJavaFile)?.packageName
            is KtClass -> return targetClass.containingKtFile.packageFqName.asString()
        }
        targetDirectory?.let { resolveDirectoryPackageName(project, it)?.let { packageName -> return packageName } }
        val ideView = e.getData(LangDataKeys.IDE_VIEW)
        if (ideView is IdeView) {
            ideView.directories.forEach { directory ->
                resolveDirectoryPackageName(project, directory)?.let { packageName -> return packageName }
            }
        }
        return null
    }

    private fun resolvePackageName(project: Project, directory: PsiDirectory, targetPackageName: String?): String {
        if (!targetPackageName.isNullOrBlank()) {
            return targetPackageName
        }
        return resolveDirectoryPackageName(project, directory).orEmpty()
    }

    private fun resolveDirectoryPackageName(project: Project, directory: PsiDirectory): String? {
        val packageFromJavaDirectoryService = JavaDirectoryService.getInstance().getPackage(directory)?.qualifiedName
        if (!packageFromJavaDirectoryService.isNullOrBlank()) {
            return packageFromJavaDirectoryService
        }
        resolvePackageFromSourceRoot(project, directory)?.let { return it }
        return resolvePackageFromPathPattern(directory)
    }

    private fun resolvePackageFromSourceRoot(project: Project, directory: PsiDirectory): String? {
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val sourceRoot = fileIndex.getSourceRootForFile(directory.virtualFile) ?: return null
        val relativePath = VfsUtilCore.getRelativePath(directory.virtualFile, sourceRoot, '/') ?: return null
        val packageName = relativePath
            .split('/')
            .filter { it.isNotBlank() }
            .joinToString(".")
        return packageName.takeIf { it.isNotBlank() }
    }

    private fun resolvePackageFromPathPattern(directory: PsiDirectory): String? {
        val normalizedPath = directory.virtualFile.path.replace('\\', '/')
        val lowerPath = normalizedPath.lowercase(getDefault())
        val markers = listOf("/main/java/", "/main/kotlin/")
        for (marker in markers) {
            val markerIndex = lowerPath.indexOf(marker)
            if (markerIndex < 0) {
                continue
            }
            val packagePath = normalizedPath.substring(markerIndex + marker.length).trim('/')
            if (packagePath.isBlank()) {
                continue
            }
            return packagePath.split('/').filter { it.isNotBlank() }.joinToString(".")
        }
        return null
    }

    private fun isProjectViewContext(e: AnActionEvent, selectedElement: PsiElement?): Boolean {
        if (e.place == ActionPlaces.PROJECT_VIEW_POPUP) {
            return true
        }
        return selectedElement is PsiDirectory || selectedElement is PsiPackage
    }

    private fun resolveSuggestedClassName(targetClass: PsiElement?, targetFile: PsiFile?): String? {
        val className = extractClassName(targetClass)
        if (!className.isNullOrBlank()) {
            return className
        }
        val virtualFile = targetFile?.virtualFile ?: return null
        val extension = virtualFile.extension?.lowercase(getDefault())
        return if (extension == "java" || extension == "kt") {
            virtualFile.nameWithoutExtension
        } else {
            null
        }
    }

    private fun resolvePreferredLanguage(targetClass: PsiElement?, targetFile: PsiFile?): TargetLanguage? {
        when (targetClass) {
            is PsiClass -> return TargetLanguage.JAVA
            is KtClass -> return TargetLanguage.KOTLIN
        }
        val extension = targetFile?.virtualFile?.extension?.lowercase(getDefault())
        return when (extension) {
            "java" -> TargetLanguage.JAVA
            "kt" -> TargetLanguage.KOTLIN
            else -> null
        }
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
        val targetFile: PsiFile?,
        val targetDirectory: PsiDirectory?,
        val suggestedClassName: String?,
        val preferredLanguage: TargetLanguage?,
        val targetPackageName: String?
    )

    private data class ParsedClassNameInput(
        val className: String,
        val preferredLanguage: TargetLanguage?
    )
}
