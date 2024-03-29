package com.itangcent.intellij.jvm

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.itangcent.intellij.jvm.element.ExplicitClass
import com.itangcent.intellij.jvm.element.ExplicitField
import com.itangcent.intellij.jvm.element.ExplicitMethod
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Test case of [DuckTypeHelper]
 */
internal class DuckTypeHelperTest : ContextLightCodeInsightFixtureTestCase() {

    private lateinit var userInfoPsiClass: PsiClass

    private lateinit var userInfoDetailPsiClass: PsiClass

    private lateinit var userLoginInfoPsiClass: PsiClass

    private lateinit var userDetailCtrlPsiClass: PsiClass

    private lateinit var iResultPsiClass: PsiClass

    private lateinit var resultPsiClass: PsiClass

    private lateinit var userInfoDetailResultPsiClass: PsiClass

    private lateinit var genericCtrlPsiClass: PsiClass

    @Inject
    private lateinit var duckTypeHelper: DuckTypeHelper

    override fun beforeBind() {
        super.beforeBind()
        loadSource(java.lang.Deprecated::class)
        loadSource(java.lang.Long::class)!!
        loadSource(java.lang.Integer::class)!!
        loadSource(java.lang.String::class)!!
        loadSource(LocalDate::class)!!
        loadSource(LocalDateTime::class)!!
        loadSource(Object::class)!!
        loadSource(Collection::class)!!
        loadSource(List::class)!!
        iResultPsiClass = loadClass("model/IResult.java")!!
        resultPsiClass = loadClass("model/Result.java")!!
        userInfoPsiClass = loadClass("model/UserInfo.java")!!
        userInfoDetailPsiClass = loadClass("model/UserInfoDetail.java")!!
        userLoginInfoPsiClass = loadClass("model/UserLoginInfo.java")!!
        userInfoDetailResultPsiClass = loadClass("model/UserInfoDetailResult.java")!!
        genericCtrlPsiClass = loadClass("api/GenericCtrl.java")!!
        userDetailCtrlPsiClass = loadClass("api/UserDetailCtrl.java")!!
    }

    fun testResolve() {
        duckTypeHelper.resolve("?", userInfoPsiClass)!!.let {
            assertTrue(it.isSingle())
            assertEquals("java.lang.Object", it.canonicalText())
            assertEquals(it, it.unbox())
            assertEquals("Object", it.name())
            assertFalse(duckTypeHelper.isQualified(it))
        }
        duckTypeHelper.resolve("int", userInfoPsiClass)!!.let {
            assertTrue(it.isSingle())
            assertEquals("int", it.canonicalText())
            assertEquals(it, it.unbox())
            assertEquals("int", it.name())
            assertTrue(duckTypeHelper.isQualified(it))
        }
        duckTypeHelper.resolve("com.itangcent.model.UserInfo", userInfoPsiClass)!!.let {
            assertTrue(it.isSingle())
            assertEquals("com.itangcent.model.UserInfo", it.canonicalText())
            assertEquals(it, it.unbox())
            assertEquals("UserInfo", it.name())
            assertTrue(duckTypeHelper.isQualified(it))
        }
        duckTypeHelper.resolve("UserInfo", userInfoDetailPsiClass)!!.let {
            assertTrue(it.isSingle())
            assertEquals("com.itangcent.model.UserInfo", it.canonicalText())
            assertEquals(it, it.unbox())
            assertEquals("UserInfo", it.name())
            assertTrue(duckTypeHelper.isQualified(it))
        }
        duckTypeHelper.resolve("com.itangcent.model.UserInfo[]", userInfoPsiClass)!!.let {
            assertFalse(it.isSingle())
            assertEquals("com.itangcent.model.UserInfo[]", it.canonicalText())
            assertEquals("com.itangcent.model.UserInfo", it.unbox().canonicalText())
            assertEquals("UserInfo[]", it.name())
            assertTrue(duckTypeHelper.isQualified(it))
        }
        duckTypeHelper.resolve("List<com.itangcent.model.UserInfo>", userInfoPsiClass)!!.let {
            assertTrue(it.isSingle())
            assertEquals("java.util.List<com.itangcent.model.UserInfo>", it.canonicalText())
            assertEquals("java.util.List<com.itangcent.model.UserInfo>", it.unbox().canonicalText())
            assertEquals("List", it.name())
            assertTrue(duckTypeHelper.isQualified(it))
        }
        duckTypeHelper.resolve("com.itangcent.model.Result", userInfoPsiClass)!!.let {
            assertTrue(it.isSingle())
            assertEquals("com.itangcent.model.Result", it.canonicalText())
            assertEquals("com.itangcent.model.Result", it.unbox().canonicalText())
            assertEquals("Result", it.name())
            assertFalse(duckTypeHelper.isQualified(it))
        }
        duckTypeHelper.resolve("com.itangcent.model.Result<com.itangcent.model.UserInfo>", userInfoPsiClass)!!.let {
            assertTrue(it.isSingle())
            assertEquals("com.itangcent.model.Result<com.itangcent.model.UserInfo>", it.canonicalText())
            assertEquals("com.itangcent.model.Result<com.itangcent.model.UserInfo>", it.unbox().canonicalText())
            assertEquals("Result", it.name())
            assertTrue(duckTypeHelper.isQualified(it))
        }
    }

