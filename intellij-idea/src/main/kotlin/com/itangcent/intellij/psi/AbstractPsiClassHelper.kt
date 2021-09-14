package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.itangcent.common.utils.*
import com.itangcent.intellij.config.rule.RuleComputer
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.jvm.*
import com.itangcent.intellij.jvm.adapt.*
import com.itangcent.intellij.jvm.duck.ArrayDuckType
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.duck.SingleUnresolvedDuckType
import com.itangcent.intellij.jvm.element.ExplicitClass
import com.itangcent.intellij.jvm.element.ExplicitElement
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper.Companion.ELEMENT_OF_COLLECTION
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.psi.JsonOption.has
import com.itangcent.intellij.util.Magics
import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayDeque

abstract class AbstractPsiClassHelper : PsiClassHelper {

    protected val resolvedInfo: HashMap<String, Any?> = LinkedHashMap()

    @Inject
    protected val logger: Logger? = null

    @Inject
    protected val duckTypeHelper: DuckTypeHelper? = null

    @Inject
    protected val actionContext: ActionContext? = null

    @Inject
    protected lateinit var ruleComputer: RuleComputer

    @Inject
    protected val docHelper: DocHelper? = null

    @Inject
    protected val jvmClassHelper: JvmClassHelper? = null

    @Inject
    protected val psiResolver: PsiResolver? = null

