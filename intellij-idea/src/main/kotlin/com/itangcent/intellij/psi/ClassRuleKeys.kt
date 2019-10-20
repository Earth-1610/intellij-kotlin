package com.itangcent.intellij.psi

import com.itangcent.intellij.config.rule.*

object ClassRuleKeys {

    val FIELD_NAME: RuleKey<String> = SimpleRuleKey("json.rule.field.name", StringRule::class, StringRuleMode.SINGLE)

    val FIELD_IGNORE: RuleKey<Boolean> =
        SimpleRuleKey("json.rule.field.ignore", BooleanRule::class, BooleanRuleMode.ANY)

    val FIELD_DOC: RuleKey<String> = SimpleRuleKey("doc.field", StringRule::class, StringRuleMode.MERGE)

    val TYPE_IS_FILE: RuleKey<Boolean> =
        SimpleRuleKey("type.is_file", BooleanRule::class, BooleanRuleMode.ANY)

    val CLASS_CONVERT: RuleKey<String> =
        SimpleRuleKey("json.rule.convert", StringRule::class, StringRuleMode.SINGLE)

    val ENUM_CONVERT: RuleKey<String> =
        SimpleRuleKey("json.rule.enum.convert", StringRule::class, StringRuleMode.SINGLE)
}