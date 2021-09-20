package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import junit.framework.Assert
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * Test case of [DefaultRuleComputer]
 */
internal class DefaultRuleComputerTest : ContextLightCodeInsightFixtureTestCase() {

    @Inject
    private lateinit var ruleComputer: RuleComputer

    private lateinit var commentDemoPsiClass: PsiClass

    override fun bind(builder: ActionContext.ActionContextBuilder) {
        super.bind(builder)
        builder.bind(RuleParser::class) { it.with(RuleParserImpl::class) }
    }

    override fun beforeBind() {
        super.beforeBind()
        loadSource(java.lang.Deprecated::class)
        loadSource(String::class.java)!!
        loadSource(Object::class.java)!!
        loadSource(Collection::class.java)!!
        commentDemoPsiClass = loadClass("model/CommentDemo.java")!!
    }

    override fun customConfig(): String {
        return "json.rule.field.name=@com.fasterxml.jackson.annotation.JsonProperty#value\n" +
                "single=#single\n" +
                "multi=#multi\n" +
                "singleOrMulti=#single\n" +
                "singleOrMulti=#multi\n" +
                "singleAndDesc=#single\n" +
                "singleAndDesc=#desc\n" +
                "singleString=~Xxx\n" +
                "singleString=~Yyy\n" +
                "mergeString=~Xxx\n" +
                "mergeString=~Yyy\n" +
                "mergeString=#tag\n" +
                "mergeString=public\n" +
                "distinctString=~Xxx\n" +
                "distinctString=~Yyy\n" +
                "distinctString=#tag\n" +
                "distinctString=public\n" +
                "eventIgnore=ILLEGAL\n" +
                "eventIgnore=NPE\n" +
                "eventIgnore=COUNT\n" +
                "eventThrow=ILLEGAL\n" +
                "eventThrow=NPE\n" +
                "eventThrow=COUNT"
    }

    fun testComputerBoolean() {
        val ruleSingle: RuleKey<Boolean> =
            SimpleRuleKey(
                "single", BooleanRuleMode.ANY
            )
        val ruleMulti: RuleKey<Boolean> =
            SimpleRuleKey(
                "multi", BooleanRuleMode.ANY
            )
        val ruleSingleOrMulti: RuleKey<Boolean> =
            SimpleRuleKey(
                "singleOrMulti", BooleanRuleMode.ANY
            )
        val ruleSingleAndDesc: RuleKey<Boolean> =
            SimpleRuleKey(
                "singleAndDesc", BooleanRuleMode.ALL
            )
        run {
            val psiField = commentDemoPsiClass.fields[0]
            assertEquals(true, ruleComputer.computer(ruleSingle, psiField))
            assertEquals(false, ruleComputer.computer(ruleMulti, psiField))
            assertEquals(true, ruleComputer.computer(ruleSingleOrMulti, psiField))
            assertEquals(true, ruleComputer.computer(ruleSingleAndDesc, psiField))
        }
        run {
            val psiField = commentDemoPsiClass.fields[1]
            assertEquals(false, ruleComputer.computer(ruleSingle, psiField))
            assertEquals(true, ruleComputer.computer(ruleMulti, psiField))
            assertEquals(true, ruleComputer.computer(ruleSingleOrMulti, psiField))
            assertEquals(false, ruleComputer.computer(ruleSingleAndDesc, psiField))
        }
    }

    fun testComputerString() {
        val ruleSingle: RuleKey<String> =
            SimpleRuleKey(
                "singleString", StringRuleMode.SINGLE
            )
        val ruleMerge: RuleKey<String> =
            SimpleRuleKey(
                "mergeString", StringRuleMode.MERGE
            )
        val ruleDistinct: RuleKey<Boolean> =
            SimpleRuleKey(
                "distinctString", StringRuleMode.MERGE_DISTINCT
            )
        run {
            val psiField = commentDemoPsiClass.fields[0]
            assertEquals("com.itangcent.model.CommentDemo#aXxx", ruleComputer.computer(ruleSingle, psiField))
            assertEquals(
                "com.itangcent.model.CommentDemo#aXxx\n" +
                        "com.itangcent.model.CommentDemo#aYyy\n" +
                        "public\n" +
                        "public", ruleComputer.computer(ruleMerge, psiField)
            )
            assertEquals(
                "com.itangcent.model.CommentDemo#aXxx\n" +
                        "com.itangcent.model.CommentDemo#aYyy\n" +
                        "public", ruleComputer.computer(ruleDistinct, psiField)
            )
        }
        run {
            val psiField = commentDemoPsiClass.fields[1]
            assertEquals("com.itangcent.model.CommentDemo#bXxx", ruleComputer.computer(ruleSingle, psiField))
            assertEquals(
                "com.itangcent.model.CommentDemo#bXxx\n" +
                        "com.itangcent.model.CommentDemo#bYyy\n" +
                        "public", ruleComputer.computer(ruleMerge, psiField)
            )
            assertEquals(
                "com.itangcent.model.CommentDemo#bXxx\n" +
                        "com.itangcent.model.CommentDemo#bYyy\n" +
                        "public", ruleComputer.computer(ruleDistinct, psiField)
            )
        }
    }

    fun testComputerEvent() {
        val ruleIgnore: RuleKey<Unit> =
            SimpleRuleKey(
                "eventIgnore", EventRuleMode.IGNORE_ERROR
            )
        val ruleThrow: RuleKey<Unit> =
            SimpleRuleKey(
                "eventThrow", EventRuleMode.THROW_IN_ERROR
            )
        run {
            val psiField = commentDemoPsiClass.fields[0]
            assertThrows<IllegalArgumentException> {
                ruleComputer.computer(ruleThrow, psiField)
            }
            Assert.assertEquals(0, cnt)
            assertDoesNotThrow {
                ruleComputer.computer(ruleIgnore, psiField)
            }
            Assert.assertEquals(1, cnt)
        }
        run {
            val psiField = commentDemoPsiClass.fields[1]
            assertThrows<IllegalArgumentException> {
                ruleComputer.computer(ruleThrow, psiField)
            }
            Assert.assertEquals(1, cnt)
            assertDoesNotThrow {
                ruleComputer.computer(ruleIgnore, psiField)
            }
            Assert.assertEquals(2, cnt)
        }
    }
}

var cnt = 0

internal class RuleParserImpl : SimpleRuleParser() {

    override fun parseEventRule(rule: String): EventRule? {
        return EventRule.of {
            when (rule) {
                "ILLEGAL" -> {
                    throw IllegalArgumentException()
                }
                "NPE" -> {
                    throw NullPointerException()
                }
                "COUNT" -> {
                    ++cnt
                }
            }
        }
    }
}