    @PostConstruct
    fun init() {
        ActionContext.getContext()
            ?.instance(ContextSwitchListener::class)
            ?.onModuleChange {
                resolvedInfo.clear()
            }
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun <T> getResolvedInfo(key: String): Any? {
        return resolvedInfo[key]
    }

    protected open fun cacheResolvedInfo(key: String, value: Any?) {
        resolvedInfo[key] = value
    }

    protected open fun groupKey(context: PsiElement?, option: Int): String {
        val group = context?.let { ruleComputer.computer(ClassRuleKeys.JSON_GROUP, context) }
        return if (group.isNullOrBlank()) {
            option.toString()
        } else {
            group + option.toString()
        }
    }

    @Deprecated(
        message = "copy is deprecated and will likely be removed in a future release.",
        replaceWith = ReplaceWith("obj.copy()", "com.itangcent.common.utils.copy")
    )
    @Suppress("UNCHECKED_CAST")
    override fun copy(obj: Any?): Any? = obj.copy()

    override fun getTypeObject(psiType: PsiType?, context: PsiElement): Any? {
        return doGetTypeObject(psiType, context).unwrapped()
    }

    override fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any? {
        return doGetTypeObject(psiType, context, option).unwrapped()
    }

    override fun getTypeObject(duckType: DuckType?, context: PsiElement): Any? {
        return doGetTypeObject(duckType, context).unwrapped()
    }

    override fun getTypeObject(duckType: DuckType?, context: PsiElement, option: Int): Any? {
        return doGetTypeObject(duckType, context, option).unwrapped()
    }

    fun doGetTypeObject(psiType: PsiType?, context: PsiElement): Any? {
        return doGetTypeObject(psiType, context, JsonOption.NONE)
    }

    fun doGetTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any? {
        actionContext!!.checkStatus()
        if (psiType == null || psiType == PsiType.NULL) return null
        val cacheKey = psiType.canonicalText + "@" + groupKey(context, option)

        val resolvedInfo = getResolvedInfo<Any>(cacheKey)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        val castTo = tryCastTo(psiType, context)
        when {
//            castTo == PsiType.NULL -> return null
//            castTo is PsiPrimitiveType -> return PsiTypesUtil.getDefaultValue(castTo)
            isNormalType(castTo) -> return getDefaultValue(castTo)
            castTo is PsiArrayType -> {   //array type
                val deepType = castTo.getDeepComponentType()
                val list = ArrayList<Any>()
                cacheResolvedInfo(cacheKey, list)//cache
                when {
                    deepType is PsiPrimitiveType -> list.add(PsiTypesUtil.getDefaultValue(deepType))
                    isNormalType(deepType) -> list.add(getDefaultValue(deepType) ?: "")
                    else -> doGetTypeObject(deepType, context, option)?.let { list.add(it) }
                }
                return list.delay()
            }
            jvmClassHelper!!.isCollection(castTo) -> {   //list type
                val list = ArrayList<Any>()
                cacheResolvedInfo(cacheKey, list)//cache
                val iterableType = PsiUtil.extractIterableTypeParameter(castTo, false)
                val iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType)
                val classTypeName: String? = iterableClass?.qualifiedName
                when {
                    classTypeName != null && isNormalType(iterableClass) -> getDefaultValue(iterableClass)?.let {
                        list.add(
                            it
                        )
                    }
                    iterableType != null -> doGetTypeObject(iterableType, context, option)?.let { list.add(it) }
                }
                return list.delay()
            }
            jvmClassHelper.isMap(castTo) -> {   //list type
                val map: HashMap<Any, Any?> = HashMap()
                cacheResolvedInfo(cacheKey, map)//cache
                val keyType = PsiUtil.substituteTypeParameter(castTo, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                val valueType = PsiUtil.substituteTypeParameter(castTo, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                var defaultKey: Any? = null
                if (keyType != null) {
                    defaultKey = if (keyType == psiType) {
                        "nested type"
                    } else {
                        doGetTypeObject(keyType, context, option)
                    }
                }
                if (defaultKey == null) defaultKey = ""

                var defaultValue: Any? = null
                if (valueType != null) {
                    defaultValue = if (valueType == psiType) {
                        Collections.emptyMap<Any, Any>()
                    } else {
                        doGetTypeObject(valueType, context, option)
                    }
                }
                if (defaultValue == null) defaultValue = null

                map[defaultKey] = defaultValue

                return map.delay()
            }
            jvmClassHelper.isEnum(psiType) -> {
                return parseEnum(psiType, context, option)
            }
            else -> {
                val typeCanonicalText = castTo.canonicalText
                if (typeCanonicalText.contains('<') && typeCanonicalText.endsWith('>')) {

                    val duckType = duckTypeHelper!!.resolve(castTo, context)

                    return when {
                        duckType != null -> {
                            val result = doGetTypeObject(duckType, context, option)
                            cacheResolvedInfo(cacheKey, result)
                            result.delay()
                        }
                        else -> null
                    }
                } else {
                    val paramCls = PsiUtil.resolveClassInClassTypeOnly(castTo) ?: return null
                    if (ruleComputer.computer(ClassRuleKeys.TYPE_IS_FILE, paramCls) == true) {
                        cacheResolvedInfo(cacheKey, Magics.FILE_STR)//cache
                        return Magics.FILE_STR
                    }
                    return try {
                        val result = doGetFields(paramCls, option)
                        cacheResolvedInfo(cacheKey, result)
                        result.delay()
                    } catch (e: Throwable) {
                        logger!!.error("error to getTypeObject:$psiType")
                        logger.trace(ExceptionUtils.getStackTrace(e))
                        null
                    }
                }
            }
        }
    }

    fun doGetTypeObject(duckType: DuckType?, context: PsiElement): Any? {
        return doGetTypeObject(duckType, context, JsonOption.NONE)
    }

    fun doGetTypeObject(duckType: DuckType?, context: PsiElement, option: Int): Any? {
        actionContext!!.checkStatus()

        if (duckType == null) return null

        val cacheKey = duckType.canonicalText() + "@" + groupKey(context, option)

        val resolvedInfo = getResolvedInfo<Any>(cacheKey)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        val type = tryCastTo(duckType, context)

        if (type is ArrayDuckType) {
            val list = ArrayList<Any>()
            cacheResolvedInfo(cacheKey, list)
            doGetTypeObject(type.componentType(), context, option)?.let { list.add(it) }
            return list.delay()
        }

        if (type is SingleDuckType) {
            return getTypeObject(type as SingleDuckType, context, option)
        }

        if (type is SingleUnresolvedDuckType) {
            return doGetTypeObject(type.psiType(), context, option)
        }

        return null
    }

    override fun getFields(psiClass: PsiClass?): KV<String, Any?> {
        return doGetFields(psiClass).unwrapped().asKV()
    }

    private fun doGetFields(psiClass: PsiClass?): Any? {
        return doGetFields(psiClass, JsonOption.NONE)
    }

    override fun getFields(psiClass: PsiClass?, context: PsiElement?): KV<String, Any?> {
        return doGetFields(psiClass, context).unwrapped().asKV()
    }

    fun doGetFields(psiClass: PsiClass?, context: PsiElement?): Any? {
        return doGetFields(psiClass, context, JsonOption.NONE)
    }

    override fun getFields(psiClass: PsiClass?, option: Int): KV<String, Any?> {
        return doGetFields(psiClass, option).unwrapped().asKV()
    }

    fun doGetFields(psiClass: PsiClass?, option: Int): Any? {
        return doGetFields(psiClass, psiClass, option)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getFields(psiClass: PsiClass?, context: PsiElement?, option: Int): KV<String, Any?> {
        return doGetFields(psiClass, context, option).unwrapped().asKV()
    }

    fun doGetFields(psiClass: PsiClass?, context: PsiElement?, option: Int): Any? {
        actionContext!!.checkStatus()

        val cacheKey = psiClass?.qualifiedName + "@" + groupKey(context, option)

        val resourcePsiClass = if (option.has(JsonOption.READ_COMMENT)) {
            psiClass?.let { getResourceClass(it) }
        } else {
            psiClass
        }

        if (resourcePsiClass != null) {
            val resolvedInfo = getResolvedInfo<Any>(cacheKey)
            if (resolvedInfo != null) {
                return resolvedInfo
            }
        }

        val kv: KV<String, Any?> = KV()

        if (resourcePsiClass != null) {
            cacheResolvedInfo(cacheKey, kv)//cache

            beforeParseClass(resourcePsiClass, option, kv)

            val explicitClass = duckTypeHelper!!.explicit(resourcePsiClass)
            foreachField(explicitClass, option) { fieldName, fieldType, fieldOrMethod ->

                if (!beforeParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, explicitClass, option, kv)) {
                    onIgnoredParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, explicitClass, option, kv)
                    return@foreachField
                }

                if (!kv.contains(fieldName)) {

                    parseFieldOrMethod(fieldName, fieldType, fieldOrMethod, explicitClass, option, kv)

                    afterParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, explicitClass, option, kv)
                }
            }

            afterParseClass(resourcePsiClass, option, kv)
        }

        return kv.delay()
    }

