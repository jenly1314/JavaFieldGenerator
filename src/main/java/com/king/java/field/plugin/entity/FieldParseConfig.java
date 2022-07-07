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
package com.king.java.field.plugin.entity;

import com.king.java.field.plugin.dialog.GenerateFieldDialog;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段解析配置
 *
 * @author <a href="mailto:yujinlin@mail.com">Jenly</a>
 */
public class FieldParseConfig {

    /**
     * 字段所在列；默认：第0列
     */
    private int fieldColumn = 0;
    /**
     * 字段类型所在列；默认：第1列
     */
    private int fieldTypeColumn = 1;
    /**
     * 字段注释说明所在列；默认：第2列
     */
    private int fieldCommentColumn = 2;
    /**
     * 字段修饰符；默认：private
     */
    private String fieldModifier = GenerateFieldDialog.Modifier.PRIVATE.getValue();
    /**
     * 是否生成 get 和 set 方法
     */
    private boolean generateGetterAndSetter;
    /**
     * 是否生成 toString 方法
     */
    private boolean generateToString;
    /**
     * 字段类型转换配置；key为原始类型，value表示需要目标类型；如：key = int，value = Integer 时，则会在生成字段类型时，自动将 int 转换为 Integer
     */
    private Map<String,String> filedTypeConvertMap;
    /**
     * 是否将数组转换为列表
     */
    private boolean convertArrayToList = true;

    public int getFieldColumn() {
        return fieldColumn;
    }

    public void setFieldColumn(int fieldColumn) {
        this.fieldColumn = fieldColumn;
    }

    public int getFieldTypeColumn() {
        return fieldTypeColumn;
    }

    public void setFieldTypeColumn(int fieldTypeColumn) {
        this.fieldTypeColumn = fieldTypeColumn;
    }

    public int getFieldCommentColumn() {
        return fieldCommentColumn;
    }

    public void setFieldCommentColumn(int fieldCommentColumn) {
        this.fieldCommentColumn = fieldCommentColumn;
    }

    public String getFieldModifier() {
        return fieldModifier;
    }

    public void setFieldModifier(String fieldModifier) {
        this.fieldModifier = fieldModifier;
    }

    public boolean isGenerateToString() {
        return generateToString;
    }

    public void setGenerateToString(boolean generateToString) {
        this.generateToString = generateToString;
    }

    public boolean isGenerateGetterAndSetter() {
        return generateGetterAndSetter;
    }

    public void setGenerateGetterAndSetter(boolean generateGetterAndSetter) {
        this.generateGetterAndSetter = generateGetterAndSetter;
    }

    @NotNull
    public Map<String, String> getFiledTypeConvertMap() {
        if (filedTypeConvertMap == null) {
            filedTypeConvertMap = createFiledTypeConvertMap();
        }
        return filedTypeConvertMap;
    }

    private Map<String, String> createFiledTypeConvertMap(){
        Map<String, String> filedTypeConvertMap = new LinkedHashMap<>(16);
        filedTypeConvertMap.put("varchar", "String");
        filedTypeConvertMap.put("tinytext", "String");
        filedTypeConvertMap.put("text", "String");
        filedTypeConvertMap.put("string", "String");
        filedTypeConvertMap.put("bool", "boolean");
        filedTypeConvertMap.put("integer", "Integer");
        return filedTypeConvertMap;
    }

    public void setFiledTypeConvertMap(Map<String, String> filedTypeConvertMap) {
        this.filedTypeConvertMap = filedTypeConvertMap;
    }

    public boolean isConvertArrayToList() {
        return convertArrayToList;
    }

    public void setConvertArrayToList(boolean convertArrayToList) {
        this.convertArrayToList = convertArrayToList;
    }

    @Override
    public String toString() {
        return "FieldParseConfig{" +
                "fieldColumn=" + fieldColumn +
                ", fieldTypeColumn=" + fieldTypeColumn +
                ", fieldCommentColumn=" + fieldCommentColumn +
                ", fieldModifier='" + fieldModifier + '\'' +
                ", generateGetterAndSetter=" + generateGetterAndSetter +
                ", generateToString=" + generateToString +
                ", filedTypeConvertMap=" + filedTypeConvertMap +
                ", convertArrayToList=" + convertArrayToList +
                '}';
    }
}
