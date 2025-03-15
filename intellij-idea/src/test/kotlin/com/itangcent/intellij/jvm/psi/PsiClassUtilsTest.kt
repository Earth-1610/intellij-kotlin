package com.itangcent.intellij.jvm.psi

import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import org.mockito.Mockito
import org.mockito.kotlin.mock

/**
 * Test case of [com.itangcent.intellij.jvm.psi.PsiClassUtil]
 */
internal class PsiClassUtilsTest : ContextLightCodeInsightFixtureTestCase() {

    private lateinit var objectPsiClass: PsiClass
    private lateinit var collectionPsiClass: PsiClass
    private lateinit var listPsiClass: PsiClass
    private lateinit var mapPsiClass: PsiClass
    private lateinit var hashMapPsiClass: PsiClass
    private lateinit var linkedListPsiClass: PsiClass
    private lateinit var modelPsiClass: PsiClass
    private lateinit var hugeModelPsiClass: PsiClass

    private lateinit var getStrPsiMethod: PsiMethod
    private lateinit var setStrPsiMethod: PsiMethod
    private lateinit var methodAPsiMethod: PsiMethod
    private lateinit var strPsiField: PsiField
    private lateinit var integerPsiField: PsiField
    private lateinit var strPsiParameter: PsiParameter
    private lateinit var commentDemoPsiFile: PsiFile

    override fun beforeBind() {
        super.beforeBind()
        objectPsiClass = loadSource(Object::class.java)!!
        collectionPsiClass = loadSource(java.util.Collection::class.java)!!
        mapPsiClass = loadSource(java.util.Map::class.java)!!
        listPsiClass = loadSource(java.util.List::class.java)!!
        hashMapPsiClass = loadSource(java.util.HashMap::class.java)!!
        linkedListPsiClass = loadSource(java.util.LinkedList::class.java)!!
        modelPsiClass = loadClass("model/Model.java")!!
        hugeModelPsiClass = loadClass("model/HugeModel.java")!!
        getStrPsiMethod = modelPsiClass.methods[0]
        setStrPsiMethod = modelPsiClass.methods[1]
        methodAPsiMethod = hugeModelPsiClass.methods.find { it.name == "methodA" }!!
        strPsiField = modelPsiClass.fields[0]
        integerPsiField = modelPsiClass.fields[1]
        strPsiParameter = setStrPsiMethod.parameterList.parameters[0]
        commentDemoPsiFile = loadFile("model/CommentDemo.java")!!
    }

    fun testIsInterface() {
        assertFalse(PsiClassUtil.isInterface(PsiTypesUtil.getClassType(objectPsiClass)))
        assertTrue(PsiClassUtil.isInterface(PsiTypesUtil.getClassType(collectionPsiClass)))
        assertTrue(PsiClassUtil.isInterface(PsiTypesUtil.getClassType(mapPsiClass)))
        assertTrue(PsiClassUtil.isInterface(PsiTypesUtil.getClassType(listPsiClass)))
        assertFalse(PsiClassUtil.isInterface(PsiTypesUtil.getClassType(hashMapPsiClass)))
        assertFalse(PsiClassUtil.isInterface(PsiTypesUtil.getClassType(linkedListPsiClass)))
        assertFalse(PsiClassUtil.isInterface(PsiTypesUtil.getClassType(modelPsiClass)))

        // Mock a PsiClass with qualifiedName "java.lang.Object"
        val mockPsiType = mock<PsiType>()

        // Check that objectPsiClass has implement the mock class
        assertFalse(PsiClassUtil.isInterface(mockPsiType))
    }

