package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.*
import com.itangcent.intellij.context.ActionContextBuilder
import com.itangcent.intellij.extend.guice.with
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import com.itangcent.testFramework.assertMapEquals
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
            assertMapEquals(
                mapOf(
                    "name" to "0.9",
                    "value" to 1.5
                ),
                standardEnumFieldResolver.resolveEnumFields(fields[0] as PsiEnumConstant)
            )
        }
        run {
            val fields = myConstantPsiClass.fields
                .filterIsInstance<PsiEnumConstant>()
            assertMapEquals(
                mapOf(
                    "name" to "a",
                    "value" to 1.1,
                    "fullName" to "ONE/1.1",
                    "code" to "ONE"
                ),
                standardEnumFieldResolver.resolveEnumFields(fields[0] as PsiEnumConstant)
            )
            assertMapEquals(
                mapOf(
                    "name" to "b-s",
                    "value" to 4.4,
                    "fullName" to "TWO/4.4",
                    "code" to "TWO"
                ),
                standardEnumFieldResolver.resolveEnumFields(fields[1])
            )
            assertMapEquals(
                mapOf(
                    "name" to "c",
                    "value" to 3.3,
                    "fullName" to "THREE/3.3",
                    "code" to "THREE"
                ),
                standardEnumFieldResolver.resolveEnumFields(fields[2])
            )
            assertMapEquals(
                mapOf(
                    "name" to "d",
                    "value" to 0,
                    "fullName" to "FOUR/0",
                    "code" to "FOUR"
                ),
                standardEnumFieldResolver.resolveEnumFields(fields[3])
            )
            assertMapEquals(
                mapOf(
                    "name" to "default:5.5",
                    "value" to 5.5,
                    "fullName" to "FIVE/5.5",
                    "code" to "FIVE"
                ),
                standardEnumFieldResolver.resolveEnumFields(fields[4])
            )
            assertMapEquals(
                mapOf(
                    "name" to "f",
                    "value" to 23.1,
                    "fullName" to "SIX/23.1",
                    "code" to "SIX"
                ),
                standardEnumFieldResolver.resolveEnumFields(fields[5])
            )
            assertMapEquals(
                mapOf(
                    "name" to "default",
                    "value" to 0,
                    "fullName" to "SEVEN/0",
                    "code" to "SEVEN"
                ),
                standardEnumFieldResolver.resolveEnumFields(fields[6])
            )
        }
        run {
            val fields = myNoArgConstantPsiClass.fields
                .filterIsInstance<PsiEnumConstant>()
            assertMapEquals(
                mapOf<String, Any>(),
                standardEnumFieldResolver.resolveEnumFields(fields[0] as PsiEnumConstant)
            )
            assertMapEquals(
                mapOf<String, Any>(),
                standardEnumFieldResolver.resolveEnumFields(fields[1])
            )
            assertMapEquals(
                mapOf<String, Any>(),
                standardEnumFieldResolver.resolveEnumFields(fields[2])
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
            Mockito.`when`(it.methods).thenReturn(
                emptyArray<PsiMethod>()
            )
            Mockito.`when`(it.interfaces).thenReturn(
                emptyArray<PsiClass>()
            )
        }
        run {
            val psiEnumConstant: PsiEnumConstant = mock {
                Mockito.`when`(it.resolveConstructor()).thenReturn(null)
                Mockito.`when`(it.text).thenReturn("ERROR(40, \"ERROR\")")
                Mockito.`when`(it.containingClass).thenReturn(psiClass)
            }
            assertMapEquals(
                mapOf(
                    "levelInt" to 40,
                    "levelStr" to "ERROR"
                ),
                standardEnumFieldResolver.resolveEnumFields(psiEnumConstant)
            )
        }
        run {
            val psiEnumConstant: PsiEnumConstant = mock {
                Mockito.`when`(it.resolveConstructor()).thenReturn(null)
                Mockito.`when`(it.text).thenReturn("DEFAULT")
                Mockito.`when`(it.containingClass).thenReturn(psiClass)
            }
            assertMapEquals(
                mapOf(
                    "levelInt" to -1,
                    "levelStr" to "default"
                ),
                standardEnumFieldResolver.resolveEnumFields(psiEnumConstant)
            )
        }
        run {
            val psiEnumConstant: PsiEnumConstant = mock {
                Mockito.`when`(it.resolveConstructor()).thenReturn(null)
                Mockito.`when`(it.text).thenReturn("DEFAULT()")
                Mockito.`when`(it.containingClass).thenReturn(psiClass)
            }
            assertMapEquals(
                mapOf(
                    "levelInt" to -1,
                    "levelStr" to "default"
                ),
                standardEnumFieldResolver.resolveEnumFields(psiEnumConstant)
            )
        }
    }
}