    fun testResolveClass() {
        assertNull(duckTypeHelper.resolveClass("?", userInfoPsiClass))
        assertNull(duckTypeHelper.resolveClass("int", userInfoPsiClass))
        assertEquals(userInfoPsiClass, duckTypeHelper.resolveClass("com.itangcent.model.UserInfo", userInfoPsiClass)!!)
        assertEquals("java.util.List", duckTypeHelper.resolveClass("List", userInfoPsiClass)!!.qualifiedName)
        assertEquals("java.lang.Long", duckTypeHelper.resolveClass("Long", userInfoPsiClass)!!.qualifiedName)
    }

    fun testBuildPsiType() {
        assertEquals("java.lang.Object", duckTypeHelper.buildPsiType("?", userInfoPsiClass)!!.canonicalText)
        assertNull(duckTypeHelper.buildPsiType("int", userInfoPsiClass))
        assertEquals(
            "com.itangcent.model.UserInfo",
            duckTypeHelper.buildPsiType("com.itangcent.model.UserInfo", userInfoPsiClass)!!.canonicalText
        )
        assertEquals("java.util.List", duckTypeHelper.buildPsiType("List", userInfoPsiClass)!!.canonicalText)
        assertEquals("java.lang.Long", duckTypeHelper.buildPsiType("Long", userInfoPsiClass)!!.canonicalText)
        assertEquals(
            "java.util.List<com.itangcent.model.UserInfo>",
            duckTypeHelper.buildPsiType("List<com.itangcent.model.UserInfo>", userInfoPsiClass)!!.canonicalText
        )
    }

