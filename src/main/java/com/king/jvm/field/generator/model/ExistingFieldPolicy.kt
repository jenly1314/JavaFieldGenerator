package com.king.jvm.field.generator.model

/**
 * 已有字段处理策略枚举
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
enum class ExistingFieldPolicy(private val displayName: String) {
    IGNORE_NEW("Ignore New Field"),
    OVERWRITE_OLD("Overwrite Existing Field");

    override fun toString(): String {
        return displayName
    }
}
