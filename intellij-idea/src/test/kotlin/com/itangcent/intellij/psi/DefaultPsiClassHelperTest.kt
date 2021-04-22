package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTypesUtil
import com.itangcent.common.utils.GsonUtils
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.jvm.PsiClassHelper
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import java.util.*
import java.util.Collection

/**
 * Test case of [DefaultPsiClassHelper]
 */
internal class DefaultPsiClassHelperTest : ContextLightCodeInsightFixtureTestCase() {

    @Inject
    private lateinit var psiClassHelper: PsiClassHelper

    private lateinit var objectPsiClass: PsiClass
    private lateinit var integerPsiClass: PsiClass
    private lateinit var stringPsiClass: PsiClass
    private lateinit var collectionPsiClass: PsiClass
    private lateinit var listPsiClass: PsiClass
    private lateinit var mapPsiClass: PsiClass
    private lateinit var hashMapPsiClass: PsiClass
    private lateinit var linkedListPsiClass: PsiClass
    private lateinit var modelPsiClass: PsiClass
    private lateinit var javaVersionPsiClass: PsiClass
    private lateinit var numbersPsiClass: PsiClass

    override fun beforeBind() {
        super.beforeBind()
        loadFile("annotation/JsonProperty.java")!!
        objectPsiClass = loadSource(Object::class.java)!!
        integerPsiClass = loadSource(Integer::class)!!
        stringPsiClass = loadSource(java.lang.String::class)!!
        collectionPsiClass = loadSource(Collection::class.java)!!
        mapPsiClass = loadSource(java.util.Map::class.java)!!
        listPsiClass = loadSource(java.util.List::class.java)!!
        hashMapPsiClass = loadSource(java.util.HashMap::class.java)!!
        linkedListPsiClass = loadSource(LinkedList::class.java)!!
        modelPsiClass = loadClass("model/Model.java")!!
        javaVersionPsiClass = loadClass("constant/JavaVersion.java")!!
        numbersPsiClass = loadClass("constant/Numbers.java")!!
    }

    override fun bind(builder: ActionContext.ActionContextBuilder) {
        super.bind(builder)
        builder.bind(PsiClassHelper::class) { it.with(DefaultPsiClassHelper::class) }
    }

    override fun customConfig(): String {
        return "json.rule.field.name=@com.fasterxml.jackson.annotation.JsonProperty#value"
    }

