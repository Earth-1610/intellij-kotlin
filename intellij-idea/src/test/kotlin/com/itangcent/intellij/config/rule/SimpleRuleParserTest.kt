package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import java.util.Collection
import java.util.List

/**
 * Test case of [SimpleRuleParser]
 */
internal class SimpleRuleParserTest : ContextLightCodeInsightFixtureTestCase() {

    @Inject
    private lateinit var ruleParser: RuleParser

    private lateinit var userCtrlPsiClass: PsiClass
    private lateinit var modelPsiClass: PsiClass
    private lateinit var listPsiClass: PsiClass

    private lateinit var greetingPsiMethod: PsiMethod

    private lateinit var getUserInfoPsiMethod: PsiMethod
    override fun bind(builder: ActionContext.ActionContextBuilder) {
        super.bind(builder)
        builder.bind(RuleParser::class) { it.with(SimpleRuleParser::class) }
    }

    override fun beforeBind() {
        super.beforeBind()
        loadSource(java.lang.Deprecated::class)
        loadSource(Object::class.java)!!
        loadSource(Collection::class.java)!!
        listPsiClass = loadSource(List::class.java)!!
        loadClass("annotation/Public.java")
        loadClass("spring/RequestMapping.java")
        loadClass("spring/GetMapping.java")
        userCtrlPsiClass = loadClass("api/UserCtrl.java")!!
        modelPsiClass = loadClass("model/Model.java")!!
        greetingPsiMethod = userCtrlPsiClass.methods[0]
        getUserInfoPsiMethod = userCtrlPsiClass.methods[1]
    }

    fun testParseStringRule() {
        val ruleReadRequestMapping: StringRule =
            ruleParser.parseStringRule("@org.springframework.web.bind.annotation.RequestMapping")!!
        assertEquals(
            "/greeting",
            ruleReadRequestMapping.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod))
        )
        assertEquals(
            null,
            ruleReadRequestMapping.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod))
        )

        val ruleReadGetMapping: StringRule =
            ruleParser.parseStringRule("@org.springframework.web.bind.annotation.GetMapping")!!
        assertEquals(null, ruleReadGetMapping.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod)))
        assertEquals(
            "/get/{id}",
            ruleReadGetMapping.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod))
        )

        val ruleReadTagFolder: StringRule = ruleParser.parseStringRule("#folder")!!
        assertEquals(null, ruleReadTagFolder.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod)))
        assertEquals(
            "update-apis",
            ruleReadTagFolder.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod))
        )

    }


    fun testParseBooleanRule() {

        val ruleCheckSimpleName: BooleanRule = ruleParser.parseBooleanRule("greeting")!!
        assertEquals(true, ruleCheckSimpleName.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod)))
        assertEquals(
            false,
            ruleCheckSimpleName.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod))
        )

        val ruleCheckQualifiedName: BooleanRule = ruleParser.parseBooleanRule("com.itangcent.api.UserCtrl#greeting")!!
        assertEquals(true, ruleCheckQualifiedName.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod)))
        assertEquals(
            false,
            ruleCheckQualifiedName.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod))
        )

        val ruleCheckPublic: BooleanRule = ruleParser.parseBooleanRule("@com.itangcent.common.annotation.Public")!!
        assertEquals(true, ruleCheckPublic.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod)))
        assertEquals(false, ruleCheckPublic.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod)))

        val ruleCheckNotPublic: BooleanRule = ruleParser.parseBooleanRule("!@com.itangcent.common.annotation.Public")!!
        assertEquals(false, ruleCheckNotPublic.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod)))
        assertEquals(true, ruleCheckNotPublic.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod)))

        val ruleCheckDeprecated: BooleanRule = ruleParser.parseBooleanRule("@java.lang.Deprecated")!!
        assertEquals(false, ruleCheckDeprecated.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod)))
        assertEquals(
            true,
            ruleCheckDeprecated.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod))
        )

        val ruleCheckUndone: BooleanRule = ruleParser.parseBooleanRule("#undone")!!
        assertEquals(false, ruleCheckUndone.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod)))
        assertEquals(
            true,
            ruleCheckUndone.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod))
        )

        val ruleCheckDone: BooleanRule = ruleParser.parseBooleanRule("!#undone")!!
        assertEquals(true, ruleCheckDone.compute(ruleParser.contextOf(greetingPsiMethod, greetingPsiMethod)))
        assertEquals(
            false,
            ruleCheckDone.compute(ruleParser.contextOf(getUserInfoPsiMethod, getUserInfoPsiMethod))
        )

        val ruleCheckIsCollection = ruleParser.parseBooleanRule("\$class:? extend java.util.Collection")!!
        assertEquals(false, ruleCheckIsCollection.compute(ruleParser.contextOf(modelPsiClass, modelPsiClass)))
        assertEquals(
            true,
            ruleCheckIsCollection.compute(ruleParser.contextOf(listPsiClass, listPsiClass))
        )
    }

    fun testParseEventRule() {
        assertNull(ruleParser.parseEventRule("any"))
    }
}