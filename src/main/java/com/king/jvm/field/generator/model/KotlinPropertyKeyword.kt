package com.king.jvm.field.generator.model

/**
 * Kotlin 属性关键字枚举
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
enum class KotlinPropertyKeyword(val value: String) {
    VAL("val"),
    VAR("var");

    override fun toString(): String {
        return value
    }
}