    fun testGetTypeObject() {

        //getTypeObject from psiType without option-------------------------------------------------

        assertEquals(
            "{}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(PsiTypesUtil.getClassType(objectPsiClass), objectPsiClass))
        )
        assertEquals(
            "0",
            GsonUtils.toJson(psiClassHelper.getTypeObject(PsiTypesUtil.getClassType(integerPsiClass), integerPsiClass))
        )
        assertEquals(
            "\"\"",
            GsonUtils.toJson(psiClassHelper.getTypeObject(PsiTypesUtil.getClassType(stringPsiClass), stringPsiClass))
        )
        assertEquals(
            "[]", GsonUtils.toJson(
                psiClassHelper.getTypeObject(PsiTypesUtil.getClassType(collectionPsiClass), collectionPsiClass)
            )
        )
        assertEquals(
            "{}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(PsiTypesUtil.getClassType(mapPsiClass), mapPsiClass))
        )
        assertEquals(
            "[]",
            GsonUtils.toJson(psiClassHelper.getTypeObject(PsiTypesUtil.getClassType(listPsiClass), listPsiClass))
        )
        assertEquals(
            "{}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(PsiTypesUtil.getClassType(hashMapPsiClass), hashMapPsiClass))
        )
        assertEquals(
            "[]",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(linkedListPsiClass),
                    linkedListPsiClass
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0]}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(PsiTypesUtil.getClassType(modelPsiClass), modelPsiClass))
        )

        //getTypeObject from psiType  with option-------------------------------------------------

        assertEquals(
            "{}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(objectPsiClass),
                    objectPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "0",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(integerPsiClass), integerPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "\"\"",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(stringPsiClass), stringPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "[]", GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(collectionPsiClass), collectionPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "{}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(mapPsiClass), mapPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "[]",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(listPsiClass), listPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "{}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(hashMapPsiClass), hashMapPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "[]",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(linkedListPsiClass),
                    linkedListPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0]}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(modelPsiClass), modelPsiClass,
                    JsonOption.NONE
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlyGet\":\"\"}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(modelPsiClass), modelPsiClass,
                    JsonOption.READ_GETTER
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlySet\":\"\"}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(modelPsiClass), modelPsiClass,
                    JsonOption.READ_SETTER
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"@comment\":{\"s\":\"string field\",\"integer\":\"integer field\",\"stringList\":\"stringList field\",\"integerArray\":\"integerArray field\"},\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlySet\":\"\",\"onlyGet\":\"\"}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(modelPsiClass), modelPsiClass,
                    JsonOption.ALL
                )
            )
        )

        //getTypeObject from duckType without option-------------------------------------------------

        assertEquals(
            "{}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(SingleDuckType(objectPsiClass), objectPsiClass))
        )
        assertEquals(
            "0",
            GsonUtils.toJson(psiClassHelper.getTypeObject(SingleDuckType(integerPsiClass), integerPsiClass))
        )
        assertEquals(
            "\"\"",
            GsonUtils.toJson(psiClassHelper.getTypeObject(SingleDuckType(stringPsiClass), stringPsiClass))
        )
        assertEquals(
            "[]", GsonUtils.toJson(
                psiClassHelper.getTypeObject(SingleDuckType(collectionPsiClass), collectionPsiClass)
            )
        )
        assertEquals(
            "{}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(SingleDuckType(mapPsiClass), mapPsiClass))
        )
        assertEquals(
            "[]",
            GsonUtils.toJson(psiClassHelper.getTypeObject(SingleDuckType(listPsiClass), listPsiClass))
        )
        assertEquals(
            "{}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(SingleDuckType(hashMapPsiClass), hashMapPsiClass))
        )
        assertEquals(
            "[]",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(linkedListPsiClass),
                    linkedListPsiClass
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0]}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(SingleDuckType(modelPsiClass), modelPsiClass))
        )

        //getTypeObject from duckType  with option-------------------------------------------------

        assertEquals(
            "{}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(objectPsiClass),
                    objectPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "0",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(integerPsiClass), integerPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "\"\"",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(stringPsiClass), stringPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "[]", GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(collectionPsiClass), collectionPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "{}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(mapPsiClass), mapPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "[]",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(listPsiClass), listPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "{}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(hashMapPsiClass), hashMapPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "[]",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(linkedListPsiClass),
                    linkedListPsiClass,
                    JsonOption.ALL
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0]}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(modelPsiClass), modelPsiClass,
                    JsonOption.NONE
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlyGet\":\"\"}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(modelPsiClass), modelPsiClass,
                    JsonOption.READ_GETTER
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlySet\":\"\"}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(modelPsiClass), modelPsiClass,
                    JsonOption.READ_SETTER
                )
            )
        )
        assertEquals(
            "{\"s\":\"\",\"@comment\":{\"s\":\"string field\",\"integer\":\"integer field\",\"stringList\":\"stringList field\",\"integerArray\":\"integerArray field\"},\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlySet\":\"\",\"onlyGet\":\"\"}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(modelPsiClass), modelPsiClass,
                    JsonOption.ALL
                )
            )
        )
    }

    fun testGetFields() {
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0]}",
            GsonUtils.toJson(psiClassHelper.getFields(modelPsiClass))
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0]}",
            GsonUtils.toJson(psiClassHelper.getFields(modelPsiClass, modelPsiClass))
        )
        assertEquals(
            "{\"s\":\"\",\"@comment\":{\"s\":\"string field\",\"integer\":\"integer field\",\"stringList\":\"stringList field\",\"integerArray\":\"integerArray field\"},\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlySet\":\"\",\"onlyGet\":\"\"}",
            GsonUtils.toJson(psiClassHelper.getFields(modelPsiClass, JsonOption.ALL))
        )
        assertEquals(
            "{\"s\":\"\",\"@comment\":{\"s\":\"string field\",\"integer\":\"integer field\",\"stringList\":\"stringList field\",\"integerArray\":\"integerArray field\"},\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlySet\":\"\",\"onlyGet\":\"\"}",
            GsonUtils.toJson(psiClassHelper.getFields(modelPsiClass, modelPsiClass, JsonOption.ALL))
        )
    }

    fun testIsNormalType() {
        //check isNormalType from PsiClass
        assertTrue(psiClassHelper.isNormalType(objectPsiClass))
        assertTrue(psiClassHelper.isNormalType(integerPsiClass))
        assertTrue(psiClassHelper.isNormalType(stringPsiClass))
        assertFalse(psiClassHelper.isNormalType(collectionPsiClass))
        assertFalse(psiClassHelper.isNormalType(mapPsiClass))
        assertFalse(psiClassHelper.isNormalType(listPsiClass))
        assertFalse(psiClassHelper.isNormalType(hashMapPsiClass))
        assertFalse(psiClassHelper.isNormalType(linkedListPsiClass))
        assertFalse(psiClassHelper.isNormalType(modelPsiClass))

        //check isNormalType from PsiType
        assertTrue(psiClassHelper.isNormalType(PsiTypesUtil.getClassType(objectPsiClass)))
        assertTrue(psiClassHelper.isNormalType(PsiTypesUtil.getClassType(integerPsiClass)))
        assertTrue(psiClassHelper.isNormalType(PsiTypesUtil.getClassType(stringPsiClass)))
        assertFalse(psiClassHelper.isNormalType(PsiTypesUtil.getClassType(collectionPsiClass)))
        assertFalse(psiClassHelper.isNormalType(PsiTypesUtil.getClassType(mapPsiClass)))
        assertFalse(psiClassHelper.isNormalType(PsiTypesUtil.getClassType(listPsiClass)))
        assertFalse(psiClassHelper.isNormalType(PsiTypesUtil.getClassType(hashMapPsiClass)))
        assertFalse(psiClassHelper.isNormalType(PsiTypesUtil.getClassType(linkedListPsiClass)))
        assertFalse(psiClassHelper.isNormalType(PsiTypesUtil.getClassType(modelPsiClass)))
    }

    fun testUnboxArrayOrList() {
        assertEquals("java.lang.String", psiClassHelper.unboxArrayOrList(modelPsiClass.fields[0].type).canonicalText)
        assertEquals("java.lang.Integer", psiClassHelper.unboxArrayOrList(modelPsiClass.fields[1].type).canonicalText)
        assertEquals("java.lang.String", psiClassHelper.unboxArrayOrList(modelPsiClass.fields[2].type).canonicalText)
        assertEquals("java.lang.Integer", psiClassHelper.unboxArrayOrList(modelPsiClass.fields[3].type).canonicalText)
    }

    fun testGetDefaultValue() {

        //check getDefaultValue of PsiClass
        assertEquals(emptyMap<Any, Any>(), psiClassHelper.getDefaultValue(objectPsiClass))
        assertEquals(0, psiClassHelper.getDefaultValue(integerPsiClass))
        assertEquals("", psiClassHelper.getDefaultValue(stringPsiClass))
        assertEquals(null, psiClassHelper.getDefaultValue(collectionPsiClass))
        assertEquals(null, psiClassHelper.getDefaultValue(mapPsiClass))
        assertEquals(null, psiClassHelper.getDefaultValue(listPsiClass))
        assertEquals(null, psiClassHelper.getDefaultValue(hashMapPsiClass))
        assertEquals(null, psiClassHelper.getDefaultValue(linkedListPsiClass))
        assertEquals(null, psiClassHelper.getDefaultValue(modelPsiClass))

        //check getDefaultValue of PsiType
        assertEquals(emptyMap<Any, Any>(), psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(objectPsiClass)))
        assertEquals(0, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(integerPsiClass)))
        assertEquals("", psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(stringPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(collectionPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(mapPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(listPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(hashMapPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(linkedListPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(modelPsiClass)))
    }

    fun testGetJsonFieldName() {
        assertEquals("s", psiClassHelper.getJsonFieldName(modelPsiClass.fields[0]))
        assertEquals("integer", psiClassHelper.getJsonFieldName(modelPsiClass.fields[1]))
    }

    fun testParseStaticFields() {
        assertEquals(
            "[{\"name\":\"ONE\",\"value\":\"1\",\"desc\":\"one\"},{\"name\":\"TWO\",\"value\":\"2\",\"desc\":\"two\"},{\"name\":\"THREE\",\"value\":\"3\",\"desc\":\"three\"},{\"name\":\"FOUR\",\"value\":\"4\",\"desc\":\"four\"}]",
            GsonUtils.toJson(psiClassHelper.parseStaticFields(numbersPsiClass))
        )
    }

    fun testParseEnumConstant() {
        assertEquals(
            "[{\"params\":{\"name\":\"0.9\",\"value\":1.5},\"name\":\"JAVA_0_9\",\"ordinal\":0,\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"params\":{\"name\":\"1.1\",\"value\":1.1},\"name\":\"JAVA_1_1\",\"ordinal\":1,\"desc\":\"Java 1.1.\"},{\"params\":{\"name\":\"1.2\",\"value\":1.2},\"name\":\"JAVA_1_2\",\"ordinal\":2,\"desc\":\"Java 1.2.\"},{\"params\":{\"name\":\"1.3\",\"value\":1.3},\"name\":\"JAVA_1_3\",\"ordinal\":3,\"desc\":\"Java 1.3.\"},{\"params\":{\"name\":\"1.4\",\"value\":1.4},\"name\":\"JAVA_1_4\",\"ordinal\":4,\"desc\":\"Java 1.4.\"},{\"params\":{\"name\":\"1.5\",\"value\":1.5},\"name\":\"JAVA_1_5\",\"ordinal\":5,\"desc\":\"Java 1.5.\"},{\"params\":{\"name\":\"1.6\",\"value\":1.6},\"name\":\"JAVA_1_6\",\"ordinal\":6,\"desc\":\"Java 1.6.\"},{\"params\":{\"name\":\"1.7\",\"value\":1.7},\"name\":\"JAVA_1_7\",\"ordinal\":7,\"desc\":\"Java 1.7.\"},{\"params\":{\"name\":\"1.8\",\"value\":1.8},\"name\":\"JAVA_1_8\",\"ordinal\":8,\"desc\":\"Java 1.8.\"},{\"params\":{\"name\":\"9\",\"value\":9.0},\"name\":\"JAVA_1_9\",\"ordinal\":9,\"desc\":\"Java 1.9.\"},{\"params\":{\"name\":\"9\",\"value\":9.0},\"name\":\"JAVA_9\",\"ordinal\":10,\"desc\":\"Java 9\"},{\"params\":{\"name\":\"10\",\"value\":10.0},\"name\":\"JAVA_10\",\"ordinal\":11,\"desc\":\"Java 10\"},{\"params\":{\"name\":\"11\",\"value\":11.0},\"name\":\"JAVA_11\",\"ordinal\":12,\"desc\":\"Java 11\"},{\"params\":{\"name\":\"12\",\"value\":12.0},\"name\":\"JAVA_12\",\"ordinal\":13,\"desc\":\"Java 12\"},{\"params\":{\"name\":\"13\",\"value\":13.0},\"name\":\"JAVA_13\",\"ordinal\":14,\"desc\":\"Java 13\"}]",
            GsonUtils.toJson(psiClassHelper.parseEnumConstant(javaVersionPsiClass))
        )
    }

    fun testResolveEnumOrStatic() {
        assertEquals(
            "[]",
            GsonUtils.toJson(
                psiClassHelper.resolveEnumOrStatic(
                    "com.itangcent.constant.JavaVersion",
                    javaVersionPsiClass,
                    ""
                )
            )
        )

        assertEquals(
            "[{\"value\":\"0.9\",\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":\"1.1\",\"desc\":\"Java 1.1.\"},{\"value\":\"1.2\",\"desc\":\"Java 1.2.\"},{\"value\":\"1.3\",\"desc\":\"Java 1.3.\"},{\"value\":\"1.4\",\"desc\":\"Java 1.4.\"},{\"value\":\"1.5\",\"desc\":\"Java 1.5.\"},{\"value\":\"1.6\",\"desc\":\"Java 1.6.\"},{\"value\":\"1.7\",\"desc\":\"Java 1.7.\"},{\"value\":\"1.8\",\"desc\":\"Java 1.8.\"},{\"value\":\"9\",\"desc\":\"Java 1.9.\"},{\"value\":\"9\",\"desc\":\"Java 9\"},{\"value\":\"10\",\"desc\":\"Java 10\"},{\"value\":\"11\",\"desc\":\"Java 11\"},{\"value\":\"12\",\"desc\":\"Java 12\"},{\"value\":\"13\",\"desc\":\"Java 13\"}]",
            GsonUtils.toJson(
                psiClassHelper.resolveEnumOrStatic(
                    "com.itangcent.constant.JavaVersion",
                    javaVersionPsiClass,
                    "name"
                )
            )
        )
        assertEquals(
            "[{\"value\":1.5,\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":1.1,\"desc\":\"Java 1.1.\"},{\"value\":1.2,\"desc\":\"Java 1.2.\"},{\"value\":1.3,\"desc\":\"Java 1.3.\"},{\"value\":1.4,\"desc\":\"Java 1.4.\"},{\"value\":1.5,\"desc\":\"Java 1.5.\"},{\"value\":1.6,\"desc\":\"Java 1.6.\"},{\"value\":1.7,\"desc\":\"Java 1.7.\"},{\"value\":1.8,\"desc\":\"Java 1.8.\"},{\"value\":9.0,\"desc\":\"Java 1.9.\"},{\"value\":9.0,\"desc\":\"Java 9\"},{\"value\":10.0,\"desc\":\"Java 10\"},{\"value\":11.0,\"desc\":\"Java 11\"},{\"value\":12.0,\"desc\":\"Java 12\"},{\"value\":13.0,\"desc\":\"Java 13\"}]",
            GsonUtils.toJson(
                psiClassHelper.resolveEnumOrStatic(
                    "com.itangcent.constant.JavaVersion",
                    javaVersionPsiClass,
                    "value"
                )
            )
        )

        //

        assertEquals(
            "[{\"value\":\"0.9\",\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":\"1.1\",\"desc\":\"Java 1.1.\"},{\"value\":\"1.2\",\"desc\":\"Java 1.2.\"},{\"value\":\"1.3\",\"desc\":\"Java 1.3.\"},{\"value\":\"1.4\",\"desc\":\"Java 1.4.\"},{\"value\":\"1.5\",\"desc\":\"Java 1.5.\"},{\"value\":\"1.6\",\"desc\":\"Java 1.6.\"},{\"value\":\"1.7\",\"desc\":\"Java 1.7.\"},{\"value\":\"1.8\",\"desc\":\"Java 1.8.\"},{\"value\":\"9\",\"desc\":\"Java 1.9.\"},{\"value\":\"9\",\"desc\":\"Java 9\"},{\"value\":\"10\",\"desc\":\"Java 10\"},{\"value\":\"11\",\"desc\":\"Java 11\"},{\"value\":\"12\",\"desc\":\"Java 12\"},{\"value\":\"13\",\"desc\":\"Java 13\"}]",
            GsonUtils.toJson(
                psiClassHelper.resolveEnumOrStatic(
                    javaVersionPsiClass, javaVersionPsiClass,
                    "name",
                    "name"
                )
            )
        )

        assertEquals(
            "[{\"value\":\"1\",\"desc\":\"one\"},{\"value\":\"2\",\"desc\":\"two\"},{\"value\":\"3\",\"desc\":\"three\"},{\"value\":\"4\",\"desc\":\"four\"}]",
            GsonUtils.toJson(
                psiClassHelper.resolveEnumOrStatic(
                    numbersPsiClass, numbersPsiClass,
                    "",
                    ""
                )
            )
        )
    }
}