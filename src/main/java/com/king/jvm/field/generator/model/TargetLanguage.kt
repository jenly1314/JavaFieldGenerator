package com.king.jvm.field.generator.model

/**
 * 生成目标语言枚举
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
enum class TargetLanguage(val value: String, private val displayName: String) {
    AUTO("AUTO", "Auto"),
    JAVA("JAVA", "Java"),
    KOTLIN("KOTLIN", "Kotlin");

    override fun toString(): String {
        return displayName
    }
}
