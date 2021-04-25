package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTypesUtil
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import junit.framework.Assert
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Test case of [StandardJvmClassHelper]
 */
internal class StandardJvmClassHelperTest : ContextLightCodeInsightFixtureTestCase() {

    @Inject
    private lateinit var jvmClassHelper: JvmClassHelper

    internal lateinit var objectPsiClass: PsiClass
    internal lateinit var integerPsiClass: PsiClass
    internal lateinit var stringPsiClass: PsiClass
    internal lateinit var longPsiClass: PsiClass
    internal lateinit var collectionPsiClass: PsiClass
    internal lateinit var listPsiClass: PsiClass
    internal lateinit var mapPsiClass: PsiClass
    internal lateinit var hashMapPsiClass: PsiClass
    internal lateinit var modelPsiClass: PsiClass
    internal lateinit var resultPsiClass: PsiClass
    internal lateinit var iResultPsiClass: PsiClass
    internal lateinit var userCtrlPsiClass: PsiClass
    internal lateinit var commentDemoPsiClass: PsiClass
    internal lateinit var javaVersionPsiClass: PsiClass
    internal lateinit var numbersPsiClass: PsiClass

    override fun beforeBind() {
        super.beforeBind()
        objectPsiClass = loadSource(Object::class.java)!!
        integerPsiClass = loadSource(java.lang.Integer::class)!!
        stringPsiClass = loadSource(java.lang.String::class)!!
        longPsiClass = loadSource(java.lang.Long::class)!!
        collectionPsiClass = loadSource(Collection::class.java)!!
        listPsiClass = loadSource(List::class.java)!!
        mapPsiClass = loadSource(Map::class.java)!!
        hashMapPsiClass = loadSource(HashMap::class.java)!!
        loadSource(LocalDate::class)
        loadSource(LocalDateTime::class)
        modelPsiClass = loadClass("model/Model.java")!!
        iResultPsiClass = loadClass("model/IResult.java")!!
        resultPsiClass = loadClass("model/Result.java")!!
        loadFile("spring/RequestMapping.java")
        userCtrlPsiClass = loadClass("api/UserCtrl.java")!!
        commentDemoPsiClass = loadClass("model/CommentDemo.java")!!
        javaVersionPsiClass = loadClass("constant/JavaVersion.java")!!
        numbersPsiClass = loadClass("constant/Numbers.java")!!
    }

    override fun bind(builder: ActionContext.ActionContextBuilder) {
        super.bind(builder)
        builder.bind(JvmClassHelper::class.java) { it.with(StandardJvmClassHelper::class) }
    }

    fun testIsInheritor() {
        assertTrue(jvmClassHelper.isInheritor(collectionPsiClass, "java.util.Collection"))
        assertTrue(jvmClassHelper.isInheritor(listPsiClass, "java.util.Collection"))
        assertFalse(jvmClassHelper.isInheritor(collectionPsiClass, "java.util.List"))
        assertTrue(jvmClassHelper.isInheritor(resultPsiClass, "com.itangcent.model.IResult"))
        assertFalse(jvmClassHelper.isInheritor(iResultPsiClass, "com.itangcent.model.Result"))
    }

    fun testIsCollection() {
        assertFalse(jvmClassHelper.isCollection(objectPsiClass))
        assertFalse(jvmClassHelper.isCollection(integerPsiClass))
        assertFalse(jvmClassHelper.isCollection(stringPsiClass))
        assertFalse(jvmClassHelper.isCollection(longPsiClass))
        assertFalse(jvmClassHelper.isCollection(mapPsiClass))
        assertFalse(jvmClassHelper.isCollection(hashMapPsiClass))
        assertTrue(jvmClassHelper.isCollection(collectionPsiClass))
        assertTrue(jvmClassHelper.isCollection(listPsiClass))
        assertFalse(jvmClassHelper.isCollection(iResultPsiClass))
        assertFalse(jvmClassHelper.isCollection(resultPsiClass))
        assertFalse(jvmClassHelper.isCollection(modelPsiClass))
        assertFalse(jvmClassHelper.isCollection(javaVersionPsiClass))
        assertFalse(jvmClassHelper.isCollection(numbersPsiClass))
    }