    fun testExplicitUserInfo() {
        val userInfoExplicitClass = duckTypeHelper.explicit(userInfoPsiClass)
        assertEquals("UserInfo", userInfoExplicitClass.name())
        assertEquals(userInfoPsiClass, userInfoExplicitClass.psi())
        assertEquals(userInfoExplicitClass, userInfoExplicitClass.containClass())
        assertEquals(userInfoExplicitClass, userInfoExplicitClass.defineClass())
        assertEquals("com.itangcent.model.UserInfo", userInfoExplicitClass.toString())

        //test methods
        val methods = userInfoExplicitClass.methods()
        assertEquals(14, methods.size)

        run {
            val idGetterExplicitMethod = methods[0]
            assertEquals("getId", idGetterExplicitMethod.name())
            assertEquals(userInfoPsiClass.methods[0], idGetterExplicitMethod.psi())
            assertEquals(userInfoExplicitClass, idGetterExplicitMethod.containClass())
            assertEquals(userInfoExplicitClass, idGetterExplicitMethod.defineClass())
            assertEquals("java.lang.Long", idGetterExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.model.UserInfo#getId", idGetterExplicitMethod.toString())

            val parameters = idGetterExplicitMethod.getParameters()
            assertTrue(parameters.isEmpty())
        }

        run {
            val idSetterExplicitMethod = methods[1]
            assertEquals("setId", idSetterExplicitMethod.name())
            assertEquals(userInfoPsiClass.methods[1], idSetterExplicitMethod.psi())
            assertEquals(userInfoExplicitClass, idSetterExplicitMethod.containClass())
            assertEquals(userInfoExplicitClass, idSetterExplicitMethod.defineClass())
            assertEquals("void", idSetterExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.model.UserInfo#setId", idSetterExplicitMethod.toString())

            val parameters = idSetterExplicitMethod.getParameters()
            assertEquals(1, parameters.size)

            //test parameter
            val parameter = parameters[0]
            assertEquals("id", parameter.name())
            assertEquals(userInfoPsiClass.methods[1].parameterList.parameters[0], parameter.psi())
            assertEquals(userInfoExplicitClass, parameter.containClass())
            assertEquals(userInfoExplicitClass, parameter.defineClass())
            assertEquals("java.lang.Long", parameter.getType()?.canonicalText())
            assertEquals(idSetterExplicitMethod, parameter.containMethod())
            assertEquals("com.itangcent.model.UserInfo#setId.id", parameter.toString())
        }

        //test fields
        val fields = userInfoExplicitClass.fields()
        assertEquals(7, fields.size)
        run {
            val idExplicitField = fields[0]
            assertEquals("id", idExplicitField.name())
            assertEquals(userInfoPsiClass.fields[0], idExplicitField.psi())
            assertEquals(userInfoExplicitClass, idExplicitField.containClass())
            assertEquals(userInfoExplicitClass, idExplicitField.defineClass())
            assertEquals("java.lang.Long", idExplicitField.getType().canonicalText())
            assertEquals("com.itangcent.model.UserInfo#id", idExplicitField.toString())
        }
    }

    fun testExplicitUserInfoDetail() {
        val userInfoDetailExplicitClass = duckTypeHelper.explicit(userInfoDetailPsiClass)
        val userInfoExplicitClass = duckTypeHelper.explicit(userInfoPsiClass)
        assertEquals("UserInfoDetail", userInfoDetailExplicitClass.name())
        assertEquals(userInfoDetailPsiClass, userInfoDetailExplicitClass.psi())
        assertEquals(userInfoDetailExplicitClass, userInfoDetailExplicitClass.containClass())
        assertEquals(userInfoDetailExplicitClass, userInfoDetailExplicitClass.defineClass())
        assertEquals("com.itangcent.model.UserInfoDetail", userInfoDetailExplicitClass.toString())

        //test methods
        val methods = userInfoDetailExplicitClass.methods()
        assertEquals(16, methods.size)

        run {
            val idGetterExplicitMethod = methods[0]
            assertEquals("getId", idGetterExplicitMethod.name())
            assertEquals(userInfoPsiClass.methods[0], idGetterExplicitMethod.psi())
            assertEquals(userInfoDetailExplicitClass, idGetterExplicitMethod.containClass())
            assertEquals(userInfoExplicitClass, idGetterExplicitMethod.defineClass())
            assertEquals("java.lang.Long", idGetterExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.model.UserInfoDetail#getId", idGetterExplicitMethod.toString())

            val parameters = idGetterExplicitMethod.getParameters()
            assertTrue(parameters.isEmpty())
        }

        run {
            val levelGetterExplicitMethod = methods[14]
            assertEquals("getLevel", levelGetterExplicitMethod.name())
            assertEquals(userInfoDetailPsiClass.methods[0], levelGetterExplicitMethod.psi())
            assertEquals(userInfoDetailExplicitClass, levelGetterExplicitMethod.containClass())
            assertEquals(userInfoDetailExplicitClass, levelGetterExplicitMethod.defineClass())
            assertEquals("java.lang.Integer", levelGetterExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.model.UserInfoDetail#getLevel", levelGetterExplicitMethod.toString())

            val parameters = levelGetterExplicitMethod.getParameters()
            assertTrue(parameters.isEmpty())
        }

        run {
            val idSetterExplicitMethod = methods[1]
            assertEquals("setId", idSetterExplicitMethod.name())
            assertEquals(userInfoPsiClass.methods[1], idSetterExplicitMethod.psi())
            assertEquals(userInfoDetailExplicitClass, idSetterExplicitMethod.containClass())
            assertEquals("UserInfo", idSetterExplicitMethod.defineClass().name())
            assertEquals("void", idSetterExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.model.UserInfoDetail#setId", idSetterExplicitMethod.toString())

            val parameters = idSetterExplicitMethod.getParameters()
            assertEquals(1, parameters.size)

            //test parameter
            val explicitParameter = parameters[0]
            assertEquals("id", explicitParameter.name())
            assertEquals(userInfoPsiClass.methods[1].parameterList.parameters[0], explicitParameter.psi())
            assertEquals(userInfoDetailExplicitClass, explicitParameter.containClass())
            assertEquals(userInfoDetailExplicitClass, explicitParameter.defineClass())
            assertEquals("java.lang.Long", explicitParameter.getType()?.canonicalText())
            assertEquals(idSetterExplicitMethod, explicitParameter.containMethod())
            assertEquals("com.itangcent.model.UserInfoDetail#setId.id", explicitParameter.toString())
        }

        //test fields
        val fields = userInfoDetailExplicitClass.fields()
        assertEquals(8, fields.size)
        run {
            val idExplicitField = fields[0]
            assertEquals("id", idExplicitField.name())
            assertEquals(userInfoPsiClass.fields[0], idExplicitField.psi())
            assertEquals(userInfoDetailExplicitClass, idExplicitField.containClass())
            assertEquals(userInfoExplicitClass, idExplicitField.defineClass())
            assertEquals("java.lang.Long", idExplicitField.getType().canonicalText())
            assertEquals("com.itangcent.model.UserInfoDetail#id", idExplicitField.toString())
        }
        run {
            val idExplicitField = fields[7]
            assertEquals("level", idExplicitField.name())
            assertEquals(userInfoDetailPsiClass.fields[0], idExplicitField.psi())
            assertEquals(userInfoDetailExplicitClass, idExplicitField.containClass())
            assertEquals(userInfoDetailExplicitClass, idExplicitField.defineClass())
            assertEquals("java.lang.Integer", idExplicitField.getType().canonicalText())
            assertEquals("com.itangcent.model.UserInfoDetail#level", idExplicitField.toString())
        }
    }

    fun testExplicitUserLoginInfoPsiClass() {
        val userLoginInfoExplicitClass = duckTypeHelper.explicit(userLoginInfoPsiClass)
        val userInfoDetailExplicitClass = duckTypeHelper.explicit(userInfoDetailPsiClass)
        val userInfoExplicitClass = duckTypeHelper.explicit(userInfoPsiClass)
        assertEquals("UserLoginInfo", userLoginInfoExplicitClass.name())
        assertEquals(userLoginInfoPsiClass, userLoginInfoExplicitClass.psi())
        assertEquals(userLoginInfoExplicitClass, userLoginInfoExplicitClass.containClass())
        assertEquals(userLoginInfoExplicitClass, userLoginInfoExplicitClass.defineClass())
        assertEquals("com.itangcent.model.UserLoginInfo", userLoginInfoExplicitClass.toString())

        //test methods
        val methods = userLoginInfoExplicitClass.methods()
        assertEquals(18, methods.size)

        run {
            val idGetterExplicitMethod = methods[0]
            assertEquals("getId", idGetterExplicitMethod.name())
            assertEquals(userInfoPsiClass.methods[0], idGetterExplicitMethod.psi())
            assertEquals(userLoginInfoExplicitClass, idGetterExplicitMethod.containClass())
            assertEquals(userInfoExplicitClass, idGetterExplicitMethod.defineClass())
            assertEquals("java.lang.Long", idGetterExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.model.UserLoginInfo#getId", idGetterExplicitMethod.toString())

            val parameters = idGetterExplicitMethod.getParameters()
            assertTrue(parameters.isEmpty())
        }

        run {
            val idSetterExplicitMethod = methods[1]
            assertEquals("setId", idSetterExplicitMethod.name())
            assertEquals(userInfoPsiClass.methods[1], idSetterExplicitMethod.psi())
            assertEquals(userLoginInfoExplicitClass, idSetterExplicitMethod.containClass())
            assertEquals("UserInfo", idSetterExplicitMethod.defineClass().name())
            assertEquals("void", idSetterExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.model.UserLoginInfo#setId", idSetterExplicitMethod.toString())

            val parameters = idSetterExplicitMethod.getParameters()
            assertEquals(1, parameters.size)

            //test parameter
            val explicitParameter = parameters[0]
            assertEquals("id", explicitParameter.name())
            assertEquals(userInfoPsiClass.methods[1].parameterList.parameters[0], explicitParameter.psi())
            assertEquals(userLoginInfoExplicitClass, explicitParameter.containClass())
            assertEquals(userLoginInfoExplicitClass, explicitParameter.defineClass())
            assertEquals("java.lang.Long", explicitParameter.getType()?.canonicalText())
            assertEquals(idSetterExplicitMethod, explicitParameter.containMethod())
            assertEquals("com.itangcent.model.UserLoginInfo#setId.id", explicitParameter.toString())
        }

        run {
            val levelGetterExplicitMethod = methods[14]
            assertEquals("getLevel", levelGetterExplicitMethod.name())
            assertEquals(userInfoDetailPsiClass.methods[0], levelGetterExplicitMethod.psi())
            assertEquals(userLoginInfoExplicitClass, levelGetterExplicitMethod.containClass())
            assertEquals("UserInfoDetail", levelGetterExplicitMethod.defineClass().name())
            assertEquals("java.lang.Integer", levelGetterExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.model.UserLoginInfo#getLevel", levelGetterExplicitMethod.toString())

            val parameters = levelGetterExplicitMethod.getParameters()
            assertTrue(parameters.isEmpty())
        }

        run {
            val loginTimeGetterExplicitMethod = methods[16]
            assertEquals("getLoginTime", loginTimeGetterExplicitMethod.name())
            assertEquals(userLoginInfoPsiClass.methods[0], loginTimeGetterExplicitMethod.psi())
            assertEquals(userLoginInfoExplicitClass, loginTimeGetterExplicitMethod.containClass())
            assertEquals(userLoginInfoExplicitClass, loginTimeGetterExplicitMethod.defineClass())
            assertEquals("java.lang.Long", loginTimeGetterExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.model.UserLoginInfo#getLoginTime", loginTimeGetterExplicitMethod.toString())

            val parameters = loginTimeGetterExplicitMethod.getParameters()
            assertTrue(parameters.isEmpty())
        }

        //test fields
        val fields = userLoginInfoExplicitClass.fields()
        assertEquals(9, fields.size)
        run {
            val idExplicitField = fields[0]
            assertEquals("id", idExplicitField.name())
            assertEquals(userInfoPsiClass.fields[0], idExplicitField.psi())
            assertEquals(userLoginInfoExplicitClass, idExplicitField.containClass())
            assertEquals(userInfoExplicitClass, idExplicitField.defineClass())
            assertEquals("java.lang.Long", idExplicitField.getType().canonicalText())
            assertEquals("com.itangcent.model.UserLoginInfo#id", idExplicitField.toString())
        }

        run {
            val loginTimeExplicitField = fields[8]
            assertEquals("loginTime", loginTimeExplicitField.name())
            assertEquals(userLoginInfoPsiClass.fields[0], loginTimeExplicitField.psi())
            assertEquals(userLoginInfoExplicitClass, loginTimeExplicitField.containClass())
            assertEquals(userLoginInfoExplicitClass, loginTimeExplicitField.defineClass())
            assertEquals("java.lang.Long", loginTimeExplicitField.getType().canonicalText())
            assertEquals("com.itangcent.model.UserLoginInfo#loginTime", loginTimeExplicitField.toString())
        }

        run {
            val levelExplicitField = fields[7]
            assertEquals("level", levelExplicitField.name())
            assertEquals(userInfoDetailPsiClass.fields[0], levelExplicitField.psi())
            assertEquals(userLoginInfoExplicitClass, levelExplicitField.containClass())
            assertEquals("UserInfoDetail", levelExplicitField.defineClass().name())
            assertEquals("java.lang.Integer", levelExplicitField.getType().canonicalText())
            assertEquals("com.itangcent.model.UserLoginInfo#level", levelExplicitField.toString())
        }
    }

    fun testExplicitUserDetailCtrl() {
        val userDetailCtrlExplicitClass = duckTypeHelper.explicit(userDetailCtrlPsiClass)
        assertEquals("UserDetailCtrl", userDetailCtrlExplicitClass.name())
        assertEquals(userDetailCtrlPsiClass, userDetailCtrlExplicitClass.psi())
        assertEquals(userDetailCtrlExplicitClass, userDetailCtrlExplicitClass.containClass())
        assertEquals(userDetailCtrlExplicitClass, userDetailCtrlExplicitClass.defineClass())
        assertEquals("com.itangcent.api.UserDetailCtrl", userDetailCtrlExplicitClass.toString())

        //test methods
        val methods = userDetailCtrlExplicitClass.methods()
        assertEquals(2, methods.size)

        run {
            val resultExplicitMethod = methods[0]
            assertEquals("result", resultExplicitMethod.name())
            assertEquals(genericCtrlPsiClass.methods[0], resultExplicitMethod.psi())
            assertEquals(userDetailCtrlExplicitClass, resultExplicitMethod.containClass())
            assertEquals("GenericCtrl", resultExplicitMethod.defineClass().name())
            assertEquals(
                "com.itangcent.model.Result<com.itangcent.model.UserInfoDetail>",
                resultExplicitMethod.getReturnType()?.canonicalText()
            )
            assertEquals("com.itangcent.api.UserDetailCtrl#result", resultExplicitMethod.toString())

            val parameters = resultExplicitMethod.getParameters()
            assertEquals(1, parameters.size)

            //test parameter
            val explicitParameter = parameters[0]
            assertEquals("t", explicitParameter.name())
            assertEquals(genericCtrlPsiClass.methods[0].parameterList.parameters[0], explicitParameter.psi())
            assertEquals(userDetailCtrlExplicitClass, explicitParameter.containClass())
            assertEquals(userDetailCtrlExplicitClass, explicitParameter.defineClass())
            assertEquals("com.itangcent.model.UserInfoDetail", explicitParameter.getType()?.canonicalText())
            assertEquals(resultExplicitMethod, explicitParameter.containMethod())
            assertEquals("com.itangcent.api.UserDetailCtrl#result.t", explicitParameter.toString())
        }

        run {
            val greetingExplicitMethod = methods[1]
            assertEquals("greeting", greetingExplicitMethod.name())
            assertEquals(userDetailCtrlPsiClass.methods[0], greetingExplicitMethod.psi())
            assertEquals(userDetailCtrlExplicitClass, greetingExplicitMethod.containClass())
            assertEquals(userDetailCtrlExplicitClass, greetingExplicitMethod.defineClass())
            assertEquals("java.lang.String", greetingExplicitMethod.getReturnType()?.canonicalText())
            assertEquals("com.itangcent.api.UserDetailCtrl#greeting", greetingExplicitMethod.toString())

            val parameters = greetingExplicitMethod.getParameters()
            assertTrue(parameters.isEmpty())
        }
    }

    fun testExplicitUserInfoDetailResult() {
        val userInfoDetailResultExplicitClass = duckTypeHelper.explicit(userInfoDetailResultPsiClass)
        assertEquals("UserInfoDetailResult", userInfoDetailResultExplicitClass.name())
        assertEquals(userInfoDetailResultPsiClass, userInfoDetailResultExplicitClass.psi())
        assertEquals(userInfoDetailResultExplicitClass, userInfoDetailResultExplicitClass.containClass())
        assertEquals(userInfoDetailResultExplicitClass, userInfoDetailResultExplicitClass.defineClass())

        //test methods
        val methods = userInfoDetailResultExplicitClass.methods()
        assertEquals(13, methods.size)

        run {
            val dataGetterExplicitMethod = methods[9]
            assertEquals("getData", dataGetterExplicitMethod.name())
            assertEquals(resultPsiClass.methods[7], dataGetterExplicitMethod.psi())
            assertEquals(userInfoDetailResultExplicitClass, dataGetterExplicitMethod.containClass())
            assertEquals("Result", dataGetterExplicitMethod.defineClass().name())
            assertEquals(
                "com.itangcent.model.UserInfoDetail",
                dataGetterExplicitMethod.getReturnType()?.canonicalText()
            )

            val parameters = dataGetterExplicitMethod.getParameters()
            assertTrue(parameters.isEmpty())
        }

        run {
            val dataSetterExplicitMethod = methods[10]
            assertEquals("setData", dataSetterExplicitMethod.name())
            assertEquals(resultPsiClass.methods[8], dataSetterExplicitMethod.psi())
            assertEquals(userInfoDetailResultExplicitClass, dataSetterExplicitMethod.containClass())
            assertEquals("Result", dataSetterExplicitMethod.defineClass().name())
            assertEquals("void", dataSetterExplicitMethod.getReturnType()?.canonicalText())

            val parameters = dataSetterExplicitMethod.getParameters()
            assertEquals(1, parameters.size)

            //test parameter
            val parameter = parameters[0]
            assertEquals("data", parameter.name())
            assertEquals(resultPsiClass.methods[8].parameterList.parameters[0], parameter.psi())
            assertEquals(userInfoDetailResultExplicitClass, parameter.containClass())
            assertEquals(userInfoDetailResultExplicitClass, parameter.defineClass())
            assertEquals("com.itangcent.model.UserInfoDetail", parameter.getType()?.canonicalText())
            assertEquals(dataSetterExplicitMethod, parameter.containMethod())
        }

        run {
            val extends = userInfoDetailResultExplicitClass.extends()!!
            assertEquals(1, extends.size)
            assertEquals(resultPsiClass, extends[0].psi())

            val implements = userInfoDetailResultExplicitClass.implements()!!
            assertEquals(0, implements.size)
        }

        run {
            val resultExplicitClass = duckTypeHelper.explicit(resultPsiClass)
            val extends = resultExplicitClass.extends()!!
            assertEquals(0, extends.size)

            val implements = resultExplicitClass.implements()!!
            assertEquals(1, implements.size)
            assertEquals(iResultPsiClass, implements[0].psi())
        }
    }

    fun testExplicitPsiElement() {
        duckTypeHelper.explicit(userInfoPsiClass as PsiElement).let {
            assertTrue(it is ExplicitClass)
            assertEquals("com.itangcent.model.UserInfo", it.toString())
        }
        duckTypeHelper.explicit(userInfoPsiClass.methods[0]).let {
            assertTrue(it is ExplicitMethod)
            assertEquals("com.itangcent.model.UserInfo#getId", it.toString())
        }
        duckTypeHelper.explicit(userInfoPsiClass.fields[0]).let {
            assertTrue(it is ExplicitField)
            assertEquals("com.itangcent.model.UserInfo#id", it.toString())
        }
        assertNull(duckTypeHelper.explicit(userInfoPsiClass.methods[1].parameterList.parameters[0]))
    }
}