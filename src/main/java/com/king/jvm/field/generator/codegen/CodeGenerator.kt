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
import com.intellij.psi.PsiElement
import com.king.jvm.field.generator.model.FieldParseConfig

/**
 * 字段代码生成接口
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
interface CodeGenerator {

    /**
     * 根据解析的字段属性生成相关字段
     */
    fun generate(project: Project, targetClass: PsiElement, fieldParseConfig: FieldParseConfig, text: String)
}
