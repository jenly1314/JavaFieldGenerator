package com.king.jvm.field.generator.model

/**
 * 字段注解类型枚举
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
enum class AnnotationType(val type: Int, val displayName: String) {
    NONE(0, "None"),
    GSON(1, "Gson"),
    MOSHI(2, "Moshi"),
    JACKSON(3, "Jackson"),
    FASTJSON(4, "FastJson"),
    KOTLIN_SERIALIZATION(5, "Kotlin Serialization"),
    CUSTOM(6, "Custom");

    companion object {
        fun fromType(type: Int): AnnotationType {
            return values().find { it.type == type } ?: NONE
        }
    }

    override fun toString(): String {
        return displayName
    }
}