    fun testIsMap() {
        assertFalse(jvmClassHelper.isMap(objectPsiClass))
        assertFalse(jvmClassHelper.isMap(integerPsiClass))
        assertFalse(jvmClassHelper.isMap(stringPsiClass))
        assertFalse(jvmClassHelper.isMap(longPsiClass))
        assertTrue(jvmClassHelper.isMap(mapPsiClass))
        assertTrue(jvmClassHelper.isMap(hashMapPsiClass))
        assertFalse(jvmClassHelper.isMap(collectionPsiClass))
        assertFalse(jvmClassHelper.isMap(listPsiClass))
        assertFalse(jvmClassHelper.isMap(iResultPsiClass))
        assertFalse(jvmClassHelper.isMap(resultPsiClass))
        assertFalse(jvmClassHelper.isMap(modelPsiClass))
        assertFalse(jvmClassHelper.isMap(javaVersionPsiClass))
        assertFalse(jvmClassHelper.isMap(numbersPsiClass))
    }

    fun testIsString() {
        assertFalse(jvmClassHelper.isString(objectPsiClass))
        assertFalse(jvmClassHelper.isString(integerPsiClass))
        assertTrue(jvmClassHelper.isString(stringPsiClass))
        assertFalse(jvmClassHelper.isString(longPsiClass))
        assertFalse(jvmClassHelper.isString(mapPsiClass))
        assertFalse(jvmClassHelper.isString(hashMapPsiClass))
        assertFalse(jvmClassHelper.isString(collectionPsiClass))
        assertFalse(jvmClassHelper.isString(listPsiClass))
        assertFalse(jvmClassHelper.isString(iResultPsiClass))
        assertFalse(jvmClassHelper.isString(resultPsiClass))
        assertFalse(jvmClassHelper.isString(modelPsiClass))
        assertFalse(jvmClassHelper.isString(javaVersionPsiClass))
        assertFalse(jvmClassHelper.isString(numbersPsiClass))
    }

    fun testIsEnum() {
        assertFalse(jvmClassHelper.isEnum(objectPsiClass))
        assertFalse(jvmClassHelper.isEnum(integerPsiClass))
        assertFalse(jvmClassHelper.isEnum(stringPsiClass))
        assertFalse(jvmClassHelper.isEnum(longPsiClass))
        assertFalse(jvmClassHelper.isEnum(mapPsiClass))
        assertFalse(jvmClassHelper.isEnum(hashMapPsiClass))
        assertFalse(jvmClassHelper.isEnum(collectionPsiClass))
        assertFalse(jvmClassHelper.isEnum(listPsiClass))
        assertFalse(jvmClassHelper.isEnum(iResultPsiClass))
        assertFalse(jvmClassHelper.isEnum(resultPsiClass))
        assertFalse(jvmClassHelper.isEnum(modelPsiClass))
        assertTrue(jvmClassHelper.isEnum(javaVersionPsiClass))
        assertFalse(jvmClassHelper.isEnum(numbersPsiClass))
    }

    fun testIsInterface() {
        assertFalse(jvmClassHelper.isInterface(objectPsiClass))
        assertFalse(jvmClassHelper.isInterface(integerPsiClass))
        assertFalse(jvmClassHelper.isInterface(stringPsiClass))
        assertFalse(jvmClassHelper.isInterface(longPsiClass))
        assertTrue(jvmClassHelper.isInterface(mapPsiClass))
        assertFalse(jvmClassHelper.isInterface(hashMapPsiClass))
        assertTrue(jvmClassHelper.isInterface(collectionPsiClass))
        assertTrue(jvmClassHelper.isInterface(listPsiClass))
        assertTrue(jvmClassHelper.isInterface(iResultPsiClass))
        assertFalse(jvmClassHelper.isInterface(resultPsiClass))
        assertFalse(jvmClassHelper.isInterface(modelPsiClass))
        assertFalse(jvmClassHelper.isInterface(javaVersionPsiClass))
        assertTrue(jvmClassHelper.isInterface(numbersPsiClass))
    }

    fun testIsStaticFinal() {
        assertTrue(jvmClassHelper.isStaticFinal(numbersPsiClass.fields[0]))
        assertTrue(jvmClassHelper.isStaticFinal(numbersPsiClass.fields[1]))
        assertFalse(jvmClassHelper.isStaticFinal(modelPsiClass.fields[1]))
    }

