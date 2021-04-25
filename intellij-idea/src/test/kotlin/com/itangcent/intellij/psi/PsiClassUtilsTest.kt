package com.itangcent.intellij.psi

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.PsiTypesUtil
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import java.util.*

/**
 * Test case of [PsiClassUtils]
 */
internal class PsiClassUtilsTest : ContextLightCodeInsightFixtureTestCase() {

    private lateinit var objectPsiClass: PsiClass
    private lateinit var collectionPsiClass: PsiClass
    private lateinit var listPsiClass: PsiClass
    private lateinit var mapPsiClass: PsiClass
    private lateinit var hashMapPsiClass: PsiClass
    private lateinit var linkedListPsiClass: PsiClass
    private lateinit var modelPsiClass: PsiClass

    private lateinit var getStrPsiMethod: PsiMethod
    private lateinit var setStrPsiMethod: PsiMethod
    private lateinit var strPsiField: PsiField
    private lateinit var integerPsiField: PsiField
    private lateinit var strPsiParameter: PsiParameter

    override fun beforeBind() {
        super.beforeBind()
        objectPsiClass = loadSource(Object::class.java)!!
        collectionPsiClass = loadSource(java.util.Collection::class.java)!!
        mapPsiClass = loadSource(java.util.Map::class.java)!!
        listPsiClass = loadSource(java.util.List::class.java)!!
        hashMapPsiClass = loadSource(java.util.HashMap::class.java)!!
        linkedListPsiClass = loadSource(java.util.LinkedList::class.java)!!
        modelPsiClass = loadClass("model/Model.java")!!
        getStrPsiMethod = modelPsiClass.methods[0]
        setStrPsiMethod = modelPsiClass.methods[1]
        strPsiField = modelPsiClass.fields[0]
        integerPsiField = modelPsiClass.fields[1]
        strPsiParameter = setStrPsiMethod.parameterList.parameters[0]
    }

    fun testIsInterface() {
        assertFalse(PsiClassUtils.isInterface(PsiTypesUtil.getClassType(objectPsiClass)))
        assertTrue(PsiClassUtils.isInterface(PsiTypesUtil.getClassType(collectionPsiClass)))
        assertTrue(PsiClassUtils.isInterface(PsiTypesUtil.getClassType(mapPsiClass)))
        assertTrue(PsiClassUtils.isInterface(PsiTypesUtil.getClassType(listPsiClass)))
        assertFalse(PsiClassUtils.isInterface(PsiTypesUtil.getClassType(hashMapPsiClass)))
        assertFalse(PsiClassUtils.isInterface(PsiTypesUtil.getClassType(linkedListPsiClass)))
        assertFalse(PsiClassUtils.isInterface(PsiTypesUtil.getClassType(modelPsiClass)))
    }

    fun testHasImplement() {
        //true
        assertTrue(PsiClassUtils.hasImplement(objectPsiClass, objectPsiClass))
        assertTrue(PsiClassUtils.hasImplement(collectionPsiClass, objectPsiClass))
        assertTrue(PsiClassUtils.hasImplement(mapPsiClass, objectPsiClass))
        assertTrue(PsiClassUtils.hasImplement(listPsiClass, objectPsiClass))
        assertTrue(PsiClassUtils.hasImplement(listPsiClass, collectionPsiClass))
        assertTrue(PsiClassUtils.hasImplement(hashMapPsiClass, objectPsiClass))
        assertTrue(PsiClassUtils.hasImplement(hashMapPsiClass, mapPsiClass))
        assertTrue(PsiClassUtils.hasImplement(linkedListPsiClass, objectPsiClass))
        assertTrue(PsiClassUtils.hasImplement(linkedListPsiClass, collectionPsiClass))
        assertTrue(PsiClassUtils.hasImplement(linkedListPsiClass, listPsiClass))
        assertTrue(PsiClassUtils.hasImplement(modelPsiClass, objectPsiClass))

        //false
        assertFalse(PsiClassUtils.hasImplement(objectPsiClass, collectionPsiClass))
        assertFalse(PsiClassUtils.hasImplement(objectPsiClass, mapPsiClass))
        assertFalse(PsiClassUtils.hasImplement(objectPsiClass, listPsiClass))
        assertFalse(PsiClassUtils.hasImplement(collectionPsiClass, listPsiClass))
        assertFalse(PsiClassUtils.hasImplement(objectPsiClass, hashMapPsiClass))
        assertFalse(PsiClassUtils.hasImplement(mapPsiClass, hashMapPsiClass))
        assertFalse(PsiClassUtils.hasImplement(objectPsiClass, linkedListPsiClass))
        assertFalse(PsiClassUtils.hasImplement(collectionPsiClass, linkedListPsiClass))
        assertFalse(PsiClassUtils.hasImplement(listPsiClass, linkedListPsiClass))
        assertFalse(PsiClassUtils.hasImplement(objectPsiClass, modelPsiClass))
        assertFalse(PsiClassUtils.hasImplement(listPsiClass, mapPsiClass))
        assertFalse(PsiClassUtils.hasImplement(hashMapPsiClass, linkedListPsiClass))
    }

