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
package com.king.jvm.field.generator.codegen

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.king.jvm.field.generator.model.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory

import com.intellij.psi.PsiJavaFile

import com.intellij.psi.PsiFileFactory
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.Logger

/**
 * 字段代码生成实现
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
@OptIn(ExperimentalStdlibApi::class)
class CodeGeneratorImpl : CodeGenerator {
    private val logger = Logger.getInstance(CodeGeneratorImpl::class.java)

    override fun generate(project: Project, targetClass: PsiElement, fieldParseConfig: FieldParseConfig, text: String) {
        val fieldProperties = parse(fieldParseConfig, text)
        if (fieldProperties.isEmpty()) {
            return
        }

        when (targetClass) {
            is PsiClass -> generateForJava(project, targetClass, fieldParseConfig, fieldProperties)
            is KtClass -> generateForKotlin(targetClass, fieldParseConfig, fieldProperties)
            else -> error("Unsupported target class type: ${targetClass::class.java.name}")
        }
    }

    private fun generateForJava(
        project: Project,
        psiClass: PsiClass,
        fieldParseConfig: FieldParseConfig,
        fieldProperties: List<FieldProperty>
    ) {
        val factory = JavaPsiFacade.getElementFactory(project)
        val uniqueFieldProperties = fieldProperties.distinctBy { it.name }
        val existingFieldNames = psiClass.allFields.mapNotNull { it.name }.toSet()
        val overwriteExistingFields = fieldParseConfig.existingFieldPolicy == ExistingFieldPolicy.OVERWRITE_OLD
        val targetFieldProperties = if (overwriteExistingFields) {
            uniqueFieldProperties
        } else {
            uniqueFieldProperties.filterNot { it.name in existingFieldNames }
        }
        if (overwriteExistingFields) {
            val overwrittenFieldNames = uniqueFieldProperties
                .map { it.name }
                .filter { it in existingFieldNames }
                .toSet()
            removeJavaFieldsByName(psiClass, overwrittenFieldNames)
            removeJavaAccessorMethods(psiClass, uniqueFieldProperties)
        }
        val orderedNewFieldProperties = orderNewFieldProperties(targetFieldProperties, fieldParseConfig.fieldSortStyle)
        val orderedMethodFieldProperties = orderNewFieldProperties(uniqueFieldProperties, fieldParseConfig.fieldSortStyle)
        
        val importClass = getAnnotationImportClass(fieldParseConfig)
        if (importClass.isNotEmpty()) {
            addJavaImportIfMissing(project, psiClass, importClass)
        }
        applyJavaClassAnnotation(psiClass, fieldParseConfig, factory)

        orderedNewFieldProperties.forEach { fieldProperty ->
            val annotation = buildAnnotationText(fieldProperty, fieldParseConfig)
            addJavaField(
                psiClass = psiClass,
                field = factory.createFieldFromText(buildJavaFieldText(fieldProperty, annotation), psiClass),
                fieldName = fieldProperty.name,
                sortStyle = fieldParseConfig.fieldSortStyle
            )
        }
        reorderJavaFields(psiClass, fieldParseConfig.fieldSortStyle, targetFieldProperties.map { it.name }.toSet())

        val toStringMethod = findJavaMethod(psiClass, "toString", 0)
        if (fieldParseConfig.isGenerateGetterAndSetter) {
            orderedMethodFieldProperties.forEach { fieldProperty ->
                val methodName = fieldProperty.name.toMethodSuffix()
                val getterPrefix = if (fieldProperty.type.equals("boolean", ignoreCase = true)) "is" else "get"
                if (!hasJavaMethod(psiClass, getterPrefix + methodName, 0)) {
                    val getterText = buildString {
                        append("public ")
                        append(fieldProperty.type)
                        append(' ')
                        append(getterPrefix)
                        append(methodName)
                        append("() {\n\t\treturn ")
                        append(fieldProperty.name)
                        append(";\n\t}\n")
                    }
                    addJavaMethodBeforeAnchor(psiClass, factory.createMethodFromText(getterText, psiClass), toStringMethod)
                }
                if (!hasJavaMethod(psiClass, "set$methodName", 1)) {
                    val setterText = buildString {
                        append("public void set")
                        append(methodName)
                        append('(')
                        append(fieldProperty.type)
                        append(' ')
                        append(fieldProperty.name)
                        append(") {\n\t\tthis.")
                        append(fieldProperty.name)
                        append(" = ")
                        append(fieldProperty.name)
                        append(";\n\t}\n")
                    }
                    addJavaMethodBeforeAnchor(psiClass, factory.createMethodFromText(setterText, psiClass), toStringMethod)
                }
            }
        }

        if (fieldParseConfig.isGenerateToString) {
            val declaredFieldNames = psiClass.fields.mapNotNull { it.name }.distinct()
            val generatedToString = factory.createMethodFromText(
                buildJavaToStringMethod(psiClass.name ?: "Class", declaredFieldNames),
                psiClass
            )
            if (toStringMethod == null) {
                psiClass.add(generatedToString)
            } else {
                toStringMethod.replace(generatedToString)
            }
        }
    }

    private fun generateForKotlin(ktClass: KtClass, fieldParseConfig: FieldParseConfig, fieldProperties: List<FieldProperty>) {
        val factory = KtPsiFactory(ktClass.project, false)
        var workingClass = ktClass
        val uniqueFieldProperties = fieldProperties.distinctBy { it.name }
        val existingPropertyNames = collectKotlinPropertyNames(workingClass)
        val overwriteExistingFields = fieldParseConfig.existingFieldPolicy == ExistingFieldPolicy.OVERWRITE_OLD
        val newFieldProperties = if (overwriteExistingFields) {
            uniqueFieldProperties
        } else {
            uniqueFieldProperties.filterNot { it.name in existingPropertyNames }
        }
        if (overwriteExistingFields) {
            removeKotlinBodyProperties(workingClass, uniqueFieldProperties.map { it.name }.toSet())
        }
        val orderedNewFieldProperties = orderNewFieldProperties(newFieldProperties, fieldParseConfig.fieldSortStyle)

        addKotlinImports(workingClass, factory, fieldParseConfig)
        workingClass = applyKotlinClassAnnotation(workingClass, fieldParseConfig, factory)

        val propertyKeyword = fieldParseConfig.kotlinPropertyKeyword.value
        val useConstructorParametersMode = shouldUseConstructorParametersMode(workingClass, orderedNewFieldProperties)
        if (useConstructorParametersMode) {
            val convertToDataClass = shouldConvertToDataClass(workingClass, fieldParseConfig)
            replaceKotlinClassWithConstructorProperties(
                ktClass = workingClass,
                fieldParseConfig = fieldParseConfig,
                fieldProperties = orderedNewFieldProperties,
                convertToDataClass = convertToDataClass,
                factory = factory
            )
        } else {
            orderedNewFieldProperties.forEach { fieldProperty ->
                val annotation = buildAnnotationText(fieldProperty, fieldParseConfig)
                addKotlinProperty(
                    ktClass = workingClass,
                    property = factory.createProperty(
                        buildKotlinPropertyText(
                            fieldProperty = fieldProperty,
                            keyword = propertyKeyword,
                            annotation = annotation,
                            nullableDefaultNull = true
                        )
                    ),
                    propertyName = fieldProperty.name,
                    sortStyle = fieldParseConfig.fieldSortStyle
                )
            }
        }
    }

    private fun buildJavaFieldText(fieldProperty: FieldProperty, annotation: String): String {
        val declaration = joinNonBlank(fieldProperty.modifier, fieldProperty.type, fieldProperty.name) + ";"
        val comment = buildDocComment(fieldProperty.comment)
        val sb = StringBuilder()
        if (comment.isNotEmpty()) {
            sb.append(comment).append("\n")
        }
        if (annotation.isNotEmpty()) {
            sb.append(annotation).append("\n")
        }
        sb.append(declaration)
        return sb.toString()
    }

    private fun buildJavaToStringMethod(className: String, fieldNames: List<String>): String {
        val builder = StringBuilder()
        builder.append("@Override\n")
            .append("public String toString(){\n")
        if (fieldNames.isEmpty()) {
            builder.append("\t\treturn \"")
                .append(className)
                .append("{}\";\n}")
            return builder.toString()
        }
        builder.append("\t\treturn \"")
            .append(className)
            .append("{\" + \n")

        fieldNames.forEachIndexed { index, fieldName ->
            builder.append("\t\t\t\t\"")
                .append(fieldName)
                .append("=\" + ")
                .append(fieldName)
            if (index < fieldNames.size - 1) {
                builder.append(" + \", \" + \n")
            } else {
                builder.append(" + \n\"}\";\n}")
            }
        }
        return builder.toString()
    }

    private fun buildKotlinPropertyText(
        fieldProperty: FieldProperty,
        keyword: String,
        annotation: String,
        nullableDefaultNull: Boolean
    ): String {
        val finalType = getKotlinPropertyType(fieldProperty)
        
        val declaration = buildString {
            if (annotation.isNotEmpty()) {
                append(annotation).append("\n")
            }
            append(joinNonBlank(keyword, fieldProperty.name))
            append(": ")
            append(finalType)
            if (!fieldProperty.isNotNull && nullableDefaultNull) {
                append(" = null")
            }
        }
        val comment = buildDocComment(fieldProperty.comment)
        return if (comment.isEmpty()) declaration else "$comment\n$declaration"
    }

    private fun buildKotlinConstructorParameterText(
        fieldProperty: FieldProperty,
        keyword: String,
        annotation: String,
        nullableDefaultNull: Boolean
    ): String {
        val finalType = getKotlinPropertyType(fieldProperty)
        val declaration = buildString {
            if (annotation.isNotEmpty()) {
                append(annotation).append("\n    ")
            }
            append(joinNonBlank(keyword, fieldProperty.name))
            append(": ")
            append(finalType)
            if (!fieldProperty.isNotNull && nullableDefaultNull) {
                append(" = null")
            }
        }
        val comment = buildDocComment(fieldProperty.comment, "")
        return if (comment.isEmpty()) declaration else "$comment\n    $declaration"
    }

    private fun getKotlinPropertyType(fieldProperty: FieldProperty): String {
        val type = toKotlinType(fieldProperty.type)
        return if (fieldProperty.isNotNull) type else "$type?"
    }


    private fun toKotlinType(type: String): String {
        val normalized = type.trim()
        return when {
            normalized.endsWith("[][]") -> "List<List<${toKotlinType(normalized.removeSuffix("[][]"))}>>"
            normalized.endsWith("[]") -> "List<${toKotlinType(normalized.removeSuffix("[]"))}>"
            else -> when (normalized) {
                "byte", "Byte" -> "Byte"
                "short", "Short" -> "Short"
                "int", "Int", "Integer" -> "Int"
                "long", "Long" -> "Long"
                "float", "Float" -> "Float"
                "double", "Double" -> "Double"
                "char", "Char", "Character" -> "Char"
                "boolean", "Boolean" -> "Boolean"
                "object", "Object", "java.lang.Object" -> "Any"
                "void" -> "Unit"
                else -> normalized
            }
        }
    }

    private fun buildDocComment(comment: String, indent: String = ""): String {
        if (comment.isBlank()) {
            return ""
        }
        val normalizedComment = comment
            .split(Regex("\\r?\\n"))
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() }
            .joinToString(" ")
        return if (normalizedComment.isEmpty()) "" else "$indent/** $normalizedComment */"
    }

    private fun joinNonBlank(vararg parts: String?): String {
        return parts.filterNot { it.isNullOrBlank() }.joinToString(" ")
    }

    private fun parse(fieldParseConfig: FieldParseConfig, text: String): List<FieldProperty> {
        val fieldPropertyList = mutableListOf<FieldProperty>()
        var currentFieldProperty: FieldProperty? = null
        val fieldTypeConvertMap: Map<String, String?> = fieldParseConfig.fieldTypeConvertMap ?: emptyMap()

        text.split(Regex("\\r?\\n")).forEach { line ->
            if (line.isBlank() && currentFieldProperty == null) {
                return@forEach
            }

            val stringArr = line.split("\t")
            if (stringArr.size == 1) {
                if (fieldParseConfig.fieldCommentColumn >= 0 && currentFieldProperty != null && stringArr[0].isNotBlank()) {
                    currentFieldProperty?.appendCommentLine(stringArr[0])
                }
                return@forEach
            }

            if (fieldParseConfig.fieldColumn >= stringArr.size) {
                return@forEach
            }

            val comment = if (fieldParseConfig.fieldCommentColumn >= 0 && fieldParseConfig.fieldCommentColumn < stringArr.size) {
                stringArr[fieldParseConfig.fieldCommentColumn].trim()
            } else {
                ""
            }

            val isNotNull = when (fieldParseConfig.nullabilityMode) {
                NullabilityMode.NOT_NULL -> true
                NullabilityMode.NULLABLE -> false
                else -> { // AUTO
                    if (fieldParseConfig.fieldNotNullColumn >= 0 && fieldParseConfig.fieldNotNullColumn < stringArr.size) {
                        val value = stringArr[fieldParseConfig.fieldNotNullColumn].trim()
                        val keywords = fieldParseConfig.notNullKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        keywords.any { it.equals(value, ignoreCase = true) }
                    } else {
                        false
                    }
                }
            }

            val rawFieldType = if (fieldParseConfig.fieldTypeColumn in stringArr.indices) {
                stringArr[fieldParseConfig.fieldTypeColumn].trim()
            } else {
                ""
            }
            val sourceFieldName = stringArr[fieldParseConfig.fieldColumn].trim()
            val transformedFieldName = transformFieldName(sourceFieldName, fieldParseConfig.fieldNameStyle)
            if (sourceFieldName.isBlank() || transformedFieldName.isBlank()) {
                currentFieldProperty = null
                return@forEach
            }
            currentFieldProperty = FieldProperty(
                modifier = fieldParseConfig.fieldModifier.value,
                type = convertFieldType(
                    fieldType = rawFieldType.ifBlank { FieldParseConfig.DEFAULT_FIELD_TYPE },
                    fieldTypeConvertMap = fieldTypeConvertMap,
                    convertArrayToList = fieldParseConfig.isConvertArrayToList
                ),
                name = transformedFieldName,
                sourceName = sourceFieldName,
                comment = comment,
                isNotNull = isNotNull
            )
            fieldPropertyList.add(currentFieldProperty!!)
        }

        return fieldPropertyList
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun String.toMethodSuffix(): String {
        return replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }
    }

    private fun convertFieldType(
        fieldType: String,
        fieldTypeConvertMap: Map<String, String?>,
        convertArrayToList: Boolean
    ): String {
        if (fieldType.endsWith("[][]")) {
            val type = fieldType.removeSuffix("[][]")
            val converted = fieldTypeConvertMap[type] ?: type
            return if (convertArrayToList) {
                "List<List<${convertWrapperType(converted)}>>"
            } else {
                "$converted[][]"
            }
        }

        if (fieldType.endsWith("[]")) {
            val type = fieldType.removeSuffix("[]")
            val converted = fieldTypeConvertMap[type] ?: type
            return if (convertArrayToList) {
                "List<${convertWrapperType(converted)}>"
            } else {
                "$converted[]"
            }
        }

        return fieldTypeConvertMap[fieldType] ?: fieldType
    }

    private fun convertWrapperType(type: String): String {
        return when (type) {
            "byte" -> "Byte"
            "short" -> "Short"
            "int" -> "Integer"
            "float" -> "Float"
            "long" -> "Long"
            "double" -> "Double"
            "char" -> "Character"
            "boolean" -> "Boolean"
            else -> type
        }
    }

    private fun getAnnotationImportClass(fieldParseConfig: FieldParseConfig): String {
        val importClass = when (fieldParseConfig.annotationType) {
            AnnotationType.GSON -> "com.google.gson.annotations.SerializedName"
            AnnotationType.MOSHI -> "com.squareup.moshi.Json"
            AnnotationType.JACKSON -> "com.fasterxml.jackson.annotation.JsonProperty"
            AnnotationType.FASTJSON -> "com.alibaba.fastjson.annotation.JSONField"
            AnnotationType.KOTLIN_SERIALIZATION -> "kotlinx.serialization.SerialName"
            AnnotationType.CUSTOM -> fieldParseConfig.customAnnotationImport
            else -> ""
        }
        return normalizeImportClass(importClass)
    }

    private fun getClassAnnotationImportClass(fieldParseConfig: FieldParseConfig): String {
        val importClass = when (fieldParseConfig.annotationType) {
            AnnotationType.KOTLIN_SERIALIZATION -> "kotlinx.serialization.Serializable"
            else -> ""
        }
        return normalizeImportClass(importClass)
    }

    private fun buildAnnotationText(fieldProperty: FieldProperty, fieldParseConfig: FieldParseConfig): String {
        val annotationType = fieldParseConfig.annotationType
        if (annotationType == AnnotationType.NONE) {
            return ""
        }
        val name = fieldProperty.sourceName
        return when (annotationType) {
            AnnotationType.GSON -> "@SerializedName(\"$name\")"
            AnnotationType.MOSHI -> "@Json(name = \"$name\")"
            AnnotationType.JACKSON -> "@JsonProperty(\"$name\")"
            AnnotationType.FASTJSON -> "@JSONField(name = \"$name\")"
            AnnotationType.KOTLIN_SERIALIZATION -> "@SerialName(\"$name\")"
            AnnotationType.CUSTOM -> {
                val format = fieldParseConfig.customPropertyAnnotation
                if (format.isNotBlank()) {
                    formatCustomAnnotation(format, name)
                } else {
                    ""
                }
            }
            else -> ""
        }
    }

    private fun buildClassAnnotationText(className: String, fieldParseConfig: FieldParseConfig): String {
        when (fieldParseConfig.annotationType) {
            AnnotationType.KOTLIN_SERIALIZATION -> return "@Serializable"
            AnnotationType.CUSTOM -> {
                val format = fieldParseConfig.customClassAnnotation.trim()
                if (format.isBlank()) {
                    return ""
                }
                return format
                    .replace("%s", className)
                    .replace("\${name}", className)
            }
            else -> return ""
        }
    }

    private fun formatCustomAnnotation(format: String, sourceName: String): String {
        return format
            .replace("%s", sourceName)
            .replace("\${name}", sourceName)
    }

    private fun normalizeImportClass(importClass: String): String {
        return importClass
            .lineSequence()
            .map { line -> line.trim() }
            .firstOrNull { line -> line.isNotEmpty() }
            .orEmpty()
            .removePrefix("import")
            .trim()
            .trimEnd(';')
    }

    private fun applyJavaClassAnnotation(
        psiClass: PsiClass,
        fieldParseConfig: FieldParseConfig,
        factory: com.intellij.psi.PsiElementFactory
    ) {
        val annotationText = buildClassAnnotationText(psiClass.name.orEmpty(), fieldParseConfig)
        if (annotationText.isBlank()) {
            return
        }
        val modifierList = psiClass.modifierList ?: return
        if (modifierList.annotations.any { annotation -> annotation.text == annotationText }) {
            return
        }
        modifierList.addBefore(factory.createAnnotationFromText(annotationText, psiClass), modifierList.firstChild)
    }

    private fun applyKotlinClassAnnotation(
        ktClass: KtClass,
        fieldParseConfig: FieldParseConfig,
        factory: KtPsiFactory
    ): KtClass {
        val annotationText = buildClassAnnotationText(ktClass.name.orEmpty(), fieldParseConfig)
        if (annotationText.isBlank() || ktClass.annotationEntries.any { entry -> entry.text == annotationText }) {
            return ktClass
        }
        val replacement = factory.createFile("Generated.kt", "$annotationText\n${ktClass.text}")
            .declarations
            .filterIsInstance<KtClass>()
            .first()
        return ktClass.replace(replacement) as KtClass
    }

    private fun addKotlinImports(ktClass: KtClass, factory: KtPsiFactory, fieldParseConfig: FieldParseConfig) {
        val importList = ktClass.containingKtFile.importList ?: return
        val existingImports = importList.imports
            .mapNotNull { importDirective -> importDirective.importPath?.pathStr }
            .toSet()
        listOf(
            getAnnotationImportClass(fieldParseConfig),
            getClassAnnotationImportClass(fieldParseConfig)
        )
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { importClass ->
                if (importClass in existingImports) {
                    return@forEach
                }
                val dummyFile = factory.createFile("Dummy.kt", "import $importClass")
                val importDirective = dummyFile.importList?.imports?.firstOrNull()
                if (importDirective != null) {
                    importList.add(importDirective)
                } else {
                    logger.warn("Skip invalid Kotlin import: $importClass")
                }
            }
    }

    private fun addJavaImportIfMissing(project: Project, psiClass: PsiClass, importClass: String) {
        val file = psiClass.containingFile as? PsiJavaFile ?: return
        val importList = file.importList ?: return
        if (importList.allImportStatements.any { importStatement ->
                importStatement.importReference?.qualifiedName == importClass
            }) {
            return
        }
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText("Dummy.java", JavaFileType.INSTANCE, "import $importClass; class Dummy {}") as PsiJavaFile
        val importStmt = dummyFile.importList?.importStatements?.firstOrNull()
        if (importStmt != null) {
            importList.add(importStmt)
        } else {
            logger.warn("Skip invalid Java import: $importClass")
        }
    }

    private fun replaceKotlinClassWithConstructorProperties(
        ktClass: KtClass,
        fieldParseConfig: FieldParseConfig,
        fieldProperties: List<FieldProperty>,
        convertToDataClass: Boolean,
        factory: KtPsiFactory
    ): KtClass {
        val parametersText = buildKotlinConstructorParametersText(ktClass, fieldParseConfig, fieldProperties)
        val originalText = ktClass.text
        val bodyText = resolveKotlinClassBodyText(ktClass)
        val headerText = if (ktClass.body != null) {
            originalText.substring(0, ktClass.body!!.startOffsetInParent).trimEnd()
        } else {
            originalText.trimEnd()
        }

        val updatedHeader = replaceOrInsertPrimaryConstructor(
            if (convertToDataClass) ensureDataClassHeader(headerText, ktClass.name.orEmpty()) else headerText,
            ktClass.name.orEmpty(),
            parametersText
        )
        val updatedClassText = joinNonBlank(updatedHeader, bodyText)
        val replacement = factory.createFile("Generated.kt", updatedClassText)
            .declarations
            .filterIsInstance<KtClass>()
            .first()
        return ktClass.replace(replacement) as KtClass
    }

    private fun resolveKotlinClassBodyText(ktClass: KtClass): String {
        val body = ktClass.body ?: return ""
        val bodyContent = body.text
            .removePrefix("{")
            .removeSuffix("}")
            .trim()
        return if (bodyContent.isEmpty()) "" else body.text
    }

    private fun canUseDataClassMode(ktClass: KtClass): Boolean {
        val modifiers = ktClass.modifierList?.text.orEmpty()
        if (Regex("""\b(open|abstract|sealed|inner)\b""").containsMatchIn(modifiers)) {
            return false
        }
        return ktClass.primaryConstructorParameters.all { parameter ->
            Regex("""\b(val|var)\b""").containsMatchIn(parameter.text.substringBefore(':'))
        }
    }

    private fun shouldUseConstructorParametersMode(
        ktClass: KtClass,
        newFieldProperties: List<FieldProperty>
    ): Boolean {
        return ktClass.primaryConstructorParameters.any { parameter ->
            Regex("""\b(val|var)\b""").containsMatchIn(parameter.text.substringBefore(':'))
        } || newFieldProperties.isNotEmpty()
    }

    private fun shouldConvertToDataClass(ktClass: KtClass, fieldParseConfig: FieldParseConfig): Boolean {
        val isDataClass = Regex("""\bdata\b""").containsMatchIn(ktClass.modifierList?.text.orEmpty())
        if (isDataClass) {
            return true
        }
        return fieldParseConfig.isUseDataClass && canUseDataClassMode(ktClass)
    }

    private fun collectKotlinPropertyNames(ktClass: KtClass): Set<String> {
        val constructorPropertyNames = ktClass.primaryConstructorParameters
            .filter { parameter -> Regex("""\b(val|var)\b""").containsMatchIn(parameter.text.substringBefore(':')) }
            .mapNotNull { parameter -> parameter.name }
        val bodyPropertyNames = ktClass.declarations
            .filterIsInstance<KtProperty>()
            .mapNotNull { property -> property.name }
        return (constructorPropertyNames + bodyPropertyNames).toSet()
    }

    private fun hasJavaMethod(psiClass: PsiClass, methodName: String, parameterCount: Int): Boolean {
        return psiClass.findMethodsByName(methodName, false).any { method ->
            method.parameterList.parametersCount == parameterCount
        }
    }

    private fun findJavaMethod(psiClass: PsiClass, methodName: String, parameterCount: Int): PsiMethod? {
        return psiClass.findMethodsByName(methodName, false).firstOrNull { method ->
            method.parameterList.parametersCount == parameterCount
        }
    }

    private fun addJavaMethodBeforeAnchor(psiClass: PsiClass, method: PsiMethod, anchorMethod: PsiMethod?) {
        if (anchorMethod == null) {
            psiClass.add(method)
        } else {
            psiClass.addBefore(method, anchorMethod)
        }
    }

    private fun removeJavaFieldsByName(psiClass: PsiClass, fieldNames: Set<String>) {
        if (fieldNames.isEmpty()) {
            return
        }
        psiClass.fields
            .filter { field -> field.name in fieldNames }
            .toList()
            .forEach { field -> field.delete() }
    }

    private fun removeJavaAccessorMethods(psiClass: PsiClass, fieldProperties: List<FieldProperty>) {
        fieldProperties.forEach { fieldProperty ->
            val methodSuffix = fieldProperty.name.toMethodSuffix()
            removeJavaMethodsBySignature(psiClass, "get$methodSuffix", 0)
            removeJavaMethodsBySignature(psiClass, "is$methodSuffix", 0)
            removeJavaMethodsBySignature(psiClass, "set$methodSuffix", 1)
        }
    }

    private fun removeJavaMethodsBySignature(psiClass: PsiClass, methodName: String, parameterCount: Int) {
        psiClass.findMethodsByName(methodName, false)
            .filter { method -> method.parameterList.parametersCount == parameterCount }
            .toList()
            .forEach { method -> method.delete() }
    }

    private fun addJavaField(psiClass: PsiClass, field: PsiField, fieldName: String, sortStyle: FieldSortStyle) {
        when (sortStyle) {
            FieldSortStyle.FIELD_NAME_GLOBAL -> {
                val anchor = findJavaFieldSortAnchor(psiClass, fieldName)
                when {
                    anchor != null -> psiClass.addBefore(field, anchor)
                    psiClass.fields.isNotEmpty() -> psiClass.addAfter(field, psiClass.fields.last())
                    else -> findFirstJavaMethodAnchor(psiClass)?.let { psiClass.addBefore(field, it) } ?: psiClass.add(field)
                }
            }
            else -> {
                if (psiClass.fields.isNotEmpty()) {
                    psiClass.addAfter(field, psiClass.fields.last())
                } else {
                    findFirstJavaMethodAnchor(psiClass)?.let { psiClass.addBefore(field, it) } ?: psiClass.add(field)
                }
            }
        }
    }

    private fun findJavaFieldSortAnchor(psiClass: PsiClass, fieldName: String): PsiField? {
        return psiClass.fields.firstOrNull { existingField ->
            compareFieldNames(existingField.name, fieldName) > 0
        }
    }

    private fun findFirstJavaMethodAnchor(psiClass: PsiClass): PsiMethod? {
        return psiClass.methods.firstOrNull()
    }

    private fun reorderJavaFields(psiClass: PsiClass, sortStyle: FieldSortStyle, newFieldNames: Set<String>) {
        if (sortStyle == FieldSortStyle.DEFAULT || newFieldNames.isEmpty()) {
            return
        }
        val currentFields = psiClass.fields.toList()
        if (currentFields.size < 2) {
            return
        }

        val reorderedFields = when (sortStyle) {
            FieldSortStyle.FIELD_NAME_LOCAL -> {
                val existingFields = currentFields.filterNot { field -> field.name in newFieldNames }
                val newFields = currentFields
                    .filter { field -> field.name in newFieldNames }
                    .sortedWith(javaFieldComparator())
                existingFields + newFields
            }
            FieldSortStyle.FIELD_NAME_GLOBAL -> currentFields.sortedWith(javaFieldComparator())
            else -> currentFields
        }

        if (currentFields.map { it.name } == reorderedFields.map { it.name }) {
            return
        }

        val fieldCopies = reorderedFields.map { field -> field.copy() as PsiField }
        currentFields.forEach { field -> field.delete() }
        val anchor = findFirstJavaMethodAnchor(psiClass) ?: psiClass.rBrace
        fieldCopies.forEach { fieldCopy ->
            if (anchor != null) {
                psiClass.addBefore(fieldCopy, anchor)
            } else {
                psiClass.add(fieldCopy)
            }
        }
    }

    private fun javaFieldComparator(): Comparator<PsiField> {
        return Comparator { left, right -> compareFieldNames(left.name, right.name) }
    }

    private fun addKotlinProperty(ktClass: KtClass, property: KtProperty, propertyName: String, sortStyle: FieldSortStyle) {
        if (sortStyle == FieldSortStyle.FIELD_NAME_GLOBAL) {
            val anchor = ktClass.declarations
                .filterIsInstance<KtProperty>()
                .firstOrNull { existingProperty -> compareFieldNames(existingProperty.name, propertyName) > 0 }
            if (anchor != null) {
                ktClass.addBefore(property, anchor)
                return
            }
        }
        ktClass.addDeclaration(property)
    }

    private fun removeKotlinBodyProperties(ktClass: KtClass, propertyNames: Set<String>) {
        if (propertyNames.isEmpty()) {
            return
        }
        ktClass.declarations
            .filterIsInstance<KtProperty>()
            .filter { property -> property.name in propertyNames }
            .toList()
            .forEach { property -> property.delete() }
    }

    private fun buildKotlinConstructorParametersText(
        ktClass: KtClass,
        fieldParseConfig: FieldParseConfig,
        newFieldProperties: List<FieldProperty>
    ): String {
        val existingParameters = ktClass.primaryConstructorParameters
            .filter { parameter -> Regex("""\b(val|var)\b""").containsMatchIn(parameter.text.substringBefore(':')) }
            .filterNot { parameter ->
                fieldParseConfig.existingFieldPolicy == ExistingFieldPolicy.OVERWRITE_OLD
                        && newFieldProperties.any { fieldProperty -> fieldProperty.name == parameter.name }
            }
            .mapNotNull { parameter ->
                parameter.name?.let { name -> ConstructorParameterSpec(name, parameter.text.trim()) }
            }
        val newParameters = newFieldProperties.map { fieldProperty ->
            ConstructorParameterSpec(
                name = fieldProperty.name,
                text = buildKotlinConstructorParameterText(
                    fieldProperty = fieldProperty,
                    keyword = fieldParseConfig.kotlinPropertyKeyword.value,
                    annotation = buildAnnotationText(fieldProperty, fieldParseConfig),
                    nullableDefaultNull = false
                )
            )
        }
        val finalParameters = when (fieldParseConfig.fieldSortStyle) {
            FieldSortStyle.FIELD_NAME_GLOBAL -> (existingParameters + newParameters).sortedWith(constructorParameterComparator())
            FieldSortStyle.FIELD_NAME_LOCAL -> existingParameters + newParameters.sortedWith(constructorParameterComparator())
            FieldSortStyle.DEFAULT -> existingParameters + newParameters
        }
        return finalParameters.joinToString(",\n    ") { parameter -> parameter.text }
    }

    private fun constructorParameterComparator(): Comparator<ConstructorParameterSpec> {
        return compareBy(String.CASE_INSENSITIVE_ORDER, ConstructorParameterSpec::name).thenBy(ConstructorParameterSpec::name)
    }

    private fun orderNewFieldProperties(fieldProperties: List<FieldProperty>, style: FieldSortStyle): List<FieldProperty> {
        return when (style) {
            FieldSortStyle.FIELD_NAME_LOCAL,
            FieldSortStyle.FIELD_NAME_GLOBAL -> fieldProperties.sortedWith(fieldPropertyComparator())
            FieldSortStyle.DEFAULT -> fieldProperties
        }
    }

    private fun fieldPropertyComparator(): Comparator<FieldProperty> {
        return compareBy(String.CASE_INSENSITIVE_ORDER, FieldProperty::name).thenBy(FieldProperty::name)
    }

    private fun compareFieldNames(left: String?, right: String?): Int {
        val leftValue = left.orEmpty()
        val rightValue = right.orEmpty()
        val ignoreCaseResult = String.CASE_INSENSITIVE_ORDER.compare(leftValue, rightValue)
        return if (ignoreCaseResult != 0) ignoreCaseResult else leftValue.compareTo(rightValue)
    }

    private data class ConstructorParameterSpec(
        val name: String,
        val text: String
    )

    private fun ensureDataClassHeader(headerText: String, className: String): String {
        if (className.isBlank() || Regex("""\bdata\s+class\b""").containsMatchIn(headerText)) {
            return headerText
        }
        return headerText.replaceFirst(
            Regex("""\bclass\s+${Regex.escape(className)}\b"""),
            "data class $className"
        )
    }

    private fun replaceOrInsertPrimaryConstructor(headerText: String, className: String, newParametersText: String): String {
        if (className.isBlank()) {
            return headerText
        }
        val classNameIndex = headerText.indexOf(className)
        if (classNameIndex < 0) {
            return headerText
        }
        var cursor = classNameIndex + className.length
        while (cursor < headerText.length && headerText[cursor].isWhitespace()) {
            cursor++
        }
        if (cursor < headerText.length && headerText[cursor] == '<') {
            cursor = findMatchingBracket(headerText, cursor, '<', '>') + 1
        }
        while (cursor < headerText.length && headerText[cursor].isWhitespace()) {
            cursor++
        }

        val boundary = findHeaderBoundary(headerText, cursor)
        val openParenIndex = headerText.indexOf('(', cursor).takeIf { it in cursor until boundary }
        return if (openParenIndex != null) {
            val closeParenIndex = findMatchingBracket(headerText, openParenIndex, '(', ')')
            buildString {
                append(headerText, 0, openParenIndex + 1)
                append(formatConstructorParametersBody(newParametersText))
                append(headerText.substring(closeParenIndex))
            }
        } else {
            buildString {
                append(headerText, 0, boundary)
                append(formatConstructorParameters(newParametersText))
                append(headerText.substring(boundary))
            }
        }
    }

    private fun formatConstructorParameters(parametersText: String): String {
        return "(${formatConstructorParametersBody(parametersText)})"
    }

    private fun formatConstructorParametersBody(parametersText: String): String {
        return if (parametersText.isBlank()) "" else "\n    $parametersText\n"
    }

    private fun findHeaderBoundary(headerText: String, startIndex: Int): Int {
        val candidates = listOf(
            headerText.indexOf(':', startIndex),
            headerText.indexOf('{', startIndex),
            headerText.indexOf(" where ", startIndex)
        ).filter { it >= 0 }
        return candidates.minOrNull() ?: headerText.length
    }

    private fun findMatchingBracket(text: String, startIndex: Int, open: Char, close: Char): Int {
        var depth = 0
        for (index in startIndex until text.length) {
            when (text[index]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) {
                        return index
                    }
                }
            }
        }
        return text.length - 1
    }

    private fun transformFieldName(name: String, style: FieldNameStyle): String {
        return when (style) {
            FieldNameStyle.CAMEL_CASE -> toCamelCase(name)
            FieldNameStyle.SNAKE_CASE -> toSnakeCase(name)
            FieldNameStyle.NONE -> name
        }
    }

    private fun toCamelCase(name: String): String {
        val parts = splitFieldName(name)
        if (parts.isEmpty()) {
            return name
        }
        return buildString {
            append(parts.first().lowercase())
            parts.drop(1).forEach { part ->
                append(part.lowercase().replaceFirstChar { first -> first.titlecase() })
            }
        }
    }

    private fun toSnakeCase(name: String): String {
        val parts = splitFieldName(name)
        return if (parts.isEmpty()) name else parts.joinToString("_") { it.lowercase() }
    }

    private fun splitFieldName(name: String): List<String> {
        return name
            .trim()
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .split(Regex("[^A-Za-z0-9]+|_+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