    fun testIsPublicStaticFinal() {
        assertTrue(jvmClassHelper.isPublicStaticFinal(numbersPsiClass.fields[0]))
        assertTrue(jvmClassHelper.isPublicStaticFinal(numbersPsiClass.fields[1]))
        assertFalse(jvmClassHelper.isPublicStaticFinal(modelPsiClass.fields[1]))
    }

    fun testIsAccessibleField() {
        assertTrue(jvmClassHelper.isAccessibleField(numbersPsiClass.fields[0]))
        assertTrue(jvmClassHelper.isAccessibleField(numbersPsiClass.fields[1]))
        assertFalse(jvmClassHelper.isAccessibleField(modelPsiClass.fields[1]))
    }

    fun testIsNormalType() {
        assertTrue(jvmClassHelper.isNormalType(objectPsiClass.qualifiedName!!))
        assertTrue(jvmClassHelper.isNormalType(integerPsiClass.qualifiedName!!))
        assertTrue(jvmClassHelper.isNormalType(stringPsiClass.qualifiedName!!))
        assertTrue(jvmClassHelper.isNormalType(longPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isNormalType(mapPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isNormalType(hashMapPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isNormalType(collectionPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isNormalType(listPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isNormalType(iResultPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isNormalType(resultPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isNormalType(modelPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isNormalType(javaVersionPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isNormalType(numbersPsiClass.qualifiedName!!))
    }

    fun testIsPrimitive() {
        assertTrue(jvmClassHelper.isPrimitive("int"))
        assertTrue(jvmClassHelper.isPrimitive("float"))
        assertTrue(jvmClassHelper.isPrimitive("long"))
        assertTrue(jvmClassHelper.isPrimitive("short"))
        assertTrue(jvmClassHelper.isPrimitive("boolean"))
        assertTrue(jvmClassHelper.isPrimitive("double"))
        assertTrue(jvmClassHelper.isPrimitive("byte"))
        assertFalse(jvmClassHelper.isPrimitive(objectPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(integerPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(stringPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(longPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(mapPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(hashMapPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(collectionPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(listPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(iResultPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(resultPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(modelPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(javaVersionPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitive(numbersPsiClass.qualifiedName!!))
    }

    fun testIsPrimitiveWrapper() {
        assertFalse(jvmClassHelper.isPrimitiveWrapper("int"))
        assertFalse(jvmClassHelper.isPrimitiveWrapper("float"))
        assertFalse(jvmClassHelper.isPrimitiveWrapper("long"))
        assertFalse(jvmClassHelper.isPrimitiveWrapper("short"))
        assertFalse(jvmClassHelper.isPrimitiveWrapper("boolean"))
        assertFalse(jvmClassHelper.isPrimitiveWrapper("double"))
        assertFalse(jvmClassHelper.isPrimitiveWrapper("byte"))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(objectPsiClass.qualifiedName!!))
        assertTrue(jvmClassHelper.isPrimitiveWrapper(integerPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(stringPsiClass.qualifiedName!!))
        assertTrue(jvmClassHelper.isPrimitiveWrapper(longPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(mapPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(hashMapPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(collectionPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(listPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(iResultPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(resultPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(modelPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(javaVersionPsiClass.qualifiedName!!))
        assertFalse(jvmClassHelper.isPrimitiveWrapper(numbersPsiClass.qualifiedName!!))
    }

    fun testGetDefaultValue() {
        assertEquals(emptyMap<Any, Any>(), jvmClassHelper.getDefaultValue(objectPsiClass.qualifiedName!!))
        assertEquals(0, jvmClassHelper.getDefaultValue(integerPsiClass.qualifiedName!!))
        assertEquals("", jvmClassHelper.getDefaultValue(stringPsiClass.qualifiedName!!))
        assertEquals(0L, jvmClassHelper.getDefaultValue(longPsiClass.qualifiedName!!))
        assertEquals(null, jvmClassHelper.getDefaultValue(mapPsiClass.qualifiedName!!))
        assertEquals(null, jvmClassHelper.getDefaultValue(hashMapPsiClass.qualifiedName!!))
        assertEquals(null, jvmClassHelper.getDefaultValue(collectionPsiClass.qualifiedName!!))
        assertEquals(null, jvmClassHelper.getDefaultValue(listPsiClass.qualifiedName!!))
        assertEquals(null, jvmClassHelper.getDefaultValue(iResultPsiClass.qualifiedName!!))
        assertEquals(null, jvmClassHelper.getDefaultValue(resultPsiClass.qualifiedName!!))
        assertEquals(null, jvmClassHelper.getDefaultValue(modelPsiClass.qualifiedName!!))
        assertEquals(null, jvmClassHelper.getDefaultValue(javaVersionPsiClass.qualifiedName!!))
        assertEquals(null, jvmClassHelper.getDefaultValue(numbersPsiClass.qualifiedName!!))
    }

    fun testIsBasicMethod() {
        objectPsiClass.methods.forEach {
            assertTrue(jvmClassHelper.isBasicMethod(it.name))
        }
    }

    fun testResolveClassInType() {
        for (psiClass in arrayOf(
            objectPsiClass, integerPsiClass, stringPsiClass, longPsiClass, mapPsiClass,
            hashMapPsiClass, collectionPsiClass, listPsiClass, iResultPsiClass, resultPsiClass,
            modelPsiClass, javaVersionPsiClass, numbersPsiClass
        )) {
            Assert.assertEquals(
                psiClass.qualifiedName,
                jvmClassHelper.resolveClassInType(PsiTypesUtil.getClassType(psiClass))!!.qualifiedName
            )
        }
    }

    fun testResolveClassToType() {
        for (psiClass in arrayOf(
            objectPsiClass, integerPsiClass, stringPsiClass, longPsiClass, mapPsiClass,
            hashMapPsiClass, collectionPsiClass, listPsiClass, iResultPsiClass, resultPsiClass,
            modelPsiClass, javaVersionPsiClass, numbersPsiClass
        )) {
            Assert.assertEquals(psiClass.qualifiedName, jvmClassHelper.resolveClassToType(psiClass)!!.canonicalText)
        }
    }

    fun testGetAllFields() {
        assertEquals(4, jvmClassHelper.getAllFields(numbersPsiClass).size)
        assertEquals(4, jvmClassHelper.getAllFields(modelPsiClass).size)
        assertEquals(17, jvmClassHelper.getAllFields(javaVersionPsiClass).size)
    }

    fun testGetAllMethods() {
        assertEquals(12, jvmClassHelper.getAllMethods(numbersPsiClass).size)
        assertEquals(22, jvmClassHelper.getAllMethods(modelPsiClass).size)
        assertEquals(7, jvmClassHelper.getAllMethods(javaVersionPsiClass).size)
    }

    fun testGetMethods() {
        assertEquals(0, jvmClassHelper.getMethods(numbersPsiClass).size)
        assertEquals(10, jvmClassHelper.getMethods(modelPsiClass).size)
        assertEquals(7, jvmClassHelper.getMethods(javaVersionPsiClass).size)
    }

    fun testGetFields() {
        assertEquals(4, jvmClassHelper.getFields(numbersPsiClass).size)
        assertEquals(4, jvmClassHelper.getFields(modelPsiClass).size)
        assertEquals(17, jvmClassHelper.getFields(javaVersionPsiClass).size)
    }

    fun testExtractModifiers() {
        assertEquals(listOf("public", "abstract"), jvmClassHelper.extractModifiers(numbersPsiClass))
        assertEquals(listOf("public", "static", "final"), jvmClassHelper.extractModifiers(numbersPsiClass.fields[0]))
        assertEquals(listOf("public", "static", "final"), jvmClassHelper.extractModifiers(numbersPsiClass.fields[1]))
        assertEquals(listOf<String>(), jvmClassHelper.extractModifiers(modelPsiClass))
        assertEquals(listOf("private"), jvmClassHelper.extractModifiers(modelPsiClass.fields[0]))
        assertEquals(listOf("public"), jvmClassHelper.extractModifiers(modelPsiClass.methods[0]))
    }

    fun testDefineClassCode() {
        assertEquals("public class Object;", jvmClassHelper.defineClassCode(objectPsiClass))
        assertEquals(
            "public final class Integer extends Number implements Comparable<Integer>;",
            jvmClassHelper.defineClassCode(integerPsiClass)
        )
        assertEquals(
            "public final class String implements java.io.Serializable, Comparable<String>, CharSequence;",
            jvmClassHelper.defineClassCode(stringPsiClass)
        )
        assertEquals(
            "public final class Long extends Number implements Comparable<Long>;",
            jvmClassHelper.defineClassCode(longPsiClass)
        )
        assertEquals("public interface Map;", jvmClassHelper.defineClassCode(mapPsiClass))
        assertEquals(
            "public class HashMap extends AbstractMap<K,V> implements java.util.Map<K,V>, Cloneable, Serializable;",
            jvmClassHelper.defineClassCode(hashMapPsiClass)
        )
        assertEquals(
            "public interface Collection extends Iterable<E>;",
            jvmClassHelper.defineClassCode(collectionPsiClass)
        )
        assertEquals(
            "public interface List extends java.util.Collection<E>;",
            jvmClassHelper.defineClassCode(listPsiClass)
        )
        assertEquals("public interface IResult;", jvmClassHelper.defineClassCode(iResultPsiClass))
        assertEquals(
            "public class Result implements com.itangcent.model.IResult;",
            jvmClassHelper.defineClassCode(resultPsiClass)
        )
        assertEquals("class Model;", jvmClassHelper.defineClassCode(modelPsiClass))
        assertEquals(
            "public final enum JavaVersion extends java.lang.Enum;",
            jvmClassHelper.defineClassCode(javaVersionPsiClass)
        )
        assertEquals("public interface Numbers;", jvmClassHelper.defineClassCode(numbersPsiClass))
    }

    fun testDefineMethodCode() {
        assertEquals("public int size();", jvmClassHelper.defineMethodCode(mapPsiClass.methods[0]))
        assertEquals(
            "static final int hash(java.lang.Object key);",
            jvmClassHelper.defineMethodCode(hashMapPsiClass.methods[0])
        )
        assertEquals("public int size();", jvmClassHelper.defineMethodCode(collectionPsiClass.methods[0]))
        assertEquals("public int size();", jvmClassHelper.defineMethodCode(listPsiClass.methods[0]))
        assertEquals(
            "public java.lang.Integer getCode();",
            jvmClassHelper.defineMethodCode(iResultPsiClass.methods[0])
        )
        assertEquals("public Result();", jvmClassHelper.defineMethodCode(resultPsiClass.methods[0]))
        assertEquals("public java.lang.String getStr();", jvmClassHelper.defineMethodCode(modelPsiClass.methods[0]))
        assertEquals(
            "private JavaVersion(float value, java.lang.String name);",
            jvmClassHelper.defineMethodCode(javaVersionPsiClass.methods[0])
        )
    }

    fun testDefineFieldCode() {
        assertEquals(
            "private static final long serialVersionUID;",
            jvmClassHelper.defineFieldCode(hashMapPsiClass.fields[0])
        )
        assertEquals("private java.lang.Integer code;", jvmClassHelper.defineFieldCode(resultPsiClass.fields[0]))
        assertEquals("private java.lang.String str;", jvmClassHelper.defineFieldCode(modelPsiClass.fields[0]))
        assertEquals(
            "public static final JAVA_0_9(1.5f, \"0.9\");",
            jvmClassHelper.defineFieldCode(javaVersionPsiClass.fields[0])
        )
    }

    fun testDefineParamCode() {
        assertEquals(
            "java.lang.Integer code",
            jvmClassHelper.defineParamCode(resultPsiClass.methods[1].parameterList.parameters[0])
        )
    }

    fun testDefineOtherCode() {
        assertEquals(
            "public Result() {\n" +
                    "    }", jvmClassHelper.defineOtherCode(resultPsiClass.methods[0])
        )
        assertEquals(
            "private Integer code;//The response code",
            jvmClassHelper.defineOtherCode(resultPsiClass.fields[0])
        )
        assertEquals(
            "Integer code",
            jvmClassHelper.defineOtherCode(resultPsiClass.methods[1].parameterList.parameters[0])
        )
    }
}