    fun testHasImplement() {
        //null
        assertFalse(PsiClassUtil.hasImplement(null, null))
        assertFalse(PsiClassUtil.hasImplement(objectPsiClass, null))
        assertFalse(PsiClassUtil.hasImplement(null, objectPsiClass))

        // Mock a PsiClass with qualifiedName "java.lang.Object"
        val mockObjectClass = mock<PsiClass>(
            extraInterfaces = arrayOf(com.intellij.psi.PsiAnonymousClass::class)
        ) {
            Mockito.`when`(it.qualifiedName).thenReturn("java.lang.Object")
        }

        // Check that objectPsiClass has implement the mock class
        assertTrue(PsiClassUtil.hasImplement(objectPsiClass, mockObjectClass))

        //true
        assertTrue(PsiClassUtil.hasImplement(objectPsiClass, objectPsiClass))
        assertTrue(PsiClassUtil.hasImplement(collectionPsiClass, objectPsiClass))
        assertTrue(PsiClassUtil.hasImplement(mapPsiClass, objectPsiClass))
        assertTrue(PsiClassUtil.hasImplement(listPsiClass, objectPsiClass))
        assertTrue(PsiClassUtil.hasImplement(listPsiClass, collectionPsiClass))
        assertTrue(PsiClassUtil.hasImplement(hashMapPsiClass, objectPsiClass))
        assertTrue(PsiClassUtil.hasImplement(hashMapPsiClass, mapPsiClass))
        assertTrue(PsiClassUtil.hasImplement(linkedListPsiClass, objectPsiClass))
        assertTrue(PsiClassUtil.hasImplement(linkedListPsiClass, collectionPsiClass))
        assertTrue(PsiClassUtil.hasImplement(linkedListPsiClass, listPsiClass))
        assertTrue(PsiClassUtil.hasImplement(modelPsiClass, objectPsiClass))

        //false
        assertFalse(PsiClassUtil.hasImplement(objectPsiClass, collectionPsiClass))
        assertFalse(PsiClassUtil.hasImplement(objectPsiClass, mapPsiClass))
        assertFalse(PsiClassUtil.hasImplement(objectPsiClass, listPsiClass))
        assertFalse(PsiClassUtil.hasImplement(collectionPsiClass, listPsiClass))
        assertFalse(PsiClassUtil.hasImplement(objectPsiClass, hashMapPsiClass))
        assertFalse(PsiClassUtil.hasImplement(mapPsiClass, hashMapPsiClass))
        assertFalse(PsiClassUtil.hasImplement(objectPsiClass, linkedListPsiClass))
        assertFalse(PsiClassUtil.hasImplement(collectionPsiClass, linkedListPsiClass))
        assertFalse(PsiClassUtil.hasImplement(listPsiClass, linkedListPsiClass))
        assertFalse(PsiClassUtil.hasImplement(objectPsiClass, modelPsiClass))
        assertFalse(PsiClassUtil.hasImplement(listPsiClass, mapPsiClass))
        assertFalse(PsiClassUtil.hasImplement(hashMapPsiClass, linkedListPsiClass))
    }

