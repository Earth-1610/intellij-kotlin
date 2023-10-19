package com.itangcent.intellij.psi

import com.itangcent.intellij.config.rule.*

object ClassRuleKeys {

    val FIELD_NAME: RuleKey<String> = SimpleRuleKey(
        "field.name",
        arrayOf("json.rule.field.name"),
        StringRuleMode.SINGLE
    )

    val FIELD_NAME_PREFIX: RuleKey<String> = SimpleRuleKey(
        "field.name.prefix",
        arrayOf("json.rule.field.prefix"),
        StringRuleMode.SINGLE
    )

    val FIELD_NAME_SUFFIX: RuleKey<String> = SimpleRuleKey(
        "field.name.suffix",
        arrayOf("json.rule.field.suffix"),
        StringRuleMode.SINGLE
    )

    val FIELD_TYPE: RuleKey<String> = SimpleRuleKey(
        "field.type",
        StringRuleMode.SINGLE
    )

    val FIELD_IGNORE: RuleKey<Boolean> =
        SimpleRuleKey(
            "field.ignore",
            arrayOf("json.rule.field.ignore"),
            BooleanRuleMode.ANY
        )

    val FIELD_ORDER: RuleKey<Int> =
        SimpleRuleKey(
            "field.order",
            IntRuleMode
        )

    val ENUM_USE_CUSTOM: RuleKey<String> =
        SimpleRuleKey(
            "enum.use.custom",
            StringRuleMode.SINGLE
        )

    val ENUM_USE_BY_TYPE: RuleKey<Boolean> =
        SimpleRuleKey(
            "enum.use.by.type",
            BooleanRuleMode.ANY
        )

    val ENUM_USE_NAME: RuleKey<Boolean> =
        SimpleRuleKey(
            "enum.use.name",
            BooleanRuleMode.ANY
        )

    val ENUM_USE_ORDINAL: RuleKey<Boolean> =
        SimpleRuleKey(
            "enum.use.ordinal",
            BooleanRuleMode.ANY
        )

    val FIELD_DOC: RuleKey<String> = SimpleRuleKey(
        "field.doc",
        arrayOf("doc.field"),
        StringRuleMode.MERGE_DISTINCT
    )

    val TYPE_IS_FILE: RuleKey<Boolean> =
        SimpleRuleKey("type.is_file", BooleanRuleMode.ANY)

    val CLASS_CONVERT: RuleKey<String> =
        SimpleRuleKey("json.rule.convert", StringRuleMode.SINGLE)

    val ENUM_CONVERT: RuleKey<String> =
        SimpleRuleKey("json.rule.enum.convert", StringRuleMode.SINGLE)

    val CONSTANT_FIELD_IGNORE: RuleKey<Boolean> =
        SimpleRuleKey("constant.field.ignore", BooleanRuleMode.ANY)

    val JSON_GROUP: RuleKey<String> =
        SimpleRuleKey("json.group", StringRuleMode.MERGE)

    /**
     * Rule used to indicate that a property/field should be parsed "unwrapped" -- that is,
     * its properties are instead included as properties of its containing Object.
     */
    val JSON_UNWRAPPED: RuleKey<Boolean> =
        SimpleRuleKey("json.unwrapped", BooleanRuleMode.ANY)

}