    open fun parseEnum(psiType: PsiType?, context: PsiElement, option: Int): Any? {
        return ""//by default use enum name `String`
    }

    /**
     * Get typeObject of SingleDuckType
     */
    protected open fun getTypeObject(duckType: SingleDuckType?, context: PsiElement, option: Int): Any? {
        actionContext!!.checkStatus()
        if (duckType == null) return null

        val cacheKey = duckType.canonicalText() + "@" + groupKey(context, option)

        val resolvedInfo = getResolvedInfo<Any>(cacheKey)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        val psiClass = if (option.has(JsonOption.READ_COMMENT)) {
            getResourceClass(duckType.psiClass())
        } else {
            duckType.psiClass()
        }

        when {
            isNormalType(duckType) -> //normal Type
                return getDefaultValue(duckType)
            jvmClassHelper!!.isCollection(duckType) -> {   //list type
                val list = ArrayList<Any>()

                cacheResolvedInfo(cacheKey, list)

                val realIterableType = findRealIterableType(duckType)
                if (realIterableType != null) {
                    doGetTypeObject(realIterableType, context, option)?.let { list.add(it) }
                }

                return list.delay()
            }
            jvmClassHelper.isMap(duckType) -> {

                val typeOfCls = duckTypeHelper!!.buildPsiType(duckType, context)

                //map type
                val map: HashMap<Any, Any?> = HashMap()
                cacheResolvedInfo(cacheKey, map)
                val keyType = PsiUtil.substituteTypeParameter(typeOfCls, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                val valueType = PsiUtil.substituteTypeParameter(typeOfCls, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                var defaultKey: Any? = null

                if (keyType == null) {
                    val realKeyType = duckType.genericInfo?.get(StandardJvmClassHelper.KEY_OF_MAP)
                    defaultKey = doGetTypeObject(realKeyType, context, option)
                }

                if (defaultKey == null) {
                    defaultKey = if (keyType != null) {
                        doGetTypeObject(
                            duckTypeHelper.ensureType(keyType, duckType.genericInfo),
                            context,
                            option
                        )
                    } else {
                        ""
                    }
                }

                var defaultValue: Any? = null

                if (valueType == null) {
                    val realValueType = duckType.genericInfo?.get(StandardJvmClassHelper.VALUE_OF_MAP)
                    defaultValue = doGetTypeObject(realValueType, context, option)
                }
                if (defaultValue == null) {
                    defaultValue = if (valueType == null) {
                        null
                    } else {
                        doGetTypeObject(
                            duckTypeHelper.ensureType(valueType, duckType.genericInfo),
                            context,
                            option
                        )
                    }
                }

                if (defaultKey != null) {
                    map[defaultKey] = defaultValue
                }

                return map.delay()
            }
            jvmClassHelper.isEnum(duckType) -> {
                return parseEnum(duckType, context, option)
            }
            else -> //class type
            {
                if (psiClass is PsiTypeParameter) {
                    val typeParams = duckType.genericInfo
                    if (typeParams != null) {
                        val realType = typeParams[psiClass.name]
                        if (realType != null) {
                            return doGetTypeObject(realType, context, option)
                        }
                    }
                }

                if (ruleComputer.computer(ClassRuleKeys.TYPE_IS_FILE, psiClass) == true) {
                    return Magics.FILE_STR
                }
                return doGetFields(duckType, context, option)
            }
        }
    }

    protected open fun parseEnum(psiType: SingleDuckType, context: PsiElement, option: Int): Any? {
        return ""//by default use enum name `String`
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun getFields(clsWithParam: SingleDuckType, context: PsiElement?, option: Int): KV<String, Any?> {
        return doGetFields(clsWithParam, context, option).unwrapped().asKV()
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun doGetFields(clsWithParam: SingleDuckType, context: PsiElement?, option: Int): Any? {
        actionContext!!.checkStatus()

        val cacheKey = clsWithParam.canonicalText() + "@" + groupKey(context, option)

        val resolvedInfo = getResolvedInfo<Any>(cacheKey)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        val psiClass = if (option.has(JsonOption.READ_COMMENT)) {
            getResourceClass(clsWithParam.psiClass())
        } else {
            clsWithParam.psiClass()
        }
        val kv: KV<String, Any?> = KV()
        cacheResolvedInfo(cacheKey, kv)
        beforeParseType(psiClass, clsWithParam, option, kv)

        val explicitClass = duckTypeHelper!!.explicit(getResourceType(clsWithParam))
        foreachField(explicitClass, option) { fieldName, fieldType, fieldOrMethod ->

            if (!beforeParseFieldOrMethod(
                    fieldName,
                    fieldType,
                    fieldOrMethod,
                    explicitClass,
                    option,
                    kv
                )
            ) {
                onIgnoredParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, explicitClass, option, kv)
                return@foreachField
            }

            if (!kv.contains(fieldName)) {

                parseFieldOrMethod(fieldName, fieldType, fieldOrMethod, explicitClass, option, kv)

                afterParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, explicitClass, option, kv)
            }
        }

        afterParseType(psiClass, clsWithParam, option, kv)

        return kv.delay()
    }

    protected open fun foreachField(
        psiClass: ExplicitClass,
        option: Int,
        handle: (
            name: String,
            type: DuckType,
            fieldOrMethod: ExplicitElement<*>
        ) -> Unit
    ) {
        actionContext!!.checkStatus()

        val readMethod = option.has(JsonOption.READ_GETTER_OR_SETTER)

        var fieldNames: HashSet<String>? = null
        if (readMethod) {
            fieldNames = HashSet()
        }

        for (explicitField in psiClass.fields()) {
            val psiField = explicitField.psi()
            if (ignoreField(psiField)) {
                continue
            }

            //it should be decided by rule {@link com.itangcent.intellij.psi.ClassRuleKeys#FIELD_IGNORE}
//            if (!jvmClassHelper.isAccessibleField(psiField)) {
//                continue
//            }

            if (!readMethod || fieldNames!!.add(explicitField.name())) {
                handle(getJsonFieldName(psiField), explicitField.getType(), explicitField)
            }
        }

        if (readMethod) {
            val readGetter = option.has(JsonOption.READ_GETTER)
            val readSetter = option.has(JsonOption.READ_SETTER)
            for (explicitMethod in psiClass.methods()) {
                val method = explicitMethod.psi()
                val methodName = method.name
                if (jvmClassHelper!!.isBasicMethod(methodName)) continue
                if (!methodName.maybeMethodPropertyName()) {
                    continue
                }
                if (method.isConstructor) continue
                if (method.hasModifierProperty(PsiModifier.STATIC)) continue
                if (!method.hasModifierProperty(PsiModifier.PUBLIC)) continue

                if (readGetter && methodName.maybeGetterMethodPropertyName()) {
                    val propertyName = methodName.getterPropertyName()
                    if (readMethod && !fieldNames!!.add(propertyName)) continue
                    if (method.parameters.isNotEmpty()) continue
                    explicitMethod.getReturnType()?.let { handle(propertyName, it, explicitMethod) }
                }

                if (readSetter && methodName.maybeSetterMethodPropertyName()) {
                    val propertyName = methodName.setterPropertyName()
                    if (readMethod && !fieldNames!!.add(propertyName)) continue
                    if (method.parameters.isEmpty()) continue
                    explicitMethod.getParameters().firstOrNull()?.getType()?.let {
                        handle(propertyName, it, explicitMethod)
                    }
                }
            }
        }
        fieldNames?.clear()
    }

    protected open fun tryCastTo(psiType: PsiType, context: PsiElement): PsiType {
        return psiType
    }

    protected open fun tryCastTo(duckType: DuckType, context: PsiElement): DuckType {
        return duckType
    }

    protected open fun findRealIterableType(duckType: SingleDuckType): DuckType? {
        if (duckType.genericInfo.notNullOrEmpty()) {
            val realIterableType =
                duckType.genericInfo?.get(ELEMENT_OF_COLLECTION) ?: duckType.genericInfo!!.entries.first().value
            if (realIterableType != null) {
                return realIterableType
            }
        }

        val psiClass = duckType.psiClass()
        val superTypes = psiClass.superTypes
        for (superType in superTypes) {
            if (jvmClassHelper!!.isCollection(superType)) {
                val parameters = superType.parameters
                if (parameters.isNullOrEmpty()) {
                    continue
                }
                val typeParameters = superType.resolve()?.typeParameters
                if (typeParameters.isNullOrEmpty()) {
                    continue
                }
                for ((index, typeParameter) in typeParameters.withIndex()) {
                    if (typeParameter.name == ELEMENT_OF_COLLECTION) {
                        if (index >= parameters.size) {
                            return null
                        }
                        return duckTypeHelper!!.ensureType(parameters[index])
                    }
                }
                return parameters.firstOrNull()?.let { duckTypeHelper!!.ensureType(it) }
            }
        }
        return null
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

        val classAndPropertyOrMethod =
            psiResolver!!.resolveClassWithPropertyOrMethod(classNameWithProperty, context)

        if (classAndPropertyOrMethod?.first != null) {
            val first = classAndPropertyOrMethod.first?.asPsiClass(jvmClassHelper!!)
            if (first != null) {
                return if (classAndPropertyOrMethod.second != null) {
                    resolveEnumOrStatic(
                        context,
                        first,
                        PsiClassUtils.nameOfMember(classAndPropertyOrMethod.second!!),
                        defaultPropertyName
                    )
                } else {
                    property = classNameWithProperty
                        .substringAfter(first.name ?: "")
                        .filterNot { "[]()".contains(it) }
                        .takeIf { it.isNotBlank() }
                    resolveEnumOrStatic(
                        context,
                        first,
                        property,
                        defaultPropertyName
                    )
                }
            }
        }

        if (classNameWithProperty.contains("#")) {
            clsName = classNameWithProperty.substringBefore("#")
            property = classNameWithProperty
                .substringAfter("#")
                .trim()
        } else {
            clsName = classNameWithProperty.trim()
        }
        val cls = duckTypeHelper!!.resolveClass(clsName, context)?.let { getResourceClass(it) }
        return resolveEnumOrStatic(context, cls, property, defaultPropertyName)
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolveEnumOrStatic(
        context: PsiElement,
        cls: PsiClass?,
        property: String?,
        defaultPropertyName: String,
        valueTypeHandle: ((DuckType) -> Unit)?
    ): ArrayList<HashMap<String, Any?>>? {
        if (cls == null) return null

        val options: ArrayList<HashMap<String, Any?>> = ArrayList()

        if (jvmClassHelper!!.isEnum(cls)) {
            val enumConstants = parseEnumConstant(cls)

            var valProperty = property.trimToNull() ?: defaultPropertyName
            if (valProperty.maybeMethodPropertyName()) {
                val candidateProperty = valProperty.getterPropertyName()
                if (valProperty != candidateProperty) {
                    val allFields = jvmClassHelper.getAllFields(cls)
                    if (!allFields.any { it.name == valProperty }
                        && allFields.any { it.name == candidateProperty }
                    ) {
                        valProperty = candidateProperty
                    }
                }
            }

            if (valProperty.isNotBlank()) {
                findConstantsByProperty(enumConstants, valProperty, options)
            }

            if (options.isEmpty() && property.isNullOrBlank()) {
                if (ruleComputer.computer(ClassRuleKeys.ENUM_USE_ORDINAL, context) == true) {
                    findConstantsByProperty(enumConstants, "ordinal()", options)
                    valueTypeHandle?.let { it(duckTypeHelper!!.resolve("java.lang.Integer", context)!!) }
                } else if (ruleComputer.computer(ClassRuleKeys.ENUM_USE_NAME, context) != false) {
                    findConstantsByProperty(enumConstants, "name()", options)
                    valueTypeHandle?.let { it(duckTypeHelper!!.resolve("java.lang.String", context)!!) }
                }
            }

        } else {
            val constants = parseStaticFields(cls)

            if (property.notNullOrBlank()) {
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
            } else {
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

        }
        return options
    }

    @Suppress("UNCHECKED_CAST")
    private fun findConstantsByProperty(
        enumConstants: List<Map<String, Any?>>,
        valProperty: String,
        options: ArrayList<HashMap<String, Any?>>
    ) {
        loop@ for (enumConstant in enumConstants) {
            val mappedVal: Any = when (valProperty) {
                "name()" -> enumConstant["name"]!!
                "ordinal()" -> enumConstant["ordinal"]!!
                else -> (enumConstant["params"] as? HashMap<String, Any?>?)
                    ?.get(valProperty) ?: continue@loop
            }

            var desc = enumConstant["desc"]
            if (desc == null) {
                desc = (enumConstant["params"] as HashMap<String, Any?>?)!!
                    .filterKeys { k -> k != valProperty }
                    .map { entry -> entry.value.toString() }
                    .joinToString(" ")
                    .trimToNull()
            }
            if (desc.anyIsNullOrBlank()) {
                desc = enumConstant["name"]
            }
            options.add(
                KV.create<String, Any?>()
                    .set("value", mappedVal)
                    .set("desc", desc)
            )
        }
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

            if (ruleComputer.computer(ClassRuleKeys.CONSTANT_FIELD_IGNORE, field) == true) {
                continue
            }

            val value = field.computeConstantValue() ?: continue

            res.add(
                KV.create<String, Any?>()
                    .set("name", field.name)
                    .set("value", value.toString())
                    .set("desc", docHelper!!.getAttrOfField(field)?.trim())
            )
        }
        staticResolvedInfo[resourceClass] = res
        return res
    }

    protected open fun ignoreField(psiField: PsiField) = jvmClassHelper!!.isStaticFinal(psiField)

    override fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>> {
        actionContext!!.checkStatus()
        val sourcePsiClass = getResourceClass(psiClass)
        if (!jvmClassHelper!!.isEnum(sourcePsiClass)) return ArrayList()

        if (staticResolvedInfo.containsKey(sourcePsiClass)) {
            return staticResolvedInfo[sourcePsiClass]!!
        }

        val res = ArrayList<Map<String, Any?>>()
        for ((index, field) in jvmClassHelper.getAllFields(sourcePsiClass).withIndex()) {
            psiResolver!!.resolveEnumFields(index, field)?.let { res.add(it) }
        }

        staticResolvedInfo[sourcePsiClass] = res
        return res
    }

    override fun isNormalType(psiType: PsiType): Boolean {
        return jvmClassHelper!!.isNormalType(psiType.canonicalText)
    }

    override fun isNormalType(duckType: DuckType): Boolean {
        return (duckType.isSingle()) && jvmClassHelper!!.isNormalType(duckType.canonicalText())
    }

    override fun isNormalType(psiClass: PsiClass): Boolean {
        return jvmClassHelper!!.isNormalType(psiClass.qualifiedName ?: return false)
    }

    override fun getDefaultValue(psiType: PsiType): Any? {
        return jvmClassHelper!!.getDefaultValue(psiType.canonicalText)
    }

    override fun getDefaultValue(duckType: DuckType): Any? {
        return jvmClassHelper!!.getDefaultValue(duckType.canonicalText())
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

    override fun getJsonFieldName(psiMethod: PsiMethod): String {
        val name = psiMethod.name
        if (name.startsWith("is")) {
            return name.removePrefix("is").decapitalize()
        }
        if (name.startsWith("get")) {
            return name.removePrefix("get").decapitalize()
        }
        return name
    }

    open fun getResourceClass(psiClass: PsiClass): PsiClass {
        return psiClass
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T : DuckType> getResourceType(duckType: T): T {
        return when (duckType) {
            is SingleDuckType -> {
                val psiClass = duckType.psiClass()
                val resourceClass = getResourceClass(psiClass)
                if (resourceClass == psiClass) {
                    duckType
                } else {
                    SingleDuckType(resourceClass, duckType.genericInfo) as T
                }
            }
            is ArrayDuckType -> {
                val componentType = duckType.componentType()
                val resourceComponentType = getResourceType(componentType)
                if (resourceComponentType == componentType) {
                    duckType
                } else {
                    ArrayDuckType(componentType) as T
                }
            }
            else -> duckType
        }
    }

    open fun beforeParseClass(psiClass: PsiClass, option: Int, kv: KV<String, Any?>) {

    }

    open fun afterParseClass(psiClass: PsiClass, option: Int, kv: KV<String, Any?>) {

    }

    //return false to ignore current fieldOrMethod
    open fun beforeParseFieldOrMethod(
        fieldName: String,
        fieldType: DuckType,
        fieldOrMethod: ExplicitElement<*>,
        resourcePsiClass: ExplicitClass,
        option: Int,
        kv: KV<String, Any?>
    ): Boolean {
        return true
    }

    open fun parseFieldOrMethod(
        fieldName: String,
        fieldType: DuckType,
        fieldOrMethod: ExplicitElement<*>,
        resourcePsiClass: ExplicitClass,
        option: Int,
        kv: KV<String, Any?>
    ) {
        kv[fieldName] = doGetTypeObject(fieldType, fieldOrMethod.psi(), option)
    }

    open fun afterParseFieldOrMethod(
        fieldName: String,
        fieldType: DuckType,
        fieldOrMethod: ExplicitElement<*>,
        resourcePsiClass: ExplicitClass,
        option: Int,
        kv: KV<String, Any?>
    ) {

    }

    open fun onIgnoredParseFieldOrMethod(
        fieldName: String,
        fieldType: DuckType,
        fieldOrMethod: ExplicitElement<*>,
        resourcePsiClass: ExplicitClass,
        option: Int,
        kv: KV<String, Any?>
    ) {

    }

    open fun beforeParseType(psiClass: PsiClass, duckType: SingleDuckType, option: Int, kv: KV<String, Any?>) {

    }

    open fun afterParseType(psiClass: PsiClass, duckType: SingleDuckType, option: Int, kv: KV<String, Any?>) {

    }

    //return false to ignore current fieldOrMethod
}

private val index = AtomicInteger()

interface Wrapped {
    fun get(): Any?
}

class WrappedValue(private var value: Any?) : KV<String, Any?>(), Wrapped {

    private val id = index.getAndIncrement()

    override fun get(): Any? {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WrappedValue

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}

fun Any?.wrap(): WrappedValue {
    return WrappedValue(this)
}

@Suppress("UNCHECKED_CAST")
fun <T> T?.unwrapped(): T? {
    return Unwrapper().unwrapped(this) as T?
}

fun Any?.delay(): Delay {
    if (this is Delay) {
        return this
    }
    return Delay(this)
}

class Delay(private var raw: Any?) : Wrapped {

    private val id = index.getAndIncrement()

    override fun get(): Any? {
        return raw
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Delay

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}

fun Any?.wrapped(deep: Int = 0): Boolean {
    when {
        this == null || deep > 10 -> {
            return false
        }
        this is Wrapped -> {
            return true
        }
        this is Collection<*> -> {
            return this.any { it.wrapped(deep + 1) }
        }
        this is Array<*> -> {
            return this.any { it.wrapped(deep + 1) }
        }
        this is Map<*, *> -> {
            return this.keys.any { it.wrapped(deep + 1) }||this.values.any { it.wrapped(deep + 1) }
        }
        else -> return false
    }
}

class Unwrapper {

    private val stack = ArrayDeque<Any>(5)
    private val unwrappedCache = SafeHashMap<Any, Any?>()

    @Suppress("UNCHECKED_CAST")
    fun unwrapped(any: Any?, parent: Map<Any?, Any?>? = null, key: String? = null): Any? {
        return any.unwrapped(0, parent, key)
    }

    @Suppress("UNCHECKED_CAST")
    fun Any?.unwrapped(deep: Int = 0, parent: Map<Any?, Any?>? = null, key: String? = null): Any? {
        if (this == null) {
            return null
        }
        if (stack.contains(this)) {
            when {
                stack.count { it == this } == 1 -> {
                    //can reentrant
                    return this.doUnwrappedWithStack(deep, parent, key)
                }
                this.wrapped(deep) -> {
                    val cache = unwrappedCache[this]
                    if (cache == null) {
                        return Any()
                    } else {
                        return cache.copy()
                    }
                }
                else -> {
                    return this.copy()
                }
            }
        } else {
            return this.doUnwrappedWithStack(deep, parent, key)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun Any.doUnwrappedWithStack(deep: Int, parent: Map<Any?, Any?>?, key: String? = null): Any? {
        stack.add(this)
        try {
            return this.doUnwrapped(deep, parent, key).also { unwrappedCache[this] = it }
        } finally {
            stack.removeLastOrNull()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun Any.doUnwrapped(deep: Int, parent: Map<Any?, Any?>?, key: String? = null): Any? {
        return when {
            deep > 10 -> {
                this.copy()
            }
            this is WrappedValue -> {
                fillExtras(this, parent, key)
                this.get().unwrapped(deep, parent, key)
            }
            this is Wrapped -> {
                this.get().unwrapped(deep, parent, key)
            }
            this.wrapped(deep) && this is Collection<*> -> {
                this.map { it.unwrapped(deep + 1, parent, key) }
            }
            this.wrapped(deep) && this is Array<*> -> {
                this.map { it.unwrapped(deep + 1, parent, key) }
            }
            this.wrapped(deep) && this is Map<*, *> -> {
                val copy = LinkedHashMap<Any?, Any?>()
                this.forEach {
                    copy.merge(
                        it.key.unwrapped(deep + 1),
                        it.value.unwrapped(deep + 1, copy, it.key as? String)
                    )
                }
                copy
            }
            else -> {
                this.copy()
            }
        }
    }


    private fun fillExtras(value: WrappedValue, parent: Map<Any?, Any?>?, key: String? = null) {
        if (parent == null || key == null) {
            return
        }
        value.forEach { k, v ->
            if ((k as? String)?.startsWith('@') == true) {
                val index = k.indexOf('@', 1)
                if (index == -1) {
                    parent.sub(k).mutable()[key] = v
                } else {
                    parent.sub(k.substring(0, index)).mutable()[key + "@" + k.substring(index + 1)] = v
                }
            }
        }
    }
}

object JsonOption {
    const val NONE = 0b0000//None additional options
    const val READ_COMMENT = 0b0001//try find comments
    const val READ_GETTER = 0b0010//Try to find the available getter method as property
    const val READ_SETTER = 0b0100//Try to find the available setter method as property
    const val ALL = READ_COMMENT or READ_GETTER or READ_SETTER//All additional options
    const val READ_GETTER_OR_SETTER =
        READ_GETTER or READ_SETTER//Try to find the available getter or setter method as property

    @Deprecated(
        message = "use #has",
        replaceWith = ReplaceWith("has(JsonOption.READ_COMMENT)")
    )
    fun needComment(flag: Int): Boolean {
        return (flag and READ_COMMENT) != 0
    }

    @Deprecated(
        message = "use #has",
        replaceWith = ReplaceWith("has(JsonOption.READ_GETTER)")
    )
    fun readGetter(flag: Int): Boolean {
        return (flag and READ_GETTER) != 0
    }

    fun Int.has(flag: Int): Boolean {
        return (this and flag) != 0
    }

    fun Int.hasAny(vararg flag: Int): Boolean {
        return flag.any { (this and it) != 0 }
    }
}