package com.itangcent.intellij.psi

import com.itangcent.intellij.config.rule.*

object ClassRuleKeys {

    val FIELD_NAME: RuleKey<String> = SimpleRuleKey(
        "field.name",
        arrayOf("json.rule.field.name"),
        StringRule::class, StringRuleMode.SINGLE
    )

    val FIELD_IGNORE: RuleKey<Boolean> =
        SimpleRuleKey(
            "field.ignore",
            arrayOf("json.rule.field.ignore"),
            BooleanRule::class, BooleanRuleMode.ANY
        )

    val ENUM_USE_NAME: RuleKey<Boolean> =
        SimpleRuleKey(
            "enum.use.name",
            BooleanRule::class, BooleanRuleMode.ANY
        )

    val ENUM_USE_ORDINAL: RuleKey<Boolean> =
        SimpleRuleKey(
            "enum.use.ordinal",
            BooleanRule::class, BooleanRuleMode.ANY
        )

    val FIELD_DOC: RuleKey<String> = SimpleRuleKey(
        "field.doc",
        arrayOf("doc.field"),
        StringRule::class, StringRuleMode.MERGE
    )

    val TYPE_IS_FILE: RuleKey<Boolean> =
        SimpleRuleKey("type.is_file", BooleanRule::class, BooleanRuleMode.ANY)

    val CLASS_CONVERT: RuleKey<String> =
        SimpleRuleKey("json.rule.convert", StringRule::class, StringRuleMode.SINGLE)

    val ENUM_CONVERT: RuleKey<String> =
        SimpleRuleKey("json.rule.enum.convert", StringRule::class, StringRuleMode.SINGLE)

    val CONSTANT_FIELD_IGNORE: RuleKey<Boolean> =
        SimpleRuleKey("constant.field.ignore", BooleanRule::class, BooleanRuleMode.ANY)

}