/*
 * Copyright (C) 2020 Jenly Yu, https://github.com/jenly1314/JavaFieldGenerator
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
package com.king.java.field.plugin.generator;


import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.king.java.field.plugin.entity.FieldParseConfig;

/**
 * @author <a href="mailto:yujinlin@mail.com">Jenly</a>
 */
public interface ICodeGenerator {


    /**
     * 根据解析的字段属性生成相关字段
     *
     * @param project          当前项目
     * @param psiClass         当前类
     * @param filedParseConfig 字段解析配置
     * @param text             需要解析的文本内容
     */
    void onGenerate(Project project, PsiClass psiClass, FieldParseConfig filedParseConfig, String text);
}