    fun testFullNameOfMethod() {
        assertEquals("com.itangcent.model.Model#getStr()", PsiClassUtils.fullNameOfMethod(getStrPsiMethod))
        assertEquals("java.util.List#getStr()", PsiClassUtils.fullNameOfMethod(listPsiClass, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#setStr(String)", PsiClassUtils.fullNameOfMethod(setStrPsiMethod))
        assertEquals("java.util.List#setStr(String)", PsiClassUtils.fullNameOfMethod(listPsiClass, setStrPsiMethod))
    }

    fun testFullNameOfField() {
        assertEquals("com.itangcent.model.Model#str", PsiClassUtils.fullNameOfField(strPsiField))
        assertEquals("java.util.List#str", PsiClassUtils.fullNameOfField(listPsiClass, strPsiField))

        assertEquals("com.itangcent.model.Model#integer", PsiClassUtils.fullNameOfField(integerPsiField))
        assertEquals("java.util.List#integer", PsiClassUtils.fullNameOfField(listPsiClass, integerPsiField))
    }

    fun testFindMethodFromFullName() {
        assertEquals(
            getStrPsiMethod,
            PsiClassUtils.findMethodFromFullName("com.itangcent.model.Model#getStr()", modelPsiClass)
        )
        assertNull(PsiClassUtils.findMethodFromFullName("java.util.List#getStr()", modelPsiClass))

        assertEquals(
            setStrPsiMethod,
            PsiClassUtils.findMethodFromFullName("com.itangcent.model.Model#setStr(String)", modelPsiClass)
        )
        assertNull(PsiClassUtils.findMethodFromFullName("java.util.List#setStr(String)", modelPsiClass))


    }

    fun testFindFieldFromFullName() {
        assertEquals(
            strPsiField,
            PsiClassUtils.findFieldFromFullName("com.itangcent.model.Model#str", modelPsiClass)
        )
        assertNull(PsiClassUtils.findFieldFromFullName("java.util.List#str", modelPsiClass))

        assertEquals(
            integerPsiField,
            PsiClassUtils.findFieldFromFullName("com.itangcent.model.Model#integer", modelPsiClass)
        )
        assertNull(PsiClassUtils.findFieldFromFullName("java.util.List#integer", modelPsiClass))
    }

    fun testFullNameOfMember() {
        assertEquals("com.itangcent.model.Model#getStr()", PsiClassUtils.fullNameOfMember(getStrPsiMethod))
        assertEquals("java.util.List#getStr()", PsiClassUtils.fullNameOfMember(listPsiClass, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#setStr(String)", PsiClassUtils.fullNameOfMember(setStrPsiMethod))
        assertEquals("java.util.List#setStr(String)", PsiClassUtils.fullNameOfMember(listPsiClass, setStrPsiMethod))

        assertEquals("com.itangcent.model.Model#str", PsiClassUtils.fullNameOfMember(strPsiField))
        assertEquals("java.util.List#str", PsiClassUtils.fullNameOfMember(listPsiClass, strPsiField))

        assertEquals("com.itangcent.model.Model#integer", PsiClassUtils.fullNameOfMember(integerPsiField))
        assertEquals("java.util.List#integer", PsiClassUtils.fullNameOfMember(listPsiClass, integerPsiField))

        assertEquals("str", PsiClassUtils.fullNameOfMember(strPsiParameter))
        assertEquals(
            "str",
            PsiClassUtils.fullNameOfMember(listPsiClass, strPsiParameter)
        )

    }

    fun testQualifiedNameOfMethod() {
        assertEquals("com.itangcent.model.Model#getStr", PsiClassUtils.qualifiedNameOfMethod(getStrPsiMethod))
        assertEquals("java.util.List#getStr", PsiClassUtils.qualifiedNameOfMethod(listPsiClass, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#setStr", PsiClassUtils.qualifiedNameOfMethod(setStrPsiMethod))
        assertEquals("java.util.List#setStr", PsiClassUtils.qualifiedNameOfMethod(listPsiClass, setStrPsiMethod))
    }

    fun testQualifiedNameOfField() {
        assertEquals("com.itangcent.model.Model#str", PsiClassUtils.qualifiedNameOfField(strPsiField))
        assertEquals("java.util.List#str", PsiClassUtils.qualifiedNameOfField(listPsiClass, strPsiField))

        assertEquals("com.itangcent.model.Model#integer", PsiClassUtils.qualifiedNameOfField(integerPsiField))
        assertEquals("java.util.List#integer", PsiClassUtils.qualifiedNameOfField(listPsiClass, integerPsiField))
    }

    fun testFindMethodFromQualifiedName() {
        assertEquals(
            getStrPsiMethod,
            PsiClassUtils.findMethodFromQualifiedName("com.itangcent.model.Model#getStr", modelPsiClass)
        )
        assertNull(PsiClassUtils.findFieldFromQualifiedName("java.util.List#getStr", modelPsiClass))

        assertEquals(
            setStrPsiMethod,
            PsiClassUtils.findMethodFromQualifiedName("com.itangcent.model.Model#setStr", modelPsiClass)
        )
        assertNull(PsiClassUtils.findFieldFromQualifiedName("java.util.List#setStr", modelPsiClass))
    }

    fun testFindFieldFromQualifiedName() {
        assertEquals(
            strPsiField,
            PsiClassUtils.findFieldFromQualifiedName("com.itangcent.model.Model#str", modelPsiClass)
        )
        assertNull(PsiClassUtils.findFieldFromQualifiedName("java.util.List#str", modelPsiClass))

        assertEquals(
            integerPsiField,
            PsiClassUtils.findFieldFromQualifiedName("com.itangcent.model.Model#integer", modelPsiClass)
        )
        assertNull(PsiClassUtils.findFieldFromQualifiedName("java.util.List#integer", modelPsiClass))
    }

    fun testQualifiedNameOfMember() {
        assertEquals("com.itangcent.model.Model#getStr", PsiClassUtils.qualifiedNameOfMember(getStrPsiMethod))
        assertEquals("java.util.List#getStr", PsiClassUtils.qualifiedNameOfMember(listPsiClass, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#setStr", PsiClassUtils.qualifiedNameOfMember(setStrPsiMethod))
        assertEquals("java.util.List#setStr", PsiClassUtils.qualifiedNameOfMember(listPsiClass, setStrPsiMethod))

        assertEquals("com.itangcent.model.Model#str", PsiClassUtils.qualifiedNameOfMember(strPsiField))
        assertEquals("java.util.List#str", PsiClassUtils.qualifiedNameOfMember(listPsiClass, strPsiField))

        assertEquals("com.itangcent.model.Model#integer", PsiClassUtils.qualifiedNameOfMember(integerPsiField))
        assertEquals("java.util.List#integer", PsiClassUtils.qualifiedNameOfMember(listPsiClass, integerPsiField))

        assertEquals("str", PsiClassUtils.qualifiedNameOfMember(strPsiParameter))
        assertEquals(
            "str", PsiClassUtils.qualifiedNameOfMember(listPsiClass, strPsiParameter)
        )
    }

    fun testNameOfMember() {
        assertEquals("getStr", PsiClassUtils.nameOfMember(getStrPsiMethod))
        assertEquals("setStr", PsiClassUtils.nameOfMember(setStrPsiMethod))
        assertEquals("str", PsiClassUtils.nameOfMember(strPsiField))
        assertEquals("integer", PsiClassUtils.nameOfMember(integerPsiField))
    }
}