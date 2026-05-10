package com.king.jvm.field.generator.model

/**
 * 输入值分隔符枚举
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
enum class ValueSeparator(
    private val displayName: String,
    val symbol: String,
    val validationRegex: String
) {
    TAB("Tab (TSV)", "\t", "[^\\t]+\\t[^\\t]*(?:\\t[^\\t]*)*"),
    COMMA("Comma (CSV)", ",", "[^,]+,[^,]*(?:,[^,]*)*"),
    SEMICOLON("Semicolon (SSV)", ";", "[^;]+;[^;]*(?:;[^;]*)*"),
    PIPE("Pipe (PSV)", "|", "[^|]+\\|[^|]*(?:\\|[^|]*)*");

    override fun toString(): String {
        return displayName
    }
}


