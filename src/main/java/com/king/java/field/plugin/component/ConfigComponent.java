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
package com.king.java.field.plugin.component;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.king.java.field.plugin.entity.FieldParseConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author <a href="mailto:yujinlin@mail.com">Jenly</a>
 */
@State(name = "com.king.java.bean.plugin.component.ConfigComponent", storages = {@Storage("configComponent.xml")})
public class ConfigComponent implements PersistentStateComponent<ConfigComponent> {

    private FieldParseConfig fieldParseConfig;

    public static ConfigComponent getInstance() {
        return ApplicationManager.getApplication().getService(ConfigComponent.class);
    }

    @Override
    public @Nullable ConfigComponent getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ConfigComponent state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public FieldParseConfig getFieldParseConfig() {
        if (fieldParseConfig == null) {
            fieldParseConfig = new FieldParseConfig();
        }
        return fieldParseConfig;
    }

    public void setFieldParseConfig(FieldParseConfig fieldParseConfig) {
        this.fieldParseConfig = fieldParseConfig;
    }
}
