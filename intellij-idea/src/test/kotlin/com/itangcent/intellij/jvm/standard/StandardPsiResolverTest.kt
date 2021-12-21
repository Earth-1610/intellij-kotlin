package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.itangcent.common.utils.GsonUtils
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.jvm.PsiResolver
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase

/**
 * Test case of [StandardPsiResolver]
 */
internal class StandardPsiResolverTest : ContextLightCodeInsightFixtureTestCase() {

    private lateinit var linkCasePsiClass: PsiClass
    private lateinit var userInfoPsiClass: PsiClass
    private lateinit var modelPsiClass: PsiClass
    private lateinit var nestedClassPsiClass: PsiClass
    private lateinit var nestedClassBPsiClass: PsiClass
    private lateinit var myInnerClassPsiClass: PsiClass
    private lateinit var innerClassAPsiClass: PsiClass
    private lateinit var staticInnerClassAPsiClass: PsiClass
    private lateinit var innerClassBPsiClass: PsiClass
    private lateinit var staticInnerClassBPsiClass: PsiClass
    private lateinit var numbersPsiClass: PsiClass
    private lateinit var javaVersionPsiClass: PsiClass

    @Inject
    private lateinit var psiResolver: PsiResolver

    override fun beforeBind() {
        linkCasePsiClass = loadClass("cases/LinkCase.java")!!
        userInfoPsiClass = loadClass("model/UserInfo.java")!!
        modelPsiClass = loadClass("model/Model.java")!!
        nestedClassPsiClass = loadClass("cases/NestedClass.java")!!
        nestedClassBPsiClass = loadClass("cases/NestedClassB.java")!!
        numbersPsiClass = loadClass("constant/Numbers.java")!!
        javaVersionPsiClass = loadClass("constant/JavaVersion.java")!!
        myInnerClassPsiClass = linkCasePsiClass.allInnerClasses.first()

        innerClassAPsiClass = nestedClassPsiClass.allInnerClasses.first()
        staticInnerClassAPsiClass = nestedClassPsiClass.allInnerClasses.last()

        innerClassBPsiClass = nestedClassBPsiClass.allInnerClasses.first()
        staticInnerClassBPsiClass = nestedClassBPsiClass.allInnerClasses.last()
    }

    override fun bind(builder: ActionContext.ActionContextBuilder) {
        super.bind(builder)
        builder.bind(PsiResolver::class) {
            it.with(StandardPsiResolver::class)
        }
    }

    fun testResolveClassWithPropertyOrMethod() {
        psiResolver.resolveClassWithPropertyOrMethod("#methodA", linkCasePsiClass)!!.let {
            assertEquals(linkCasePsiClass, it.first)
            assertEquals(linkCasePsiClass.methods[0], it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("UserInfo", linkCasePsiClass)!!.let {
            assertEquals(userInfoPsiClass, it.first)
            assertNull(it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("MyInnerClass", linkCasePsiClass)!!.let {
            assertEquals(myInnerClassPsiClass, it.first)
            assertNull(it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("InnerClassA", linkCasePsiClass)!!.let {
            assertEquals(innerClassAPsiClass, it.first)
            assertNull(it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("InnerClassB", linkCasePsiClass)!!.let {
            assertEquals(innerClassBPsiClass, it.first)
            assertNull(it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("StaticInnerClassA", linkCasePsiClass)!!.let {
            assertEquals(staticInnerClassAPsiClass, it.first)
            assertNull(it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("StaticInnerClassB", linkCasePsiClass)!!.let {
            assertEquals(staticInnerClassBPsiClass, it.first)
            assertNull(it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("com.itangcent.model.Model", linkCasePsiClass)!!.let {
            assertEquals(modelPsiClass, it.first)
            assertNull(it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("JAVA_0_9", linkCasePsiClass)!!.let {
            assertEquals(javaVersionPsiClass, it.first)
            assertEquals(javaVersionPsiClass.allFields[0], it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("ONE", linkCasePsiClass)!!.let {
            assertEquals(numbersPsiClass, it.first)
            assertEquals(numbersPsiClass.allFields[0], it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("UserInfo#getId", linkCasePsiClass)!!.let {
            assertEquals(userInfoPsiClass, it.first)
            assertEquals(userInfoPsiClass.allMethods[0], it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("com.itangcent.model.Model#getStr()", linkCasePsiClass)!!.let {
            assertEquals(modelPsiClass, it.first)
            assertEquals(modelPsiClass.allMethods[0], it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("UserInfo#id", linkCasePsiClass)!!.let {
            assertEquals(userInfoPsiClass, it.first)
            assertEquals(userInfoPsiClass.allFields[0], it.second)
        }
        psiResolver.resolveClassWithPropertyOrMethod("com.itangcent.model.Model#str", linkCasePsiClass)!!.let {
            assertEquals(modelPsiClass, it.first)
            assertEquals(modelPsiClass.allFields[0], it.second)
        }
    }

    fun testResolveEnumFields() {
        assertEquals(
            "{\"params\":{\"name\":\"0.9\",\"value\":1.5},\"name\":\"JAVA_0_9\",\"ordinal\":0,\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"}",
            GsonUtils.toJson(psiResolver.resolveEnumFields(0, javaVersionPsiClass.allFields[0]))
        )
        assertNull(
            psiResolver.resolveEnumFields(0, numbersPsiClass.allFields[0])
        )
    }

    fun testResolveRefText() {
        assertEquals(
            userInfoPsiClass.text,
            psiResolver.resolveRefText(userInfoPsiClass)
        )
        assertEquals(
            "1",
            psiResolver.resolveRefText(numbersPsiClass.fields[0])
        )
        assertEquals(
            "PsiEnumConstant:JAVA_0_9",
            psiResolver.resolveRefText(javaVersionPsiClass.fields[0])
        )
    }
}