    fun testFullNameOfMethod() {
        assertEquals("getStr()", PsiClassUtil.fullNameOfMethod(null, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#getStr()", PsiClassUtil.fullNameOfMethod(getStrPsiMethod))
        assertEquals("java.util.List#getStr()", PsiClassUtil.fullNameOfMethod(listPsiClass, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#setStr(String)", PsiClassUtil.fullNameOfMethod(setStrPsiMethod))
        assertEquals("java.util.List#setStr(String)", PsiClassUtil.fullNameOfMethod(listPsiClass, setStrPsiMethod))

        // Test method with multiple parameters
        assertEquals("com.itangcent.model.HugeModel#methodA(int,int)", PsiClassUtil.fullNameOfMethod(methodAPsiMethod))
        assertEquals("java.util.List#methodA(int,int)", PsiClassUtil.fullNameOfMethod(listPsiClass, methodAPsiMethod))
    }

    fun testFullNameOfField() {
        assertEquals("str", PsiClassUtil.fullNameOfField(null, strPsiField))

        assertEquals("com.itangcent.model.Model#str", PsiClassUtil.fullNameOfField(strPsiField))
        assertEquals("java.util.List#str", PsiClassUtil.fullNameOfField(listPsiClass, strPsiField))

        assertEquals("com.itangcent.model.Model#integer", PsiClassUtil.fullNameOfField(integerPsiField))
        assertEquals("java.util.List#integer", PsiClassUtil.fullNameOfField(listPsiClass, integerPsiField))
    }

    fun testFindMethodFromFullName() {
        assertNull(PsiClassUtil.findMethodFromFullName("java.util.Foo", modelPsiClass))

        assertEquals(
            getStrPsiMethod,
            PsiClassUtil.findMethodFromFullName("com.itangcent.model.Model#getStr()", modelPsiClass)
        )
        assertNull(PsiClassUtil.findMethodFromFullName("java.util.List#getStr()", modelPsiClass))

        assertEquals(
            setStrPsiMethod,
            PsiClassUtil.findMethodFromFullName("com.itangcent.model.Model#setStr(String)", modelPsiClass)
        )
        assertNull(PsiClassUtil.findMethodFromFullName("java.util.List#setStr(String)", modelPsiClass))

        // Test cases for Map's remove methods
        val removeMethod = mapPsiClass.findMethodsByName("remove", false)
            .first { it.parameterList.parametersCount == 1 }
        assertEquals(
            removeMethod,
            PsiClassUtil.findMethodFromFullName("java.util.Map#remove(java.lang.Object)", mapPsiClass)
        )

        val removeWithValueMethod = mapPsiClass.findMethodsByName("remove", false)
            .first { it.parameterList.parametersCount == 2 }
        assertEquals(
            removeWithValueMethod,
            PsiClassUtil.findMethodFromFullName("java.util.Map#remove(java.lang.Object,java.lang.Object)", mapPsiClass)
        )
    }

    fun testFindFieldFromFullName() {
        assertEquals(
            strPsiField,
            PsiClassUtil.findFieldFromFullName("com.itangcent.model.Model#str", modelPsiClass)
        )
        assertNull(PsiClassUtil.findFieldFromFullName("java.util.List#str", modelPsiClass))

        assertEquals(
            integerPsiField,
            PsiClassUtil.findFieldFromFullName("com.itangcent.model.Model#integer", modelPsiClass)
        )
        assertNull(PsiClassUtil.findFieldFromFullName("java.util.List#integer", modelPsiClass))
    }

    fun testFullNameOfMember() {
        assertEquals("com.itangcent.model.Model#getStr()", PsiClassUtil.fullNameOfMember(getStrPsiMethod))
        assertEquals("java.util.List#getStr()", PsiClassUtil.fullNameOfMember(listPsiClass, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#setStr(String)", PsiClassUtil.fullNameOfMember(setStrPsiMethod))
        assertEquals("java.util.List#setStr(String)", PsiClassUtil.fullNameOfMember(listPsiClass, setStrPsiMethod))

        assertEquals("com.itangcent.model.Model#str", PsiClassUtil.fullNameOfMember(strPsiField))
        assertEquals("java.util.List#str", PsiClassUtil.fullNameOfMember(listPsiClass, strPsiField))

        assertEquals("com.itangcent.model.Model#integer", PsiClassUtil.fullNameOfMember(integerPsiField))
        assertEquals("java.util.List#integer", PsiClassUtil.fullNameOfMember(listPsiClass, integerPsiField))

        assertEquals("str", PsiClassUtil.fullNameOfMember(strPsiParameter))
        assertEquals(
            "str",
            PsiClassUtil.fullNameOfMember(listPsiClass, strPsiParameter)
        )

    }

    fun testQualifiedNameOfMethod() {
        assertEquals("getStr", PsiClassUtil.qualifiedNameOfMethod(null, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#getStr", PsiClassUtil.qualifiedNameOfMethod(getStrPsiMethod))
        assertEquals("java.util.List#getStr", PsiClassUtil.qualifiedNameOfMethod(listPsiClass, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#setStr", PsiClassUtil.qualifiedNameOfMethod(setStrPsiMethod))
        assertEquals("java.util.List#setStr", PsiClassUtil.qualifiedNameOfMethod(listPsiClass, setStrPsiMethod))
    }

    fun testQualifiedNameOfField() {
        assertEquals("str", PsiClassUtil.qualifiedNameOfField(null, strPsiField))
        assertEquals("com.itangcent.model.Model#str", PsiClassUtil.qualifiedNameOfField(strPsiField))
        assertEquals("java.util.List#str", PsiClassUtil.qualifiedNameOfField(listPsiClass, strPsiField))

        assertEquals("com.itangcent.model.Model#integer", PsiClassUtil.qualifiedNameOfField(integerPsiField))
        assertEquals("java.util.List#integer", PsiClassUtil.qualifiedNameOfField(listPsiClass, integerPsiField))
    }

    fun testFindMethodFromQualifiedName() {
        assertEquals(
            getStrPsiMethod,
            PsiClassUtil.findMethodFromQualifiedName("com.itangcent.model.Model#getStr", modelPsiClass)
        )
        assertNull(PsiClassUtil.findFieldFromQualifiedName("java.util.List#getStr", modelPsiClass))

        assertEquals(
            setStrPsiMethod,
            PsiClassUtil.findMethodFromQualifiedName("com.itangcent.model.Model#setStr", modelPsiClass)
        )
        assertNull(PsiClassUtil.findFieldFromQualifiedName("java.util.List#setStr", modelPsiClass))
    }

    fun testFindFieldFromQualifiedName() {
        assertEquals(
            strPsiField,
            PsiClassUtil.findFieldFromQualifiedName("com.itangcent.model.Model#str", modelPsiClass)
        )
        assertNull(PsiClassUtil.findFieldFromQualifiedName("java.util.List#str", modelPsiClass))

        assertEquals(
            integerPsiField,
            PsiClassUtil.findFieldFromQualifiedName("com.itangcent.model.Model#integer", modelPsiClass)
        )
        assertNull(PsiClassUtil.findFieldFromQualifiedName("java.util.List#integer", modelPsiClass))
    }

    fun testQualifiedNameOfMember() {
        assertEquals("com.itangcent.model.Model#getStr", PsiClassUtil.qualifiedNameOfMember(getStrPsiMethod))
        assertEquals("java.util.List#getStr", PsiClassUtil.qualifiedNameOfMember(listPsiClass, getStrPsiMethod))

        assertEquals("com.itangcent.model.Model#setStr", PsiClassUtil.qualifiedNameOfMember(setStrPsiMethod))
        assertEquals("java.util.List#setStr", PsiClassUtil.qualifiedNameOfMember(listPsiClass, setStrPsiMethod))

        assertEquals("com.itangcent.model.Model#str", PsiClassUtil.qualifiedNameOfMember(strPsiField))
        assertEquals("java.util.List#str", PsiClassUtil.qualifiedNameOfMember(listPsiClass, strPsiField))

        assertEquals("com.itangcent.model.Model#integer", PsiClassUtil.qualifiedNameOfMember(integerPsiField))
        assertEquals("java.util.List#integer", PsiClassUtil.qualifiedNameOfMember(listPsiClass, integerPsiField))

        assertEquals("str", PsiClassUtil.qualifiedNameOfMember(strPsiParameter))
        assertEquals(
            "str", PsiClassUtil.qualifiedNameOfMember(listPsiClass, strPsiParameter)
        )
    }

    fun testNameOfMember() {
        assertEquals("getStr", PsiClassUtil.nameOfMember(getStrPsiMethod))
        assertEquals("setStr", PsiClassUtil.nameOfMember(setStrPsiMethod))
        assertEquals("str", PsiClassUtil.nameOfMember(strPsiField))
        assertEquals("integer", PsiClassUtil.nameOfMember(integerPsiField))

        // Test PsiParameter
        assertEquals("str", PsiClassUtil.nameOfMember(strPsiParameter))

        // Test PsiFile
        assertEquals("CommentDemo.java", PsiClassUtil.nameOfMember(commentDemoPsiFile))

        // Test simple mock PsiElement (not a PsiNamedElement)
        val mockPsiElement = mock<PsiElement>()
        assertEquals("anonymous", PsiClassUtil.nameOfMember(mockPsiElement))

        // Test mock PsiNamedElement with null name
        val mockPsiNamedElement = mock<PsiNamedElement> {
            Mockito.`when`(it.name).thenReturn(null)
        }
        assertEquals("anonymous", PsiClassUtil.nameOfMember(mockPsiNamedElement))

        // Test mock PsiNamedElement with name
        val mockPsiNamedElementWithName = mock<PsiNamedElement> {
            Mockito.`when`(it.name).thenReturn("customElement")
        }
        assertEquals("customElement", PsiClassUtil.nameOfMember(mockPsiNamedElementWithName))
    }

    fun testGetAllMethods() {
        // Test with HashMap which inherits methods from AbstractMap and implements Map interface
        val hashMapMethods = PsiClassUtil.getAllMethods(hashMapPsiClass)

        // Verify that methods from HashMap itself are included
        assertTrue(hashMapMethods.any { it.name == "put" })
        assertTrue(hashMapMethods.any { it.name == "get" })

        // Verify that methods inherited from AbstractMap are included
        assertTrue(hashMapMethods.any { it.name == "isEmpty" })
        assertTrue(hashMapMethods.any { it.name == "containsKey" })

        // Verify that methods from Map interface are included
        assertTrue(hashMapMethods.any { it.name == "size" })
        assertTrue(hashMapMethods.any { it.name == "clear" })

        // Verify that methods from Object are included
        assertTrue(hashMapMethods.any { it.name == "equals" })
        assertTrue(hashMapMethods.any { it.name == "hashCode" })
        assertTrue(hashMapMethods.any { it.name == "toString" })

        // Test with LinkedList which implements List interface
        val linkedListMethods = PsiClassUtil.getAllMethods(linkedListPsiClass)

        // Verify that methods from LinkedList itself are included
        assertTrue(linkedListMethods.any { it.name == "add" })
        assertTrue(linkedListMethods.any { it.name == "remove" })

        // Verify that methods from List interface are included
        assertTrue(linkedListMethods.any { it.name == "get" })
        assertTrue(linkedListMethods.any { it.name == "indexOf" })

        // Test with Model class
        val modelMethods = PsiClassUtil.getAllMethods(modelPsiClass)

        // Verify that methods from Model itself are included
        assertTrue(modelMethods.any { it.name == "getStr" })
        assertTrue(modelMethods.any { it.name == "setStr" })

        // Verify that methods from Object are included
        assertTrue(modelMethods.any { it.name == "equals" })
        assertTrue(modelMethods.any { it.name == "hashCode" })
        assertTrue(modelMethods.any { it.name == "toString" })
    }

    fun testLogicalNameOfMember() {
        // Test PsiMethod - should add parentheses
        assertEquals("getStr()", PsiClassUtil.logicalNameOfMember(getStrPsiMethod))
        assertEquals("setStr()", PsiClassUtil.logicalNameOfMember(setStrPsiMethod))

        // Test PsiField - should return just the name
        assertEquals("str", PsiClassUtil.logicalNameOfMember(strPsiField))
        assertEquals("integer", PsiClassUtil.logicalNameOfMember(integerPsiField))

        // Test PsiParameter
        assertEquals("str", PsiClassUtil.logicalNameOfMember(strPsiParameter))

        // Test PsiFile
        assertEquals("CommentDemo.java", PsiClassUtil.logicalNameOfMember(commentDemoPsiFile))

        // Test simple mock PsiElement (not a PsiNamedElement)
        val mockPsiElement = mock<PsiElement>()
        assertEquals("anonymous", PsiClassUtil.logicalNameOfMember(mockPsiElement))

        // Test mock PsiNamedElement with null name
        val mockPsiNamedElement = mock<PsiNamedElement> {
            Mockito.`when`(it.name).thenReturn(null)
        }
        assertEquals("anonymous", PsiClassUtil.logicalNameOfMember(mockPsiNamedElement))

        // Test mock PsiNamedElement with name
        val mockPsiNamedElementWithName = mock<PsiNamedElement> {
            Mockito.`when`(it.name).thenReturn("customElement")
        }
        assertEquals("customElement", PsiClassUtil.logicalNameOfMember(mockPsiNamedElementWithName))
    }

    fun testGetPackageNameOf() {
        // Test with standard Java classes
        assertEquals("java.lang", PsiClassUtil.getPackageNameOf(objectPsiClass))
        assertEquals("java.util", PsiClassUtil.getPackageNameOf(collectionPsiClass))

        // Test with model classes
        assertEquals("com.itangcent.model", PsiClassUtil.getPackageNameOf(modelPsiClass))

        // Test with a class that has null qualified name
        val mockClassWithNullName = mock<PsiClass> {
            Mockito.`when`(it.qualifiedName).thenReturn(null)
        }
        assertNull(PsiClassUtil.getPackageNameOf(mockClassWithNullName))

        // Test with inner class
        val mockInnerClass = mock<PsiClass> {
            Mockito.`when`(it.qualifiedName).thenReturn("com.example.OuterClass.InnerClass")
        }
        assertEquals("com.example.OuterClass", PsiClassUtil.getPackageNameOf(mockInnerClass))
    }
}