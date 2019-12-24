package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.lang.jvm.JvmNamedElement
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.itangcent.common.utils.KV
import com.itangcent.common.utils.invokeMethod
import com.itangcent.common.utils.reduceSafely
import com.itangcent.intellij.config.rule.RuleComputer
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.jvm.*
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper.Companion.ELEMENT_OF_COLLECTION
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.util.Magics
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.full.createInstance

abstract class AbstractPsiClassHelper : PsiClassHelper {

    private val resolvedInfo: HashMap<Int, HashMap<Any, Any?>> = HashMap()

    @Inject
    protected val logger: Logger? = null

    @Inject
    protected val duckTypeHelper: DuckTypeHelper? = null

    @Inject
    protected val actionContext: ActionContext? = null

    @Inject
    protected val ruleComputer: RuleComputer? = null

    @Inject
    protected val docHelper: DocHelper? = null

    @Inject
    protected val jvmClassHelper: JvmClassHelper? = null

    @Inject
    protected val psiResolver: PsiResolver? = null

    @PostConstruct
    fun init() {
        val contextSwitchListener: ContextSwitchListener? = ActionContext.getContext()
            ?.instance(ContextSwitchListener::class)
        contextSwitchListener!!.onModuleChange {
            resolvedInfo.clear()
        }
    }

    @Suppress("UNCHECKED_CAST")
    open protected fun <T> getResolvedInfo(key: Any?, option: Int): T? {
        if (key == null) return null
        val value = getCache(option)[key] ?: return null

        return copy(value) as T?
    }

    open protected fun cacheResolvedInfo(key: Any?, option: Int, value: Any?) {
        if (key != null) {
            getCache(option)[key] = value
        }
    }

    open protected fun getCache(option: Int): HashMap<Any, Any?> {
        return resolvedInfo.computeIfAbsent(option) { HashMap() }
    }

    @Suppress("UNCHECKED_CAST")
    override fun copy(obj: Any?): Any? {
        if (obj == null) return null
        if (obj is Collection<*>) {
            try {
                val copyObj = obj::class.createInstance() as MutableCollection<Any?>
                obj.forEach { element -> copy(element)?.let { copyObj.add(it) } }
                return copyObj
            } catch (e: Exception) {
            }
        } else if (obj is Map<*, *>) {
            try {
                val copyObj = obj::class.createInstance() as MutableMap<Any?, Any?>
                obj.forEach { k, v ->
                    copyObj[copy(k)] = copy(v)
                }
                return copyObj
            } catch (e: Exception) {
            }
        }

        if (obj is Cloneable) {
            try {
                return obj.invokeMethod("clone")
            } catch (e: Exception) {
            }
        }
        return obj
    }

    override fun getTypeObject(psiType: PsiType?, context: PsiElement): Any? {
        return getTypeObject(psiType, context, JsonOption.NONE)
    }

