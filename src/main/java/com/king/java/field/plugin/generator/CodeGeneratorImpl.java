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
package com.king.java.field.plugin.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.king.java.field.plugin.entity.FieldProperty;
import com.king.java.field.plugin.entity.FieldParseConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.http.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:yujinlin@mail.com">Jenly</a>
 */
public class CodeGeneratorImpl implements ICodeGenerator {

    @Override
    public void onGenerate(Project project, PsiClass psiClass, FieldParseConfig filedParseConfig, String text) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        // 解析文本内容得到字段属性集合
        List<FieldProperty> fieldProperties = parse(filedParseConfig, text);
        System.out.println("fieldProperties:" + fieldProperties);
        int size = fieldProperties.size();
        if(size == 0){
            return;
        }
        for (FieldProperty fieldProperty : fieldProperties) {
            StringBuilder sb = new StringBuilder();
            // 注释
            if (StringUtils.isNotBlank(fieldProperty.getComment())) {
                sb.append("/**\n\t* ").append(fieldProperty.getComment().replace("\n", "\n* ")).append("\n*/");
            }
            // 字段
            sb.append(String.format("\n%s %s %s;", fieldProperty.getModifier(), fieldProperty.getType(), fieldProperty.getName()));
            PsiField psiField = factory.createFieldFromText(sb.toString(), psiClass);
            // 添加到 PsiClass
            psiClass.add(psiField);
        }

        if (filedParseConfig.isGenerateGetterAndSetter() || filedParseConfig.isGenerateToString()) {
            int index = 0;
            StringBuilder toStringBuilder = new StringBuilder();
            if (filedParseConfig.isGenerateToString()) {
                toStringBuilder.append("@Override\n")
                        .append("public String toString(){\n")
                        .append("\t\treturn ")
                        .append("\"").append(psiClass.getName()).append("{\" + \n");
            }

            // 循环遍历
            for (FieldProperty fieldProperty : fieldProperties) {
                String fieldType = fieldProperty.getType();
                String fieldName = fieldProperty.getName();

                if (filedParseConfig.isGenerateGetterAndSetter()) {
                    String methodName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    //Generate getter
                    String getter;
                    if ("boolean".equalsIgnoreCase(fieldType)) {
                        getter = String.format("public %s is%s() {\n\t\treturn %s;\n\t}\n", fieldType, methodName, fieldName);
                    } else {
                        getter = String.format("public %s get%s() {\n\t\treturn %s;\n\t}\n", fieldType, methodName, fieldName);
                    }
                    PsiMethod getterPsiMethod = factory.createMethodFromText(getter, psiClass);
                    //Generate setter
                    String setter = String.format("public void set%s(%s %s) {\n\t\tthis.%s = %s;\n\t}\n", methodName, fieldType, fieldName, fieldName, fieldName);
                    PsiMethod setterPsiMethod = factory.createMethodFromText(setter, psiClass);

                    // 添加到 PsiClass
                    psiClass.add(getterPsiMethod);
                    psiClass.add(setterPsiMethod);
                }

                // toString
                if (filedParseConfig.isGenerateToString()) {
                    toStringBuilder.append("\t\t\t\t\"")
                            .append(fieldName)
                            .append("=\" + ")
                            .append(fieldName);
                    if (index < size - 1) {
                        toStringBuilder.append(" + \", \" + \n");
                    } else {
                        toStringBuilder.append(" + \n\"}\";\n}");
                    }
                }
                index++;

            }

            if (filedParseConfig.isGenerateToString()) {
                PsiMethod toStringPsiMethod = factory.createMethodFromText(toStringBuilder.toString(), psiClass);
                // 添加到 PsiClass
                psiClass.add(toStringPsiMethod);
            }
        }

    }

    /**
     * 根据配置解析对应的文本内容
     *
     * @param filedParseConfig 字段解析配置
     * @param text             需解析的文本内容
     * @return 字段属性集合
     */
    private List<FieldProperty> parse(FieldParseConfig filedParseConfig, String text) {
        List<FieldProperty> fieldPropertyList = new ArrayList<>();
        // 首先按行分割
        String[] lines = text.split("\n");
        FieldProperty fieldProperty = null;
        for (String singleLine : lines) {
            if (TextUtils.isEmpty(singleLine) && fieldProperty == null) {
                continue;
            }
            String[] stringArr = singleLine.split("\t");
            int length = stringArr.length;
            // 如果该行只有一个字符串，认为是上一行的注释有多行
            if (length == 1 && fieldProperty != null) {
                if (StringUtils.isNotBlank(stringArr[0])) {
                    fieldProperty.setComment(fieldProperty.getComment() + "\n" + stringArr[0].trim());
                }
            } else {
                // 如果连字段或字段类型都没有，则直接跳过
                if (filedParseConfig.getFieldColumn() >= length || filedParseConfig.getFieldTypeColumn() >= length) {
                    continue;
                }
                fieldProperty = new FieldProperty();
                fieldProperty.setModifier(filedParseConfig.getFieldModifier());
                fieldProperty.setName(stringArr[filedParseConfig.getFieldColumn()].trim());
                fieldProperty.setType(stringArr[filedParseConfig.getFieldTypeColumn()].trim());

                if (filedParseConfig.getFieldCommentColumn() < length) {
                    fieldProperty.setComment(stringArr[filedParseConfig.getFieldCommentColumn()].trim());
                }

                fieldPropertyList.add(fieldProperty);
            }
        }
        return fieldPropertyList;
    }
}
