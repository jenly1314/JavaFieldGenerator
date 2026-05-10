/*
 * Copyright (C) 2020 Jenly Yu
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
package com.king.jvm.field.generator.model

import java.util.LinkedHashMap

/**
 * 字段解析配置模型
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
class FieldParseConfig {

    var fieldColumn: Int = DEFAULT_FIELD_COLUMN
    var fieldTypeColumn: Int = DEFAULT_FIELD_TYPE_COLUMN
    var fieldCommentColumn: Int = DEFAULT_FIELD_COMMENT_COLUMN
    var fieldNotNullColumn: Int = DEFAULT_FIELD_NOTNULL_COLUMN
    var valueSeparator: ValueSeparator = ValueSeparator.TAB
    var fieldNameStyle: FieldNameStyle = FieldNameStyle.NONE
    var fieldSortStyle: FieldSortStyle = FieldSortStyle.DEFAULT
    var existingFieldPolicy: ExistingFieldPolicy = ExistingFieldPolicy.IGNORE_NEW
    var notNullKeywords: String = DEFAULT_NOT_NULL_KEYWORDS
    var nullabilityMode: NullabilityMode = NullabilityMode.AUTO
    var fieldModifier: Modifier = Modifier.PRIVATE
    var targetLanguage: TargetLanguage = TargetLanguage.AUTO
    var isLastGeneratedKotlin: Boolean? = null
    var isEnableInputRegexValidation: Boolean = true
    var isGenerateGetterAndSetter: Boolean = true
    var isGenerateToString: Boolean = true
    var isConvertArrayToList: Boolean = true
    var isUseDataClass: Boolean = true
    var kotlinPropertyKeyword: KotlinPropertyKeyword = KotlinPropertyKeyword.VAL

    var annotationType: AnnotationType = AnnotationType.NONE
    var customAnnotationImport: String = ""
    var customClassAnnotation: String = ""
    var customPropertyAnnotation: String = ""

    var fieldTypeConvertMap: MutableMap<String, String?>? = null
        get() {
            if (field == null) {
                field = createFieldTypeConvertMap()
            }
            return field
        }

    private fun createFieldTypeConvertMap(): MutableMap<String, String?> {
        val convertMap = LinkedHashMap<String, String?>(48)
        convertMap["char"] = "String"
        convertMap["nchar"] = "String"
        convertMap["varchar"] = "String"
        convertMap["varchar2"] = "String"
        convertMap["nvarchar"] = "String"
        convertMap["tinytext"] = "String"
        convertMap["text"] = "String"
        convertMap["mediumtext"] = "String"
        convertMap["longtext"] = "String"
        convertMap["ntext"] = "String"
        convertMap["json"] = "String"
        convertMap["jsonb"] = "String"
        convertMap["uuid"] = "String"
        convertMap["string"] = "String"
        convertMap["bool"] = "boolean"
        convertMap["boolean"] = "boolean"
        convertMap["bit"] = "boolean"
        convertMap["tinyint"] = "Byte"
        convertMap["smallint"] = "Short"
        convertMap["int"] = "Integer"
        convertMap["integer"] = "Integer"
        convertMap["int4"] = "Integer"
        convertMap["bigint"] = "Long"
        convertMap["int8"] = "Long"
        convertMap["float"] = "Float"
        convertMap["float4"] = "Float"
        convertMap["double"] = "Double"
        convertMap["float8"] = "Double"
        convertMap["decimal"] = "Double"
        convertMap["numeric"] = "Double"
        convertMap["number"] = "Double"
        convertMap["real"] = "Double"
        convertMap["date"] = "String"
        convertMap["datetime"] = "String"
        convertMap["timestamp"] = "String"
        convertMap["time"] = "String"
        convertMap["year"] = "Integer"
        return convertMap
    }

    fun ensureDefaults(): FieldParseConfig {
        valueSeparator = valueOrDefault(valueSeparator, ValueSeparator.TAB)
        fieldNameStyle = valueOrDefault(fieldNameStyle, FieldNameStyle.NONE)
        fieldSortStyle = valueOrDefault(fieldSortStyle, FieldSortStyle.DEFAULT)
        existingFieldPolicy = valueOrDefault(existingFieldPolicy, ExistingFieldPolicy.IGNORE_NEW)
        notNullKeywords = valueOrDefault(notNullKeywords, DEFAULT_NOT_NULL_KEYWORDS)
        nullabilityMode = valueOrDefault(nullabilityMode, NullabilityMode.AUTO)
        fieldModifier = valueOrDefault(fieldModifier, Modifier.PRIVATE)
        targetLanguage = valueOrDefault(targetLanguage, TargetLanguage.AUTO)
        kotlinPropertyKeyword = valueOrDefault(kotlinPropertyKeyword, KotlinPropertyKeyword.VAL)
        annotationType = valueOrDefault(annotationType, AnnotationType.NONE)
        customAnnotationImport = valueOrDefault(customAnnotationImport, "")
        customClassAnnotation = valueOrDefault(customClassAnnotation, "")
        customPropertyAnnotation = valueOrDefault(customPropertyAnnotation, "")
        if (fieldTypeConvertMap == null) {
            fieldTypeConvertMap = createFieldTypeConvertMap()
        }
        return this
    }

    private fun <T> valueOrDefault(value: T?, defaultValue: T): T {
        return value ?: defaultValue
    }

    companion object {
        const val DEFAULT_FIELD_COLUMN: Int = 0
        const val DEFAULT_FIELD_TYPE_COLUMN: Int = 1
        const val DEFAULT_FIELD_COMMENT_COLUMN: Int = -1
        const val DEFAULT_FIELD_NOTNULL_COLUMN: Int = 2
        const val DEFAULT_FIELD_TYPE: String = "Object"
        const val DEFAULT_NOT_NULL_KEYWORDS: String = "Y,是,必须"
    }
}
