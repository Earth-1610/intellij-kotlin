package com.itangcent.intellij.config

interface RuleParser {
    fun parseStringRule(rule: String): List<SimpleStringRule>
    fun parseStringRule(rule: String, delimiters: String): List<SimpleStringRule>
    fun parseBooleanRule(rule: String): List<SimpleBooleanRule>
    fun parseBooleanRule(rule: String, delimiters: String, defaultValue: Boolean): List<SimpleBooleanRule>
}