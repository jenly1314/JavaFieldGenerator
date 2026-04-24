package com.king.jvm.field.generator.model

/**
 * 空安全策略枚举
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
enum class NullabilityMode(val mode: Int, val displayName: String) {
    AUTO(0, "Auto"),
    NOT_NULL(1, "Not-Nullable"),
    NULLABLE(2, "Nullable");

    companion object {
        fun fromMode(mode: Int): NullabilityMode {
            return values().find { it.mode == mode } ?: AUTO
        }
    }

    override fun toString(): String {
        return displayName
    }
}
