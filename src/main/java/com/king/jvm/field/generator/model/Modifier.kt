package com.king.jvm.field.generator.model

/**
 * 字段访问修饰符枚举
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
enum class Modifier(val value: String, private val displayName: String) {
    PRIVATE("private", "private"),
    PROTECTED("protected", "protected"),
    PUBLIC("public", "public"),
    DEFAULT("", "default");

    override fun toString(): String {
        return displayName
    }
}
