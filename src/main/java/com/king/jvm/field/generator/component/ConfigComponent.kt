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
package com.king.jvm.field.generator.component

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.king.jvm.field.generator.model.FieldParseConfig

/**
 * 插件配置持久化组件
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
@State(name = "com.king.jvm.field.generator.component.ConfigComponent", storages = [Storage("configComponent.xml")])
class ConfigComponent : PersistentStateComponent<ConfigComponent> {

    var fieldParseConfig: FieldParseConfig = FieldParseConfig()

    override fun getState(): ConfigComponent {
        fieldParseConfig = normalizeFieldParseConfig(fieldParseConfig)
        return this
    }

    override fun loadState(state: ConfigComponent) {
        XmlSerializerUtil.copyBean(state, this)
        fieldParseConfig = normalizeFieldParseConfig(fieldParseConfig)
    }

    fun resolveFieldParseConfig(): FieldParseConfig {
        fieldParseConfig = normalizeFieldParseConfig(fieldParseConfig)
        return fieldParseConfig
    }

    private fun normalizeFieldParseConfig(config: FieldParseConfig?): FieldParseConfig {
        return (config ?: FieldParseConfig()).ensureDefaults()
    }

    companion object {
        @JvmStatic
        fun getInstance(): ConfigComponent {
            return ApplicationManager.getApplication().getService(ConfigComponent::class.java)
        }
    }
}
