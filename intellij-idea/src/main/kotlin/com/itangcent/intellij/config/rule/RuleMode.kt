package com.itangcent.intellij.config.rule

interface RuleMode {
}

enum class StringRuleMode : RuleMode {
    SINGLE,
    MERGE,
    MERGE_DISTINCT
}

enum class BooleanRuleMode : RuleMode {
    ANY,
    ALL
}

enum class EventRuleMode : RuleMode {
    IGNORE_ERROR,
    THROW_IN_ERROR,
}
