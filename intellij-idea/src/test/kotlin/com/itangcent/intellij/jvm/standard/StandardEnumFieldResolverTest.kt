package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.*
import com.itangcent.common.utils.GsonUtils
import com.itangcent.intellij.context.ActionContextBuilder
import com.itangcent.intellij.extend.guice.with
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import junit.framework.Assert
import org.mockito.Mockito
import org.mockito.kotlin.mock

/**
 * Test case of [StandardEnumFieldResolver]
 */
internal class StandardEnumFieldResolverTest : ContextLightCodeInsightFixtureTestCase() {

    @Inject
    private lateinit var standardEnumFieldResolver: StandardEnumFieldResolver

    private lateinit var javaVersionPsiClass: PsiClass

    private lateinit var myConstantPsiClass: PsiClass

    private lateinit var myNoArgConstantPsiClass: PsiClass

    override fun beforeBind() {
        super.beforeBind()
        loadSource(Object::class.java)!!
        loadSource(java.lang.String::class)!!
        javaVersionPsiClass = loadClass("constant/JavaVersion.java")!!
        myConstantPsiClass = loadClass("constant/MyConstant.java")!!
        myNoArgConstantPsiClass = loadClass("constant/MyNoArgConstant.java")!!
    }

    override fun bind(builder: ActionContextBuilder) {
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
            Assert.assertEquals(
                "{\"name\":\"default\",\"value\":0}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[6]))
            )
        }
        run {
            val fields = myNoArgConstantPsiClass.fields
                .filterIsInstance<PsiEnumConstant>()
            Assert.assertEquals(
                "{}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[0] as PsiEnumConstant))
            )
            Assert.assertEquals(
                "{}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[1]))
            )
            Assert.assertEquals(
                "{}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(fields[2]))
            )
        }
    }

    fun testResolveEnumFieldsOfClsClass() {
        val construct0: PsiMethod
        run {
            val param0: PsiParameter = mock {
                Mockito.`when`(it.name).thenReturn("i")
                Mockito.`when`(it.isVarArgs).thenReturn(false)
            }
            val param1: PsiParameter = mock {
                Mockito.`when`(it.name).thenReturn("s")
                Mockito.`when`(it.isVarArgs).thenReturn(false)
            }
            val parameterList: PsiParameterList = mock {
                Mockito.`when`(it.parameters).thenReturn(arrayOf(param0, param1))
            }
            construct0 = mock {
                Mockito.`when`(it.text).thenReturn(
                    "private Level(int i, String s) {\n" +
                            "        this.levelInt = i;\n" +
                            "        this.levelStr = s;\n" +
                            "    }"
                )
                Mockito.`when`(it.parameterList).thenReturn(parameterList)
            }
        }
        val construct1: PsiMethod
        run {
            val parameterList: PsiParameterList = mock {
                Mockito.`when`(it.parameters).thenReturn(emptyArray())
            }
            construct1 = mock {
                Mockito.`when`(it.text).thenReturn(
                    "private Level() {\n" +
                            "        this.levelInt = -1;\n" +
                            "        this.levelStr = \"default\";\n" +
                            "    }"
                )
                Mockito.`when`(it.parameterList).thenReturn(parameterList)
            }
        }
        val psiClass: PsiClass = mock {
            Mockito.`when`(it.constructors).thenReturn(
                arrayOf(construct0, construct1)
            )
        }
        run {
            val psiEnumConstant: PsiEnumConstant = mock {
                Mockito.`when`(it.resolveConstructor()).thenReturn(null)
                Mockito.`when`(it.text).thenReturn("ERROR(40, \"ERROR\")")
                Mockito.`when`(it.containingClass).thenReturn(psiClass)
            }
            Assert.assertEquals(
                "{\"levelInt\":40,\"levelStr\":\"ERROR\"}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(psiEnumConstant))
            )
        }
        run {
            val psiEnumConstant: PsiEnumConstant = mock {
                Mockito.`when`(it.resolveConstructor()).thenReturn(null)
                Mockito.`when`(it.text).thenReturn("DEFAULT")
                Mockito.`when`(it.containingClass).thenReturn(psiClass)
            }
            Assert.assertEquals(
                "{\"levelInt\":-1,\"levelStr\":\"default\"}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(psiEnumConstant))
            )
        }
        run {
            val psiEnumConstant: PsiEnumConstant = mock {
                Mockito.`when`(it.resolveConstructor()).thenReturn(null)
                Mockito.`when`(it.text).thenReturn("DEFAULT()")
                Mockito.`when`(it.containingClass).thenReturn(psiClass)
            }
            Assert.assertEquals(
                "{\"levelInt\":-1,\"levelStr\":\"default\"}",
                GsonUtils.toJson(standardEnumFieldResolver.resolveEnumFields(psiEnumConstant))
            )
        }
    }
}