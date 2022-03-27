package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.itangcent.common.utils.GsonUtils
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import junit.framework.Assert

/**
 * Test case of [StandardEnumFieldResolver]
 */
internal class StandardEnumFieldResolverTest : ContextLightCodeInsightFixtureTestCase() {

    @Inject
    private lateinit var standardEnumFieldResolver: StandardEnumFieldResolver

    private lateinit var javaVersionPsiClass: PsiClass

    private lateinit var myConstantPsiClass: PsiClass

    override fun beforeBind() {
        super.beforeBind()
        loadSource(Object::class.java)!!
        loadSource(java.lang.String::class)!!
        javaVersionPsiClass = loadClass("constant/JavaVersion.java")!!
        myConstantPsiClass = loadClass("constant/MyConstant.java")!!
    }

    override fun bind(builder: ActionContext.ActionContextBuilder) {
        super.bind(builder)
        builder.bind(StandardEnumFieldResolver::class.java) { it.with(StandardEnumFieldResolverImpl::class) }
    }


    fun testResolveEnumFields() {
        run {
            val fields = javaVersionPsiClass.fields
            Assert.assertEquals(
                "{\"name\":\"0.9\",\"value\":1.5}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[0] as PsiEnumConstant))
            )
        }
        run {
            val fields = myConstantPsiClass.fields
                .filterIsInstance<PsiEnumConstant>()
            Assert.assertEquals(
                "{\"name\":\"a\",\"value\":1.1}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[0] as PsiEnumConstant))
            )
            Assert.assertEquals(
                "{\"name\":\"b-s\",\"value\":4.4}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[1]))
            )
            Assert.assertEquals(
                "{\"name\":\"c\",\"value\":3.3}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[2]))
            )
            Assert.assertEquals(
                "{\"name\":\"d\",\"value\":0}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[3]))
            )
            Assert.assertEquals(
                "{\"name\":\"default:5.5\",\"value\":5.5}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[4]))
            )
            Assert.assertEquals(
                "{\"name\":\"f\",\"value\":23.1}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[5]))
            )
        }
    }
}