    override fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any? {
        actionContext!!.checkStatus()
        if (psiType != null) {
            val resolvedInfo = getResolvedInfo<Any>(psiType, option)
            if (resolvedInfo != null) {
                return resolvedInfo
            }
        }

        if (psiType == null || psiType == PsiType.NULL) return null
        val castTo = tryCastTo(psiType, context)
        when {
            castTo == PsiType.NULL -> return null
            castTo is PsiPrimitiveType -> return PsiTypesUtil.getDefaultValue(castTo)
            isNormalType(castTo) -> return getDefaultValue(castTo)
            castTo is PsiArrayType -> {   //array type
                val deepType = castTo.getDeepComponentType()
                val list = java.util.ArrayList<Any>()
                cacheResolvedInfo(castTo, option, list)//cache
                when {
                    deepType is PsiPrimitiveType -> list.add(PsiTypesUtil.getDefaultValue(deepType))
                    isNormalType(deepType) -> list.add(getDefaultValue(deepType) ?: "")
                    else -> getTypeObject(deepType, context, option)?.let { list.add(it) }
                }
                return copy(list)
            }
            jvmClassHelper!!.isCollection(castTo) -> {   //list type

                val list = java.util.ArrayList<Any>()
                cacheResolvedInfo(castTo, option, list)//cache
                val iterableType = PsiUtil.extractIterableTypeParameter(castTo, false)
                val iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType)
                val classTypeName: String? = iterableClass?.qualifiedName
                when {
                    classTypeName != null && isNormalType(iterableClass) -> getDefaultValue(iterableClass)?.let {
                        list.add(
                            it
                        )
                    }
                    iterableType != null -> getTypeObject(iterableType, context, option)?.let { list.add(it) }
                }
                return copy(list)
            }
            jvmClassHelper.isMap(castTo) -> {   //list type
                val map: HashMap<Any, Any?> = HashMap()
                cacheResolvedInfo(castTo, option, map)//cache
                val keyType = PsiUtil.substituteTypeParameter(castTo, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                val valueType = PsiUtil.substituteTypeParameter(castTo, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                var defaultKey: Any? = null
                if (keyType != null) {
                    defaultKey = if (keyType == psiType) {
                        "nested type"
                    } else {
                        getTypeObject(keyType, context, option)
                    }
                }
                if (defaultKey == null) defaultKey = ""

                var defaultValue: Any? = null
                if (valueType != null) {
                    defaultValue = if (valueType == psiType) {
                        Collections.emptyMap<Any, Any>()
                    } else {
                        getTypeObject(valueType, context, option)
                    }
                }
                if (defaultValue == null) defaultValue = null

                map[defaultKey] = defaultValue

                return copy(map)
            }
            jvmClassHelper.isEnum(psiType) -> {
                return ""//by default use enum name `String`
            }
            else -> {
                val typeCanonicalText = castTo.canonicalText
                if (typeCanonicalText.contains('<') && typeCanonicalText.endsWith('>')) {

                    val tmType = duckTypeHelper!!.resolve(castTo, context)

                    return when {
                        tmType != null -> {
                            val result = getTypeObject(tmType, context, option)
                            cacheResolvedInfo(castTo, option, result)
                            copy(result)
                        }
                        else -> null
                    }
                } else {
                    val paramCls = PsiUtil.resolveClassInClassTypeOnly(castTo) ?: return null
                    if (ruleComputer!!.computer(ClassRuleKeys.TYPE_IS_FILE, paramCls) == true) {
                        cacheResolvedInfo(castTo, option, Magics.FILE_STR)//cache
                        return Magics.FILE_STR
                    }
                    return try {
                        val result = getFields(paramCls, option)
                        cacheResolvedInfo(castTo, option, result)
                        copy(result)
                    } catch (e: Throwable) {
                        logger!!.error("error to getTypeObject:$psiType")
                        logger.trace(ExceptionUtils.getStackTrace(e))
                        null
                    }
                }
            }
        }
    }

    override fun getFields(psiClass: PsiClass?): KV<String, Any?> {
        return getFields(psiClass, JsonOption.NONE)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getFields(psiClass: PsiClass?, option: Int): KV<String, Any?> {
        actionContext!!.checkStatus()

        val resourcePsiClass = if (JsonOption.needComment(option)) {
            psiClass?.let { getResourceClass(it) }
        } else {
            psiClass
        }

        if (resourcePsiClass != null) {
            val resolvedInfo = getResolvedInfo<Any>(resourcePsiClass, option)
            if (resolvedInfo != null) {
                return resolvedInfo as KV<String, Any?>
            }
        }

        val kv: KV<String, Any?> = KV.create()

        if (resourcePsiClass != null) {
            cacheResolvedInfo(resourcePsiClass, option, kv)//cache

            beforeParseClass(resourcePsiClass, option, kv)

            foreachField(resourcePsiClass, option) { fieldName, fieldType, fieldOrMethod ->

                if (!beforeParseFieldOrMethod(fieldType, fieldOrMethod, resourcePsiClass, option, kv)) {
                    return@foreachField
                }
                val name = fieldName()
                if (!kv.contains(name)) {

                    parseFieldOrMethod(name, fieldType, fieldOrMethod, resourcePsiClass, option, kv)

                    afterParseFieldOrMethod(name, fieldType, fieldOrMethod, resourcePsiClass, option, kv)
                }
            }

            afterParseClass(resourcePsiClass, option, kv)
        }

        return copy(kv) as KV<String, Any?>
    }

    protected open fun getTypeObject(duckType: DuckType?, context: PsiElement, option: Int): Any? {
        actionContext!!.checkStatus()

        val resolvedInfo = getResolvedInfo<Any>(duckType, option)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        if (duckType == null) return null

        if (duckType is ArrayDuckType) {
            val list = ArrayList<Any>()
            cacheResolvedInfo(duckType, option, list)
            getTypeObject(duckType.componentType(), context, option)?.let { list.add(it) }
            return copy(list)
        }

        if (duckType is SingleDuckType) {
            return getTypeObject(duckType as SingleDuckType, context, option)
        }
        return null
    }

    protected open fun getTypeObject(clsWithParam: SingleDuckType?, context: PsiElement, option: Int): Any? {
        actionContext!!.checkStatus()

        if (clsWithParam != null) {
            val resolvedInfo = getResolvedInfo<Any>(clsWithParam, option)
            if (resolvedInfo != null) {
                return resolvedInfo
            }
        }

        if (clsWithParam == null) return null
        val psiClass = if (JsonOption.needComment(option)) {
            getResourceClass(clsWithParam.psiClass())
        } else {
            clsWithParam.psiClass()
        }
        val typeOfCls = PsiTypesUtil.getClassType(psiClass)

        val type = tryCastTo(typeOfCls, psiClass)
        if (type is PsiPrimitiveType) {       //primitive Type
            return PsiTypesUtil.getDefaultValueOfType(type)
        } else {    //reference Type
            when {
                isNormalType(type) -> //normal Type
                    return getDefaultValue(type)
                type is PsiArrayType -> {   //array type
                    val deepType = type.getDeepComponentType()
                    val list = ArrayList<Any>()
                    cacheResolvedInfo(clsWithParam, option, list)
                    when {
                        deepType is PsiPrimitiveType -> list.add(PsiTypesUtil.getDefaultValue(deepType))
                        isNormalType(deepType) -> getDefaultValue(deepType)?.let {
                            list.add(
                                it
                            )
                        }
                        else -> getTypeObject(
                            duckTypeHelper!!.ensureType(deepType, clsWithParam.genericInfo),
                            context,
                            option
                        )?.let { list.add(it) }
                    }
                    return copy(list)
                }
                jvmClassHelper!!.isCollection(type) -> {   //list type
                    val list = ArrayList<Any>()

                    cacheResolvedInfo(clsWithParam, option, list)
                    val iterableType = PsiUtil.extractIterableTypeParameter(type, false)
                    if (iterableType == null) {//maybe generic type
                        val realIterableType = clsWithParam.genericInfo?.get(ELEMENT_OF_COLLECTION)
                        if (realIterableType != null) {
                            getTypeObject(realIterableType, context, option)?.let { list.add(it) }
                            return copy(list)
                        }
                    }

                    if (iterableType != null) {
                        when {
                            isNormalType(iterableType) -> getDefaultValue(iterableType)?.let {
                                list.add(
                                    it
                                )
                            }
                            else -> getTypeObject(
                                duckTypeHelper!!.ensureType(
                                    iterableType,
                                    clsWithParam.genericInfo
                                ), context, option
                            )?.let { list.add(it) }
                        }
                    }

                    return copy(list)
                }
                jvmClassHelper.isMap(type) -> {
                    //list type
                    val map: HashMap<Any, Any?> = HashMap()
                    cacheResolvedInfo(clsWithParam, option, map)
                    val keyType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                    val valueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                    var defaultKey: Any? = null

                    if (keyType == null) {
                        val realKeyType = clsWithParam.genericInfo?.get(StandardJvmClassHelper.KEY_OF_MAP)
                        defaultKey = getTypeObject(realKeyType, context, option)
                    }

                    if (defaultKey == null) {
                        defaultKey = if (keyType != null) {
                            getTypeObject(
                                duckTypeHelper!!.ensureType(keyType, clsWithParam.genericInfo),
                                context,
                                option
                            )
                        } else {
                            ""
                        }
                    }

                    var defaultValue: Any? = null

                    if (valueType == null) {
                        val realValueType = clsWithParam.genericInfo?.get(StandardJvmClassHelper.VALUE_OF_MAP)
                        defaultValue = getTypeObject(realValueType, context, option)
                    }
                    if (defaultValue == null) {
                        defaultValue = if (valueType == null) {
                            null
                        } else {
                            getTypeObject(
                                duckTypeHelper!!.ensureType(valueType, clsWithParam.genericInfo),
                                context,
                                option
                            )
                        }
                    }

                    if (defaultKey != null) {
                        map[defaultKey] = defaultValue
                    }

                    return copy(map)
                }
                jvmClassHelper.isEnum(type) -> {
                    return ""//by default use enum name `String`
                }
                else -> //class type
                {
                    val clsOfType = jvmClassHelper.resolveClassInType(type) ?: return null

                    if (clsOfType is PsiTypeParameter) {
                        val typeParams = clsWithParam.genericInfo
                        if (typeParams != null) {
                            val realType = typeParams[clsOfType.name]
                            if (realType != null) {
                                return getTypeObject(realType, context, option)
                            }
                        }
                    }

                    if (ruleComputer!!.computer(ClassRuleKeys.TYPE_IS_FILE, clsOfType) == true) {
                        return Magics.FILE_STR
                    }
                    return getFields(clsWithParam, option)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun getFields(clsWithParam: SingleDuckType, option: Int): KV<String, Any?> {
        actionContext!!.checkStatus()

        val resolvedInfo = getResolvedInfo<Any>(clsWithParam, option)
        if (resolvedInfo != null) {
            return resolvedInfo as KV<String, Any?>
        }

        val psiClass = if (JsonOption.needComment(option)) {
            getResourceClass(clsWithParam.psiClass())
        } else {
            clsWithParam.psiClass()
        }
        val kv: KV<String, Any?> = KV.create()
        cacheResolvedInfo(clsWithParam, option, kv)
        beforeParseType(psiClass, clsWithParam, option, kv)

        foreachField(psiClass, option) { fieldName, fieldType, fieldOrMethod ->

            if (!beforeParseFieldOrMethod(
                    fieldType,
                    fieldOrMethod,
                    psiClass,
                    clsWithParam,
                    option,
                    kv
                )
            ) {
                return@foreachField
            }

            val name = fieldName()
            if (!kv.contains(name)) {

                parseFieldOrMethod(name, fieldType, fieldOrMethod, psiClass, clsWithParam, option, kv)

                afterParseFieldOrMethod(name, fieldType, fieldOrMethod, psiClass, clsWithParam, option, kv)
            }
        }

        afterParseType(psiClass, clsWithParam, option, kv)

        return copy(kv) as KV<String, Any?>
    }

    protected open fun foreachField(
        psiClass: PsiClass,
        option: Int,
        handle: (name: () -> String, type: PsiType, fieldOrMethod: PsiElement) -> Unit
    ) {
        actionContext!!.checkStatus()

        val readGetter = JsonOption.readGetter(option)
        var fieldNames: HashSet<String>? = null
        if (readGetter) {
            fieldNames = HashSet()
        }

        for (field in jvmClassHelper!!.getAllFields(psiClass)) {
            if (jvmClassHelper.isStaticFinal(field)) {
                continue
            }

            if (!jvmClassHelper.isAccessibleField(field)) {
                continue
            }

            if (!readGetter || fieldNames!!.add(field.name)) {
                handle({ getJsonFieldName(field) }, field.type, field)
            }
        }

        if (JsonOption.readGetter(option)) {
            for (method in psiClass.allMethods) {
                val methodName = method.name
                if (jvmClassHelper!!.isBasicMethod(methodName)) continue
                val propertyName = propertyNameOfGetter(methodName) ?: continue
                if (readGetter && !fieldNames!!.add(propertyName)) continue
                if (method.isConstructor) continue
                if (method.parameters.isNotEmpty()) continue
                if (method.hasModifierProperty(PsiModifier.STATIC)) continue
                if (!method.hasModifierProperty(PsiModifier.PUBLIC)) continue

                method.returnType?.let { handle({ propertyName }, it, method) }
            }
        }
        fieldNames?.clear()
    }

    protected fun propertyNameOfGetter(methodName: String): String? {
        return when {
            methodName.startsWith("get") -> methodName.removePrefix("get")
            methodName.startsWith("is") -> methodName.removePrefix("is")
            else -> null
        }?.decapitalize()?.removeSuffix("()")
    }

    protected open fun tryCastTo(psiType: PsiType, context: PsiElement): PsiType {
        return psiType
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolveEnumOrStatic(
        classNameWithProperty: String,
        context: PsiElement,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>? {
        actionContext!!.checkStatus()
        val clsName: String?
        var property: String? = null
        val cls: PsiClass?

        if (classNameWithProperty.contains("#")) {
            clsName = classNameWithProperty.substringBefore("#")
            property = classNameWithProperty.substringAfter("#")
                .trim()
                .removeSuffix("()")
        } else {
            clsName = classNameWithProperty.trim().removeSuffix("()")
        }
        cls = psiResolver!!.resolveClass(clsName, context)?.let { getResourceClass(it) }
        return resolveEnumOrStatic(cls, property, defaultPropertyName)
    }


    @Suppress("UNCHECKED_CAST")
    override fun resolveEnumOrStatic(
        cls: PsiClass?,
        property: String?,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>? {
        if (cls == null) return null

        val options: ArrayList<HashMap<String, Any?>> = ArrayList()

        if (cls.isEnum) {
            val enumConstants = parseEnumConstant(cls)

            var valProperty = property ?: defaultPropertyName
            if (!valProperty.isBlank()) {
                val candidateProperty = propertyNameOfGetter(valProperty)
                if (candidateProperty != null && valProperty != candidateProperty) {
                    if (!jvmClassHelper!!.getAllFields(cls)
                            .filter { it.name == valProperty }
                            .any() && jvmClassHelper.getAllFields(cls)
                            .filter { it.name == candidateProperty }
                            .any()
                    ) {
                        valProperty = candidateProperty
                    }
                }
            }

            for (enumConstant in enumConstants) {
                val mappedVal = (enumConstant["params"] as HashMap<String, Any?>?)
                    ?.get(valProperty) ?: continue

                var desc = enumConstant["desc"]
                if (desc == null) {
                    desc = (enumConstant["params"] as HashMap<String, Any?>?)!!
                        .filterKeys { k -> k != valProperty }
                        .map { entry -> entry.value.toString() }
                        .reduceSafely { s1, s2 -> "$s1 $s2" }
                        ?.trim()
                }
                if (desc == null || (desc is String && desc.isBlank())) {
                    desc = enumConstant["name"]
                }
                options.add(
                    KV.create<String, Any?>()
                        .set("value", mappedVal)
                        .set("desc", desc)
                )

            }

        } else {
            val constants = parseStaticFields(cls)

            if (!property.isNullOrBlank()) {
                for (constant in constants) {
                    val name = constant["name"] as String

                    if (name != property) continue
                    val mappedVal = constant["value"]
                    val desc = constant["desc"] ?: constant["name"]

                    options.add(
                        KV.create<String, Any?>()
                            .set("value", mappedVal)
                            .set("desc", desc)
                    )
                    break
                }

                if (options.isNotEmpty()) {
                    return options
                }
            }

            for (constant in constants) {
                val mappedVal = constant["value"]
                val desc = constant["desc"] ?: constant["name"]
                options.add(
                    KV.create<String, Any?>()
                        .set("value", mappedVal)
                        .set("desc", desc)
                )
            }

        }
        return options
    }

    protected open val staticResolvedInfo: HashMap<PsiClass, List<Map<String, Any?>>> = HashMap()

    override fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>> {
        actionContext!!.checkStatus()
        val resourceClass = getResourceClass(psiClass)
        if (staticResolvedInfo.containsKey(resourceClass)) {
            return staticResolvedInfo[resourceClass]!!
        }
        val res = ArrayList<Map<String, Any?>>()
        val filter: (PsiField) -> Boolean = if (resourceClass.isInterface) {
            { field -> jvmClassHelper!!.isStaticFinal(field) }
        } else {
            { field -> jvmClassHelper!!.isPublicStaticFinal(field) }
        }
        for (field in jvmClassHelper!!.getAllFields(resourceClass)) {

            if (!filter(field)) {
                continue
            }

            if (ruleComputer!!.computer(ClassRuleKeys.CONSTANT_FIELD_IGNORE, field) == true) {
                continue
            }

            val value = field.computeConstantValue() ?: continue

            res.add(
                KV.create<String, Any?>()
                    .set("name", field.name)
                    .set("value", value.toString())
                    .set("desc", getAttrOfField(field))
            )
        }
        staticResolvedInfo[resourceClass] = res
        return res
    }

    override fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>> {
        actionContext!!.checkStatus()
        val sourcePsiClass = getResourceClass(psiClass)
        if (!sourcePsiClass.isEnum) return ArrayList()

        if (staticResolvedInfo.containsKey(sourcePsiClass)) {
            return staticResolvedInfo[sourcePsiClass]!!
        }

        val res = ArrayList<Map<String, Any?>>()
        for (field in jvmClassHelper!!.getAllFields(sourcePsiClass)) {

            val value = field.computeConstantValue() ?: continue

            if (value !is PsiEnumConstant) continue

            val constant = HashMap<String, Any?>()
            val params = HashMap<String, Any?>()
            val construct = value.resolveConstructor()
            val expressions = value.argumentList?.expressions
            val parameters = construct?.parameterList?.parameters
            if (expressions != null && parameters != null && parameters.isNotEmpty()) {
                if (parameters.last().isVarArgs) {

                    for (index in 0 until parameters.size - 1) {
                        params[parameters[index].name!!] = resolveExpr(expressions[index])
                    }
                    try {
                        //resolve varArgs
                        val lastVarArgParam: ArrayList<Any?> = ArrayList(1)
                        params[parameters[parameters.size - 1].name!!] = lastVarArgParam
                        for (index in parameters.size - 1..expressions.size) {
                            lastVarArgParam.add(resolveExpr(expressions[index]))
                        }
                    } catch (e: Throwable) {
                    }
                }

                for ((index, parameter) in parameters.withIndex()) {
                    try {
                        params[parameter.name!!] = resolveExpr(expressions[index])
                    } catch (e: Throwable) {
                    }
                }
            }
            constant["params"] = params
            constant["name"] = field.name
            constant["desc"] = getAttrOfField(field)
            res.add(constant)
        }

        staticResolvedInfo[sourcePsiClass] = res
        return res
    }

    protected fun resolveExpr(expOrEle: Any): Any? {
        if (expOrEle is PsiExpression) {
            return resolveExpr(expOrEle)
        } else if (expOrEle is PsiElement) {
            return resolveExpr(expOrEle)
        }
        return expOrEle.toString()
    }

    protected open fun resolveExpr(psiExpression: PsiExpression): Any? {
        when (psiExpression) {
            is PsiLiteralExpression -> return psiExpression.value
            is PsiReferenceExpression -> {
                val value = psiExpression.resolve()
                if (value != null) {
                    return resolveExpr(value)
                }
            }
            is JvmNamedElement -> return psiExpression.name
        }
        return psiExpression.text
    }

    protected open fun resolveExpr(psiElement: PsiElement): Any? {
        when (psiElement) {
            is PsiVariable -> {
                val constantValue = psiElement.computeConstantValue()
                if (constantValue != null && constantValue != psiElement) {
                    return resolveExpr(constantValue)
                }
                return psiElement.name
            }
            is JvmNamedElement -> return psiElement.name
        }
        return psiElement.text
    }

    override fun getAttrOfField(field: PsiField): String? {

        var result: String? = null

        val attrInDoc = docHelper!!.getAttrOfDocComment(field)
        if (StringUtils.isNotBlank(attrInDoc)) {
            result = (result ?: "") + attrInDoc
        }

        var fieldText = field.text
        if (fieldText.contains("//")) {
            fieldText = fieldText.trim()

            val lines = fieldText.split('\r', '\n')
            var innerDoc = ""
            for (line in lines) {
                if (StringUtils.isBlank(line)) {
                    continue
                }
                //ignore region/endregion
                if (!line.contains("//")
                    || line.startsWith("//region")
                    || line.startsWith("//endregion")
                ) {
                    continue
                }
                if (innerDoc.isNotEmpty()) innerDoc += ","
                innerDoc += line.substringAfter("//").trim()
            }
            if (StringUtils.isNotBlank(innerDoc)) {
                result = (result ?: "") + innerDoc
            }
        }

        return result
    }

    override fun isNormalType(psiType: PsiType): Boolean {
        return jvmClassHelper!!.isNormalType(psiType.canonicalText)
    }

    override fun isNormalType(psiClass: PsiClass): Boolean {
        return jvmClassHelper!!.isNormalType(psiClass.qualifiedName ?: return false)
    }

    override fun getDefaultValue(psiType: PsiType): Any? {
        return jvmClassHelper!!.getDefaultValue(psiType.canonicalText)
    }

    override fun getDefaultValue(psiClass: PsiClass): Any? {
        return jvmClassHelper!!.getDefaultValue(psiClass.qualifiedName ?: return null)
    }

    override fun unboxArrayOrList(psiType: PsiType): PsiType {
        when {
            psiType is PsiPrimitiveType -> return psiType
            isNormalType(psiType) -> return psiType
            psiType is PsiArrayType -> {   //array type
                return psiType.getDeepComponentType()
            }
            jvmClassHelper!!.isCollection(psiType) -> {   //list type
                val iterableType = PsiUtil.extractIterableTypeParameter(psiType, false)
                return when {
                    iterableType != null -> iterableType
                    else -> PsiType.NULL
                }
            }
            else -> {
                return psiType
            }
        }
    }

    override fun getJsonFieldName(psiField: PsiField): String {
        return psiField.name
    }

    open fun getResourceClass(psiClass: PsiClass): PsiClass {
        return psiClass
    }

    open fun beforeParseClass(psiClass: PsiClass, option: Int, kv: KV<String, Any?>) {

    }

    open fun afterParseClass(psiClass: PsiClass, option: Int, kv: KV<String, Any?>) {

    }

    //return false to ignore current fieldOrMethod
    open fun beforeParseFieldOrMethod(
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        option: Int,
        kv: KV<String, Any?>
    ): Boolean {
        return true
    }

    open fun parseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        option: Int,
        kv: KV<String, Any?>
    ) {
        if (fieldType is PsiPrimitiveType) {       //primitive Type
            kv[fieldName] = PsiTypesUtil.getDefaultValue(fieldType)
        } else {    //reference Type

            if (fieldType == PsiType.NULL) {
                kv[fieldName] = null
                return
            }

            val type = tryCastTo(fieldType, resourcePsiClass)
            when {
                isNormalType(type) -> //normal Type
                    kv[fieldName] = getDefaultValue(type)
                type is PsiArrayType -> {   //array type
                    val deepType = type.getDeepComponentType()

                    val list = ArrayList<Any>()
                    when {
                        deepType is PsiPrimitiveType -> list.add(PsiTypesUtil.getDefaultValue(deepType))
                        isNormalType(deepType) -> getDefaultValue(deepType)?.let {
                            list.add(
                                it
                            )
                        }
                        else -> getTypeObject(deepType, fieldOrMethod, option)?.let { list.add(it) }
                    }
                    kv[fieldName] = list
                }
                jvmClassHelper!!.isCollection(type) -> {   //list type
                    val iterableType = PsiUtil.extractIterableTypeParameter(type, false)
                    val iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType)
                    val list = ArrayList<Any>()
                    when {
                        iterableType != null && isNormalType(iterableType) -> getDefaultValue(
                            iterableType
                        )?.let { list.add(it) }
                        iterableClass == resourcePsiClass -> list.add(Collections.emptyMap<Any, Any>())
                        else -> getTypeObject(iterableType, fieldOrMethod, option)?.let { list.add(it) }
                    }
                    kv[fieldName] = list
                }
                jvmClassHelper.isMap(type) -> {   //map type
                    val map: HashMap<Any, Any?> = HashMap()
                    val keyType =
                        PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                    val valueType =
                        PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                    var defaultKey: Any? = null
                    if (keyType != null) {
                        defaultKey = when {
                            keyType == type -> "nested type"
                            else -> getTypeObject(keyType, fieldOrMethod, option)
                        }
                    }
                    if (defaultKey == null) defaultKey = ""

                    var defaultValue: Any? = null
                    if (valueType != null) {
                        defaultValue = when {
                            valueType == type -> Collections.emptyMap<Any, Any>()
                            else -> getTypeObject(valueType, fieldOrMethod, option)
                        }
                    }
                    if (defaultValue == null) defaultValue = null

                    map[defaultKey] = defaultValue

                    kv[fieldName] = map
                }
                jvmClassHelper.isEnum(type) -> {
                    kv[fieldName] = ""//by default use enum name `String`
                }
                else -> //class type
                {
                    val clsOfType = jvmClassHelper.resolveClassInType(type)
                    if (clsOfType != null && clsOfType != resourcePsiClass) {
                        if (ruleComputer!!.computer(ClassRuleKeys.TYPE_IS_FILE, clsOfType) == true) {
                            kv[fieldName] = Magics.FILE_STR
                        } else {
                            kv[fieldName] = getFields(clsOfType, option)
                        }
                    }
                }

            }
        }
    }

    open fun afterParseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        option: Int,
        kv: KV<String, Any?>
    ) {

    }

    open fun beforeParseType(psiClass: PsiClass, duckType: SingleDuckType, option: Int, kv: KV<String, Any?>) {

    }

    open fun afterParseType(psiClass: PsiClass, duckType: SingleDuckType, option: Int, kv: KV<String, Any?>) {

    }

    //return false to ignore current fieldOrMethod
    open fun beforeParseFieldOrMethod(
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        duckType: SingleDuckType,
        option: Int,
        kv: KV<String, Any?>
    ): Boolean {
        return true
    }


    open fun parseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        duckType: SingleDuckType,
        option: Int,
        kv: KV<String, Any?>
    ) {

        if (fieldType is PsiPrimitiveType) {//primitive Type
            kv[fieldName] = PsiTypesUtil.getDefaultValue(fieldType)
            return
        }

        //reference Type
        if (isNormalType(fieldType)) {//normal Type
            kv[fieldName] = getDefaultValue(fieldType)
            return
        }
        val clsOfType = jvmClassHelper!!.resolveClassInType(fieldType)

        if (clsOfType is PsiTypeParameter) {
            val typeParams = duckType.genericInfo
            if (typeParams != null) {
                val realType = typeParams[clsOfType.name]
                if (realType != null) {
                    kv[fieldName] = getTypeObject(realType, fieldOrMethod, option)
                    return
                }
            }
        }

        val type = tryCastTo(fieldType, fieldOrMethod)
        when {
            type is PsiArrayType -> {   //array type
                val deepType = type.getDeepComponentType()
                val list = ArrayList<Any>()

                when {
                    deepType is PsiPrimitiveType -> list.add(PsiTypesUtil.getDefaultValue(deepType))
                    isNormalType(deepType) -> getDefaultValue(deepType)?.let {
                        list.add(
                            it
                        )
                    }
                    else -> getTypeObject(
                        duckTypeHelper!!.ensureType(deepType, duckType.genericInfo),
                        resourcePsiClass,
                        option
                    )?.let { list.add(it) }
                }
                kv[fieldName] = list
            }
            jvmClassHelper.isCollection(type) -> {   //list type
                val list = ArrayList<Any>()
                val iterableType = PsiUtil.extractIterableTypeParameter(type, false)

                if (iterableType != null) {
                    when {
                        isNormalType(iterableType) -> list.add(
                            getDefaultValue(
                                iterableType
                            )!!
                        )
                        iterableType.canonicalText == resourcePsiClass.qualifiedName -> list.add(Collections.emptyMap<Any, Any>())
                        else -> getTypeObject(
                            duckTypeHelper!!.ensureType(
                                iterableType,
                                duckType.genericInfo
                            ), resourcePsiClass, option
                        )?.let { list.add(it) }
                    }
                }
                kv[fieldName] = list
            }
            jvmClassHelper.isMap(type) -> {   //list type
                val map: HashMap<Any, Any?> = HashMap()
                val keyType =
                    PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                val valueType =
                    PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                var defaultKey: Any? = null
                if (keyType != null) defaultKey = getTypeObject(
                    duckTypeHelper!!.ensureType(keyType, duckType.genericInfo),
                    resourcePsiClass,
                    option
                )
                if (defaultKey == null) defaultKey = ""

                var defaultValue: Any? = null
                if (valueType != null) defaultValue = getTypeObject(
                    duckTypeHelper!!.ensureType(valueType, duckType.genericInfo),
                    resourcePsiClass,
                    option
                )
                if (defaultValue == null) defaultValue = null

                map[defaultKey] = defaultValue

                kv[fieldName] = map
            }
            jvmClassHelper.isEnum(type) -> {
                kv[fieldName] = ""//by default use enum name `String`
            }
            else -> //class type
            {
                if (clsOfType != null && clsOfType != resourcePsiClass) {
                    if (ruleComputer!!.computer(ClassRuleKeys.TYPE_IS_FILE, clsOfType) == true) {
                        kv[fieldName] = Magics.FILE_STR
                    } else {
                        kv[fieldName] = getFields(clsOfType, option)
                    }
                }
            }
        }

    }

    open fun afterParseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        duckType: SingleDuckType,
        option: Int,
        kv: KV<String, Any?>
    ) {
        if (jvmClassHelper!!.isEnum(fieldType)) {

        }

    }
}

object JsonOption {
    const val NONE = 0b0000//None additional options
    const val READ_COMMENT = 0b0001//try find comments
    const val READ_GETTER = 0b0010//Try to find the available getter method as property
    const val ALL = READ_COMMENT or READ_GETTER//All additional options

    fun needComment(flag: Int): Boolean {
        return (flag and READ_COMMENT) != 0
    }

    fun readGetter(flag: Int): Boolean {
        return (flag and READ_GETTER) != 0
    }

}
