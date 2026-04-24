package com.king.jvm.field.generator.model

/**
 * 字段排序方式枚举
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
enum class FieldSortStyle(private val displayName: String) {
    DEFAULT("Default Order"),
    FIELD_NAME_LOCAL("Field Name (New Fields Only)"),
    FIELD_NAME_GLOBAL("Field Name (Overall)");

    override fun toString(): String {
        return displayName
    }
}
