package com.king.jvm.field.generator.model

/**
 * 字段命名风格枚举
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
enum class FieldNameStyle(private val displayName: String) {
    NONE("Keep Original"),
    CAMEL_CASE("Camel Case"),
    SNAKE_CASE("Snake Case");

    override fun toString(): String {
        return displayName
    }
}
