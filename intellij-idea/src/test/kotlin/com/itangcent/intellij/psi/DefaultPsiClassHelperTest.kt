package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTypesUtil
import com.itangcent.common.utils.GsonUtils
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.jvm.JsonOption
import com.itangcent.intellij.jvm.PsiClassHelper
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
    private lateinit var nodePsiClass: PsiClass
    private lateinit var hugeModelPsiClass: PsiClass
    private lateinit var userInfoPsiClass: PsiClass
    private lateinit var userInfoDetailPsiClass: PsiClass
    private lateinit var userLoginInfoPsiClass: PsiClass
    private lateinit var javaVersionPsiClass: PsiClass
    private lateinit var numbersPsiClass: PsiClass
    private lateinit var myNoArgConstantPsiClass: PsiClass
    private lateinit var resultPsiClass: PsiClass

    override fun beforeBind() {
        super.beforeBind()
        loadFile("annotation/JsonProperty.java")!!
        objectPsiClass = loadSource(Object::class.java)!!
        integerPsiClass = loadSource(Integer::class)!!
        loadSource(java.lang.Long::class)!!
        stringPsiClass = loadSource(java.lang.String::class)!!
        collectionPsiClass = loadSource(Collection::class.java)!!
        mapPsiClass = loadSource(java.util.Map::class.java)!!
        listPsiClass = loadSource(java.util.List::class.java)!!
        hashMapPsiClass = loadSource(java.util.HashMap::class.java)!!
        linkedListPsiClass = loadSource(LinkedList::class.java)!!
        loadSource(LocalDate::class.java)
        loadSource(LocalDateTime::class.java)
        javaVersionPsiClass = loadClass("constant/JavaVersion.java")!!
        numbersPsiClass = loadClass("constant/Numbers.java")!!
        myNoArgConstantPsiClass = loadClass("constant/MyNoArgConstant.java")!!
        modelPsiClass = loadClass("model/Model.java")!!
        nodePsiClass = loadClass("model/Node.java")!!
        hugeModelPsiClass = loadClass("model/HugeModel.java")!!
        userInfoPsiClass = loadClass("model/UserInfo.java")!!
        userInfoDetailPsiClass = loadClass("model/UserInfoDetail.java")!!
        userLoginInfoPsiClass = loadClass("model/UserLoginInfo.java")!!
        resultPsiClass = loadClass("model/Result.java")!!
    }

    override fun bind(builder: ActionContext.ActionContextBuilder) {
        super.bind(builder)
        builder.bind(PsiClassHelper::class) { it.with(DefaultPsiClassHelper::class) }
    }

    override fun customConfig(): String {
        return "json.rule.field.name=@com.fasterxml.jackson.annotation.JsonProperty#value\n" +
                "field.type=#type\n" +
                "json.unwrapped=#unwrapped\n" +
                "json.rule.convert[java.util.Date]=java.lang.String\n" +
                "json.rule.convert[java.sql.Timestamp]=java.lang.String\n" +
                "json.rule.convert[java.time.LocalDateTime]=java.lang.String\n" +
                "json.rule.convert[java.time.LocalDate]=java.lang.String\n" +
                "enum.use.name=true\n" +
                "field.doc[#deprecated]=「Deprecated」\n" +
                "field.ignore=#ignore\n" +
                "field.order=#order"
    }

    fun testGetTypeObject() {
        doTestGetTypeObject()
        doTestGetTypeObject()
    }

    fun doTestGetTypeObject() {

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

        assertEquals(
            "{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(PsiTypesUtil.getClassType(nodePsiClass), nodePsiClass))
        )
        assertEquals(
            "{\"shouldBeFirst\":0,\"a\":\"\",\"b\":\"\",\"c\":\"\",\"d\":{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]},\"e\":\"\",\"r\":\"\",\"shouldBeLastIfSetter\":0,\"ignoreByGetter\":\"\",\"ignoreBySetter\":\"\",\"candidates\":[\"\"],\"version\":\"\",\"myNoArgConstant\":\"\",\"id\":0,\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\",\"shouldBeFirstIfGetter\":0,\"shouldBeLast\":0}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(hugeModelPsiClass),
                    hugeModelPsiClass
                )
            )
        )
        assertEquals(
            "{\"code\":0,\"msg\":\"\",\"data\":{},\"extra\":{}}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(resultPsiClass),
                    resultPsiClass
                )
            )
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
            "{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(nodePsiClass), nodePsiClass,
                    JsonOption.NONE
                )
            )
        )
        assertEquals(
            "{\"shouldBeFirst\":0,\"a\":\"\",\"b\":\"\",\"c\":\"\",\"d\":{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]},\"e\":\"\",\"r\":\"\",\"shouldBeLastIfSetter\":0,\"ignoreByGetter\":\"\",\"ignoreBySetter\":\"\",\"candidates\":[\"\"],\"version\":\"\",\"myNoArgConstant\":\"\",\"id\":0,\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\",\"shouldBeFirstIfGetter\":0,\"shouldBeLast\":0}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    PsiTypesUtil.getClassType(hugeModelPsiClass), hugeModelPsiClass,
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

        assertEquals(
            "{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(SingleDuckType(nodePsiClass), nodePsiClass))
        )

        assertEquals(
            "{\"shouldBeFirst\":0,\"a\":\"\",\"b\":\"\",\"c\":\"\",\"d\":{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]},\"e\":\"\",\"r\":\"\",\"shouldBeLastIfSetter\":0,\"ignoreByGetter\":\"\",\"ignoreBySetter\":\"\",\"candidates\":[\"\"],\"version\":\"\",\"myNoArgConstant\":\"\",\"id\":0,\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\",\"shouldBeFirstIfGetter\":0,\"shouldBeLast\":0}",
            GsonUtils.toJson(psiClassHelper.getTypeObject(SingleDuckType(hugeModelPsiClass), hugeModelPsiClass))
        )

        assertEquals(
            "{\"code\":0,\"msg\":\"\",\"data\":{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0]},\"extra\":{\"{}\":{}}}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(resultPsiClass, mapOf("T" to SingleDuckType(modelPsiClass))),
                    resultPsiClass
                )
            )
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
        assertEquals(
            "{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]}",
            GsonUtils.toJson(
                psiClassHelper.getTypeObject(
                    SingleDuckType(nodePsiClass), nodePsiClass,
                    JsonOption.ALL
                )
            )
        )

    }

    fun testGetFields() {
        doTestGetFields()
        doTestGetFields()
    }

    fun doTestGetFields() {
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0]}",
            GsonUtils.toJson(psiClassHelper.getFields(modelPsiClass))
        )
        assertEquals(
            "{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]}",
            GsonUtils.toJson(psiClassHelper.getFields(nodePsiClass))
        )
        assertEquals(
            "{\"shouldBeFirst\":0,\"a\":\"\",\"b\":\"\",\"c\":\"\",\"d\":{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]},\"e\":\"\",\"r\":\"\",\"shouldBeLastIfSetter\":0,\"ignoreByGetter\":\"\",\"ignoreBySetter\":\"\",\"candidates\":[\"\"],\"version\":\"\",\"myNoArgConstant\":\"\",\"id\":0,\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\",\"shouldBeFirstIfGetter\":0,\"shouldBeLast\":0}",
            GsonUtils.toJson(psiClassHelper.getFields(hugeModelPsiClass))
        )
        assertEquals(
            "{\"s\":\"\",\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0]}",
            GsonUtils.toJson(psiClassHelper.getFields(modelPsiClass, modelPsiClass))
        )
        assertEquals(
            "{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]}",
            GsonUtils.toJson(psiClassHelper.getFields(nodePsiClass, nodePsiClass))
        )
        assertEquals(
            "{\"shouldBeFirst\":0,\"a\":\"\",\"b\":\"\",\"c\":\"\",\"d\":{\"id\":\"\",\"code\":\"\",\"parent\":{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]},\"e\":\"\",\"r\":\"\",\"shouldBeLastIfSetter\":0,\"ignoreByGetter\":\"\",\"ignoreBySetter\":\"\",\"candidates\":[\"\"],\"version\":\"\",\"myNoArgConstant\":\"\",\"id\":0,\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\",\"shouldBeFirstIfGetter\":0,\"shouldBeLast\":0}",
            GsonUtils.toJson(psiClassHelper.getFields(hugeModelPsiClass, hugeModelPsiClass))
        )
        assertEquals(
            "{\"s\":\"\",\"@comment\":{\"s\":\"string field\",\"integer\":\"integer field\",\"stringList\":\"stringList field\",\"integerArray\":\"integerArray field\"},\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlySet\":\"\",\"onlyGet\":\"\"}",
            GsonUtils.toJson(psiClassHelper.getFields(modelPsiClass, JsonOption.ALL))
        )
        assertEquals(
            "{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]}",
            GsonUtils.toJson(psiClassHelper.getFields(nodePsiClass, JsonOption.ALL))
        )
        assertEquals(
            "{\"shouldBeFirstIfGetter\":0,\"@comment\":{\"shouldBeFirstIfGetter\":\"\",\"shouldBeFirst\":\"\",\"a\":\"single line\",\"b\":\"multi-line\\nsecond line\",\"c\":\"head line\\nsecond line\\n<pre>\\n    {\\n        \\\"a\\\":\\\"b\\\",\\n        \\\"c\\\":{\\n             \\\"x\\\":[\\\"y\\\"]\\n        }\\n    }\\n</pre>\\nsee{@link somelink}\\ntail line\",\"d\":\"head line\\nsecond line\\n<pre>\\n\\n    {\\n        \\\"a\\\":\\\"b\\\",\\n        \\\"c\\\":{\\n             \\\"x\\\":[\\\"y\\\"]\\n        }\\n    }\\n\\n</pre>\\n<p>\\nsee{@link somelink}\\ntail line\",\"e\":\"E is a mathematical constant approximately equal to 2.71828\",\"r\":\"R, or r, is the eighteenth letter of the modern English alphabet and the ISO basic Latin alphabet.\",\"candidates\":\"candidates versions\",\"version\":\"primary version\",\"myNoArgConstant\":\"no arg constant\",\"userInfo\":\"\",\"shouldBeLast\":\"\",\"shouldBeLastIfSetter\":\"\",\"candidates@options\":[{\"value\":\"JAVA_0_9\",\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":\"JAVA_1_1\",\"desc\":\"Java 1.1.\"},{\"value\":\"JAVA_1_2\",\"desc\":\"Java 1.2.\"},{\"value\":\"JAVA_1_3\",\"desc\":\"Java 1.3.\"},{\"value\":\"JAVA_1_4\",\"desc\":\"Java 1.4.\"},{\"value\":\"JAVA_1_5\",\"desc\":\"Java 1.5.\"},{\"value\":\"JAVA_1_6\",\"desc\":\"Java 1.6.\"},{\"value\":\"JAVA_1_7\",\"desc\":\"Java 1.7.\"},{\"value\":\"JAVA_1_8\",\"desc\":\"Java 1.8.\"},{\"value\":\"JAVA_1_9\",\"desc\":\"Java 1.9.\\n「Deprecated」\"},{\"value\":\"JAVA_9\",\"desc\":\"Java 9\"},{\"value\":\"JAVA_10\",\"desc\":\"Java 10\"},{\"value\":\"JAVA_11\",\"desc\":\"Java 11\"},{\"value\":\"JAVA_12\",\"desc\":\"Java 12\"},{\"value\":\"JAVA_13\",\"desc\":\"Java 13\"}],\"version@options\":[{\"value\":\"JAVA_0_9\",\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":\"JAVA_1_1\",\"desc\":\"Java 1.1.\"},{\"value\":\"JAVA_1_2\",\"desc\":\"Java 1.2.\"},{\"value\":\"JAVA_1_3\",\"desc\":\"Java 1.3.\"},{\"value\":\"JAVA_1_4\",\"desc\":\"Java 1.4.\"},{\"value\":\"JAVA_1_5\",\"desc\":\"Java 1.5.\"},{\"value\":\"JAVA_1_6\",\"desc\":\"Java 1.6.\"},{\"value\":\"JAVA_1_7\",\"desc\":\"Java 1.7.\"},{\"value\":\"JAVA_1_8\",\"desc\":\"Java 1.8.\"},{\"value\":\"JAVA_1_9\",\"desc\":\"Java 1.9.\\n「Deprecated」\"},{\"value\":\"JAVA_9\",\"desc\":\"Java 9\"},{\"value\":\"JAVA_10\",\"desc\":\"Java 10\"},{\"value\":\"JAVA_11\",\"desc\":\"Java 11\"},{\"value\":\"JAVA_12\",\"desc\":\"Java 12\"},{\"value\":\"JAVA_13\",\"desc\":\"Java 13\"}],\"myNoArgConstant@options\":[{\"value\":\"ONE\",\"desc\":\"1st\"},{\"value\":\"TWO\",\"desc\":\"2nd\"},{\"value\":\"THREE\",\"desc\":\"3rd\"}],\"id\":\"user id\",\"type\":\"\",\"name\":\"\",\"age\":\"user age\",\"sex\":\"「Deprecated」\",\"birthDay\":\"user birthDay\",\"regtime\":\"user regtime\"},\"shouldBeFirst\":0,\"a\":\"\",\"b\":\"\",\"c\":\"\",\"d\":{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]},\"e\":\"\",\"r\":\"\",\"candidates\":[\"\"],\"version\":\"\",\"myNoArgConstant\":\"\",\"id\":0,\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\",\"shouldBeLast\":0,\"shouldBeLastIfSetter\":0}",
            GsonUtils.toJson(psiClassHelper.getFields(hugeModelPsiClass, JsonOption.ALL))
        )
        assertEquals(
            "{\"s\":\"\",\"@comment\":{\"s\":\"string field\",\"integer\":\"integer field\",\"stringList\":\"stringList field\",\"integerArray\":\"integerArray field\"},\"integer\":0,\"stringList\":[\"\"],\"integerArray\":[0],\"onlySet\":\"\",\"onlyGet\":\"\"}",
            GsonUtils.toJson(psiClassHelper.getFields(modelPsiClass, modelPsiClass, JsonOption.ALL))
        )
        assertEquals(
            "{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]}",
            GsonUtils.toJson(psiClassHelper.getFields(nodePsiClass, nodePsiClass, JsonOption.ALL))
        )
        assertEquals(
            "{\"shouldBeFirstIfGetter\":0,\"@comment\":{\"shouldBeFirstIfGetter\":\"\",\"shouldBeFirst\":\"\",\"a\":\"single line\",\"b\":\"multi-line\\nsecond line\",\"c\":\"head line\\nsecond line\\n<pre>\\n    {\\n        \\\"a\\\":\\\"b\\\",\\n        \\\"c\\\":{\\n             \\\"x\\\":[\\\"y\\\"]\\n        }\\n    }\\n</pre>\\nsee{@link somelink}\\ntail line\",\"d\":\"head line\\nsecond line\\n<pre>\\n\\n    {\\n        \\\"a\\\":\\\"b\\\",\\n        \\\"c\\\":{\\n             \\\"x\\\":[\\\"y\\\"]\\n        }\\n    }\\n\\n</pre>\\n<p>\\nsee{@link somelink}\\ntail line\",\"e\":\"E is a mathematical constant approximately equal to 2.71828\",\"r\":\"R, or r, is the eighteenth letter of the modern English alphabet and the ISO basic Latin alphabet.\",\"candidates\":\"candidates versions\",\"version\":\"primary version\",\"myNoArgConstant\":\"no arg constant\",\"userInfo\":\"\",\"shouldBeLast\":\"\",\"shouldBeLastIfSetter\":\"\",\"candidates@options\":[{\"value\":\"JAVA_0_9\",\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":\"JAVA_1_1\",\"desc\":\"Java 1.1.\"},{\"value\":\"JAVA_1_2\",\"desc\":\"Java 1.2.\"},{\"value\":\"JAVA_1_3\",\"desc\":\"Java 1.3.\"},{\"value\":\"JAVA_1_4\",\"desc\":\"Java 1.4.\"},{\"value\":\"JAVA_1_5\",\"desc\":\"Java 1.5.\"},{\"value\":\"JAVA_1_6\",\"desc\":\"Java 1.6.\"},{\"value\":\"JAVA_1_7\",\"desc\":\"Java 1.7.\"},{\"value\":\"JAVA_1_8\",\"desc\":\"Java 1.8.\"},{\"value\":\"JAVA_1_9\",\"desc\":\"Java 1.9.\\n「Deprecated」\"},{\"value\":\"JAVA_9\",\"desc\":\"Java 9\"},{\"value\":\"JAVA_10\",\"desc\":\"Java 10\"},{\"value\":\"JAVA_11\",\"desc\":\"Java 11\"},{\"value\":\"JAVA_12\",\"desc\":\"Java 12\"},{\"value\":\"JAVA_13\",\"desc\":\"Java 13\"}],\"version@options\":[{\"value\":\"JAVA_0_9\",\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":\"JAVA_1_1\",\"desc\":\"Java 1.1.\"},{\"value\":\"JAVA_1_2\",\"desc\":\"Java 1.2.\"},{\"value\":\"JAVA_1_3\",\"desc\":\"Java 1.3.\"},{\"value\":\"JAVA_1_4\",\"desc\":\"Java 1.4.\"},{\"value\":\"JAVA_1_5\",\"desc\":\"Java 1.5.\"},{\"value\":\"JAVA_1_6\",\"desc\":\"Java 1.6.\"},{\"value\":\"JAVA_1_7\",\"desc\":\"Java 1.7.\"},{\"value\":\"JAVA_1_8\",\"desc\":\"Java 1.8.\"},{\"value\":\"JAVA_1_9\",\"desc\":\"Java 1.9.\\n「Deprecated」\"},{\"value\":\"JAVA_9\",\"desc\":\"Java 9\"},{\"value\":\"JAVA_10\",\"desc\":\"Java 10\"},{\"value\":\"JAVA_11\",\"desc\":\"Java 11\"},{\"value\":\"JAVA_12\",\"desc\":\"Java 12\"},{\"value\":\"JAVA_13\",\"desc\":\"Java 13\"}],\"myNoArgConstant@options\":[{\"value\":\"ONE\",\"desc\":\"1st\"},{\"value\":\"TWO\",\"desc\":\"2nd\"},{\"value\":\"THREE\",\"desc\":\"3rd\"}],\"id\":\"user id\",\"type\":\"\",\"name\":\"\",\"age\":\"user age\",\"sex\":\"「Deprecated」\",\"birthDay\":\"user birthDay\",\"regtime\":\"user regtime\"},\"shouldBeFirst\":0,\"a\":\"\",\"b\":\"\",\"c\":\"\",\"d\":{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]},\"sub\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}],\"siblings\":[{\"id\":\"\",\"@comment\":{\"id\":\"primary key\",\"code\":\"org code\",\"parent\":\"\",\"sub\":\"sub nodes\",\"siblings\":\"siblings nodes\"},\"code\":\"\",\"parent\":{},\"sub\":[{}],\"siblings\":[{}]}]},\"e\":\"\",\"r\":\"\",\"candidates\":[\"\"],\"version\":\"\",\"myNoArgConstant\":\"\",\"id\":0,\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\",\"shouldBeLast\":0,\"shouldBeLastIfSetter\":0}",
            GsonUtils.toJson(psiClassHelper.getFields(hugeModelPsiClass, hugeModelPsiClass, JsonOption.ALL))
        )
        assertEquals(
            "{\"id\":0,\"@comment\":{\"id\":\"user id\",\"type\":\"\",\"name\":\"\",\"age\":\"user age\",\"sex\":\"「Deprecated」\",\"birthDay\":\"user birthDay\",\"regtime\":\"user regtime\"},\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\"}",
            GsonUtils.toJson(psiClassHelper.getFields(userInfoPsiClass, userInfoPsiClass, JsonOption.ALL))
        )
        assertEquals(
            "{\"id\":0,\"@comment\":{\"id\":\"user id\",\"type\":\"\",\"name\":\"\",\"age\":\"user age\",\"sex\":\"「Deprecated」\",\"birthDay\":\"user birthDay\",\"regtime\":\"user regtime\",\"level\":\"\"},\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\",\"level\":0}",
            GsonUtils.toJson(psiClassHelper.getFields(userInfoDetailPsiClass, userInfoDetailPsiClass, JsonOption.ALL))
        )
        assertEquals(
            "{\"id\":0,\"@comment\":{\"id\":\"user id\",\"type\":\"\",\"name\":\"\",\"age\":\"user age\",\"sex\":\"「Deprecated」\",\"birthDay\":\"user birthDay\",\"regtime\":\"user regtime\",\"level\":\"\",\"loginTime\":\"\"},\"type\":0,\"name\":\"\",\"age\":0,\"sex\":0,\"birthDay\":\"\",\"regtime\":\"\",\"level\":0,\"loginTime\":0}",
            GsonUtils.toJson(psiClassHelper.getFields(userLoginInfoPsiClass, userLoginInfoPsiClass, JsonOption.ALL))
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
        assertEquals(null, psiClassHelper.getDefaultValue(nodePsiClass))
        assertEquals(null, psiClassHelper.getDefaultValue(hugeModelPsiClass))

        //check getDefaultValue of PsiType
        assertEquals(emptyMap<Any, Any>(), psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(objectPsiClass)))
        assertEquals(0, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(integerPsiClass)))
        assertEquals("", psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(stringPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(collectionPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(mapPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(listPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(hashMapPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(linkedListPsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(nodePsiClass)))
        assertEquals(null, psiClassHelper.getDefaultValue(PsiTypesUtil.getClassType(hugeModelPsiClass)))
    }

    fun testGetJsonFieldName() {
        assertEquals("s", psiClassHelper.getJsonFieldName(modelPsiClass.fields[0]))
        assertEquals("integer", psiClassHelper.getJsonFieldName(modelPsiClass.fields[1]))
    }

    fun testParseStaticFields() {
        doTestParseStaticFields()
        doTestParseStaticFields()
    }

    fun doTestParseStaticFields() {
        assertEquals(
            "[{\"name\":\"ONE\",\"value\":\"1\",\"desc\":\"one\"},{\"name\":\"TWO\",\"value\":\"2\",\"desc\":\"two\"},{\"name\":\"THREE\",\"value\":\"3\",\"desc\":\"three\"},{\"name\":\"FOUR\",\"value\":\"4\",\"desc\":\"four\"}]",
            GsonUtils.toJson(psiClassHelper.parseStaticFields(numbersPsiClass))
        )
    }

    fun testParseEnumConstant() {
        doTestParseEnumConstant()
        doTestParseEnumConstant()
    }

    fun doTestParseEnumConstant() {
        assertEquals(
            "[{\"params\":{\"name\":\"0.9\",\"value\":1.5},\"name\":\"JAVA_0_9\",\"ordinal\":0,\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"params\":{\"name\":\"1.1\",\"value\":1.1},\"name\":\"JAVA_1_1\",\"ordinal\":1,\"desc\":\"Java 1.1.\"},{\"params\":{\"name\":\"1.2\",\"value\":1.2},\"name\":\"JAVA_1_2\",\"ordinal\":2,\"desc\":\"Java 1.2.\"},{\"params\":{\"name\":\"1.3\",\"value\":1.3},\"name\":\"JAVA_1_3\",\"ordinal\":3,\"desc\":\"Java 1.3.\"},{\"params\":{\"name\":\"1.4\",\"value\":1.4},\"name\":\"JAVA_1_4\",\"ordinal\":4,\"desc\":\"Java 1.4.\"},{\"params\":{\"name\":\"1.5\",\"value\":1.5},\"name\":\"JAVA_1_5\",\"ordinal\":5,\"desc\":\"Java 1.5.\"},{\"params\":{\"name\":\"1.6\",\"value\":1.6},\"name\":\"JAVA_1_6\",\"ordinal\":6,\"desc\":\"Java 1.6.\"},{\"params\":{\"name\":\"1.7\",\"value\":1.7},\"name\":\"JAVA_1_7\",\"ordinal\":7,\"desc\":\"Java 1.7.\"},{\"params\":{\"name\":\"1.8\",\"value\":1.8},\"name\":\"JAVA_1_8\",\"ordinal\":8,\"desc\":\"Java 1.8.\"},{\"params\":{\"name\":\"9\",\"value\":9.0},\"name\":\"JAVA_1_9\",\"ordinal\":9,\"desc\":\"Java 1.9.\\n「Deprecated」\"},{\"params\":{\"name\":\"9\",\"value\":9.0},\"name\":\"JAVA_9\",\"ordinal\":10,\"desc\":\"Java 9\"},{\"params\":{\"name\":\"10\",\"value\":10.0},\"name\":\"JAVA_10\",\"ordinal\":11,\"desc\":\"Java 10\"},{\"params\":{\"name\":\"11\",\"value\":11.0},\"name\":\"JAVA_11\",\"ordinal\":12,\"desc\":\"Java 11\"},{\"params\":{\"name\":\"12\",\"value\":12.0},\"name\":\"JAVA_12\",\"ordinal\":13,\"desc\":\"Java 12\"},{\"params\":{\"name\":\"13\",\"value\":13.0},\"name\":\"JAVA_13\",\"ordinal\":14,\"desc\":\"Java 13\"}]",
            GsonUtils.toJson(psiClassHelper.parseEnumConstant(javaVersionPsiClass))
        )
    }

    fun testResolveEnumOrStatic() {
        doTestResolveEnumOrStatic()
        doTestResolveEnumOrStatic()
    }

    fun doTestResolveEnumOrStatic() {
        assertEquals(
            "[{\"value\":\"JAVA_0_9\",\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":\"JAVA_1_1\",\"desc\":\"Java 1.1.\"},{\"value\":\"JAVA_1_2\",\"desc\":\"Java 1.2.\"},{\"value\":\"JAVA_1_3\",\"desc\":\"Java 1.3.\"},{\"value\":\"JAVA_1_4\",\"desc\":\"Java 1.4.\"},{\"value\":\"JAVA_1_5\",\"desc\":\"Java 1.5.\"},{\"value\":\"JAVA_1_6\",\"desc\":\"Java 1.6.\"},{\"value\":\"JAVA_1_7\",\"desc\":\"Java 1.7.\"},{\"value\":\"JAVA_1_8\",\"desc\":\"Java 1.8.\"},{\"value\":\"JAVA_1_9\",\"desc\":\"Java 1.9.\\n「Deprecated」\"},{\"value\":\"JAVA_9\",\"desc\":\"Java 9\"},{\"value\":\"JAVA_10\",\"desc\":\"Java 10\"},{\"value\":\"JAVA_11\",\"desc\":\"Java 11\"},{\"value\":\"JAVA_12\",\"desc\":\"Java 12\"},{\"value\":\"JAVA_13\",\"desc\":\"Java 13\"}]",
            GsonUtils.toJson(
                psiClassHelper.resolveEnumOrStatic(
                    "com.itangcent.constant.JavaVersion",
                    javaVersionPsiClass,
                    ""
                )
            )
        )

        assertEquals(
            "[{\"value\":\"0.9\",\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":\"1.1\",\"desc\":\"Java 1.1.\"},{\"value\":\"1.2\",\"desc\":\"Java 1.2.\"},{\"value\":\"1.3\",\"desc\":\"Java 1.3.\"},{\"value\":\"1.4\",\"desc\":\"Java 1.4.\"},{\"value\":\"1.5\",\"desc\":\"Java 1.5.\"},{\"value\":\"1.6\",\"desc\":\"Java 1.6.\"},{\"value\":\"1.7\",\"desc\":\"Java 1.7.\"},{\"value\":\"1.8\",\"desc\":\"Java 1.8.\"},{\"value\":\"9\",\"desc\":\"Java 1.9.\\n「Deprecated」\"},{\"value\":\"9\",\"desc\":\"Java 9\"},{\"value\":\"10\",\"desc\":\"Java 10\"},{\"value\":\"11\",\"desc\":\"Java 11\"},{\"value\":\"12\",\"desc\":\"Java 12\"},{\"value\":\"13\",\"desc\":\"Java 13\"}]",
            GsonUtils.toJson(
                psiClassHelper.resolveEnumOrStatic(
                    "com.itangcent.constant.JavaVersion",
                    javaVersionPsiClass,
                    "name"
                )
            )
        )
        assertEquals(
            "[{\"value\":1.5,\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":1.1,\"desc\":\"Java 1.1.\"},{\"value\":1.2,\"desc\":\"Java 1.2.\"},{\"value\":1.3,\"desc\":\"Java 1.3.\"},{\"value\":1.4,\"desc\":\"Java 1.4.\"},{\"value\":1.5,\"desc\":\"Java 1.5.\"},{\"value\":1.6,\"desc\":\"Java 1.6.\"},{\"value\":1.7,\"desc\":\"Java 1.7.\"},{\"value\":1.8,\"desc\":\"Java 1.8.\"},{\"value\":9.0,\"desc\":\"Java 1.9.\\n「Deprecated」\"},{\"value\":9.0,\"desc\":\"Java 9\"},{\"value\":10.0,\"desc\":\"Java 10\"},{\"value\":11.0,\"desc\":\"Java 11\"},{\"value\":12.0,\"desc\":\"Java 12\"},{\"value\":13.0,\"desc\":\"Java 13\"}]",
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
            "[{\"value\":\"0.9\",\"desc\":\"The Java version reported by Android. This is not an official Java version number.\"},{\"value\":\"1.1\",\"desc\":\"Java 1.1.\"},{\"value\":\"1.2\",\"desc\":\"Java 1.2.\"},{\"value\":\"1.3\",\"desc\":\"Java 1.3.\"},{\"value\":\"1.4\",\"desc\":\"Java 1.4.\"},{\"value\":\"1.5\",\"desc\":\"Java 1.5.\"},{\"value\":\"1.6\",\"desc\":\"Java 1.6.\"},{\"value\":\"1.7\",\"desc\":\"Java 1.7.\"},{\"value\":\"1.8\",\"desc\":\"Java 1.8.\"},{\"value\":\"9\",\"desc\":\"Java 1.9.\\n「Deprecated」\"},{\"value\":\"9\",\"desc\":\"Java 9\"},{\"value\":\"10\",\"desc\":\"Java 10\"},{\"value\":\"11\",\"desc\":\"Java 11\"},{\"value\":\"12\",\"desc\":\"Java 12\"},{\"value\":\"13\",\"desc\":\"Java 13\"}]",
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