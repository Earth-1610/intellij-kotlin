package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.itangcent.common.utils.KV
import com.itangcent.common.utils.invokeMethod
import com.itangcent.common.utils.isNullOrBlank
import com.itangcent.common.utils.trimToNull
import com.itangcent.intellij.config.rule.RuleComputer
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.jvm.*
import com.itangcent.intellij.jvm.duck.ArrayDuckType
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.duck.SingleUnresolvedDuckType
import com.itangcent.intellij.jvm.element.ExplicitClass
import com.itangcent.intellij.jvm.element.ExplicitElement
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper.Companion.ELEMENT_OF_COLLECTION
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.util.Magics
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
        if (psiType == null || psiType == PsiType.NULL) return null

        val resolvedInfo = getResolvedInfo<Any>(psiType, option)
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

            val explicitClass = duckTypeHelper!!.explicit(resourcePsiClass)
            foreachField(explicitClass, option) { fieldName, fieldType, fieldOrMethod ->

                if (!beforeParseFieldOrMethod(fieldType, fieldOrMethod, explicitClass, option, kv)) {
                    return@foreachField
                }
                val name = fieldName()
                if (!kv.contains(name)) {

                    parseFieldOrMethod(name, fieldType, fieldOrMethod, explicitClass, option, kv)

                    afterParseFieldOrMethod(name, fieldType, fieldOrMethod, explicitClass, option, kv)
                }
            }

            afterParseClass(resourcePsiClass, option, kv)
        }

        return copy(kv) as KV<String, Any?>
    }

    override fun getTypeObject(duckType: DuckType?, context: PsiElement): Any? {
        return getTypeObject(duckType, context, JsonOption.NONE)
    }

    override fun getTypeObject(duckType: DuckType?, context: PsiElement, option: Int): Any? {
        actionContext!!.checkStatus()

        if (duckType == null) return null

        val resolvedInfo = getResolvedInfo<Any>(duckType, option)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        val type = tryCastTo(duckType, context)

        if (type is ArrayDuckType) {
            val list = ArrayList<Any>()
            cacheResolvedInfo(type, option, list)
            getTypeObject(type.componentType(), context, option)?.let { list.add(it) }
            return copy(list)
        }

        if (type is SingleDuckType) {
            return getTypeObject(type as SingleDuckType, context, option)
        }

        if (type is SingleUnresolvedDuckType) {
            return getTypeObject(type.psiType(), context, option)
        }

        return null
    }

    protected open fun getTypeObject(duckType: SingleDuckType?, context: PsiElement, option: Int): Any? {
        actionContext!!.checkStatus()
        if (duckType == null) return null

        val resolvedInfo = getResolvedInfo<Any>(duckType, option)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        val psiClass = if (JsonOption.needComment(option)) {
            getResourceClass(duckType.psiClass())
        } else {
            duckType.psiClass()
        }

        when {
            isNormalType(duckType) -> //normal Type
                return getDefaultValue(duckType)
            jvmClassHelper!!.isCollection(duckType) -> {   //list type
                val list = ArrayList<Any>()

                cacheResolvedInfo(duckType, option, list)

                if (!duckType.genericInfo.isNullOrEmpty()) {
                    val realIterableType =
                        duckType.genericInfo?.get(ELEMENT_OF_COLLECTION) ?: duckType.genericInfo!!.entries.first().value
                    if (realIterableType != null) {
                        getTypeObject(realIterableType, context, option)?.let { list.add(it) }
                    }
                }

                return copy(list)
            }
            jvmClassHelper.isMap(duckType) -> {

                val typeOfCls = duckTypeHelper!!.buildPsiType(duckType, context)

                //map type
                val map: HashMap<Any, Any?> = HashMap()
                cacheResolvedInfo(duckType, option, map)
                val keyType = PsiUtil.substituteTypeParameter(typeOfCls, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                val valueType = PsiUtil.substituteTypeParameter(typeOfCls, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                var defaultKey: Any? = null

                if (keyType == null) {
                    val realKeyType = duckType.genericInfo?.get(StandardJvmClassHelper.KEY_OF_MAP)
                    defaultKey = getTypeObject(realKeyType, context, option)
                }

                if (defaultKey == null) {
                    defaultKey = if (keyType != null) {
                        getTypeObject(
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
                    defaultValue = getTypeObject(realValueType, context, option)
                }
                if (defaultValue == null) {
                    defaultValue = if (valueType == null) {
                        null
                    } else {
                        getTypeObject(
                            duckTypeHelper!!.ensureType(valueType, duckType.genericInfo),
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
            jvmClassHelper.isEnum(duckType) -> {
                return ""//by default use enum name `String`
            }
            else -> //class type
            {
                if (psiClass is PsiTypeParameter) {
                    val typeParams = duckType.genericInfo
                    if (typeParams != null) {
                        val realType = typeParams[psiClass.name]
                        if (realType != null) {
                            return getTypeObject(realType, context, option)
                        }
                    }
                }

                if (ruleComputer!!.computer(ClassRuleKeys.TYPE_IS_FILE, psiClass) == true) {
                    return Magics.FILE_STR
                }
                return getFields(duckType, option)
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

        val explicitClass = duckTypeHelper!!.explicit(getResourceType(clsWithParam))
        foreachField(explicitClass, option) { fieldName, fieldType, fieldOrMethod ->

            if (!beforeParseFieldOrMethod(
                    fieldType,
                    fieldOrMethod,
                    explicitClass,
                    option,
                    kv
                )
            ) {
                return@foreachField
            }

            val name = fieldName()
            if (!kv.contains(name)) {

                parseFieldOrMethod(name, fieldType, fieldOrMethod, explicitClass, option, kv)

                afterParseFieldOrMethod(name, fieldType, fieldOrMethod, explicitClass, option, kv)
            }
        }

        afterParseType(psiClass, clsWithParam, option, kv)

        return copy(kv) as KV<String, Any?>
    }

    protected open fun foreachField(
        psiClass: ExplicitClass,
        option: Int,
        handle: (
            name: () -> String,
            type: DuckType,
            fieldOrMethod: ExplicitElement<*>
        ) -> Unit
    ) {
        actionContext!!.checkStatus()

        val readGetter = JsonOption.readGetter(option)
        var fieldNames: HashSet<String>? = null
        if (readGetter) {
            fieldNames = HashSet()
        }

        for (explicitField in psiClass.fields()) {
            val psiField = explicitField.psi()
            if (jvmClassHelper!!.isStaticFinal(psiField)) {
                continue
            }

            //it should be decided by rule {@link com.itangcent.intellij.psi.ClassRuleKeys#FIELD_IGNORE}
//            if (!jvmClassHelper.isAccessibleField(psiField)) {
//                continue
//            }

            if (!readGetter || fieldNames!!.add(explicitField.name())) {
                handle({ getJsonFieldName(psiField) }, explicitField.getType(), explicitField)
            }
        }

        if (JsonOption.readGetter(option)) {
            for (explicitMethod in psiClass.methods()) {
                val method = explicitMethod.psi()
                val methodName = method.name
                if (jvmClassHelper!!.isBasicMethod(methodName)) continue
                val propertyName = propertyNameOfGetter(methodName) ?: continue
                if (readGetter && !fieldNames!!.add(propertyName)) continue
                if (method.isConstructor) continue
                if (method.parameters.isNotEmpty()) continue
                if (method.hasModifierProperty(PsiModifier.STATIC)) continue
                if (!method.hasModifierProperty(PsiModifier.PUBLIC)) continue

                explicitMethod.getReturnType()?.let { handle({ propertyName }, it, explicitMethod) }
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

    protected open fun tryCastTo(duckType: DuckType, context: PsiElement): DuckType {
        return duckType
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

        val classAndPropertyOrMethod =
            psiResolver!!.resolveClassWithPropertyOrMethod(classNameWithProperty, context)

        if (classAndPropertyOrMethod?.first != null) {
            val first = classAndPropertyOrMethod.first?.asPsiClass(jvmClassHelper!!)
            if (first != null) {
                return if (classAndPropertyOrMethod.second != null) {
                    resolveEnumOrStatic(
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
                .removeSuffix("()")
        } else {
            clsName = classNameWithProperty.trim().removeSuffix("()")
        }
        cls = duckTypeHelper!!.resolveClass(clsName, context)?.let { getResourceClass(it) }
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

        if (jvmClassHelper!!.isEnum(cls)) {
            val enumConstants = parseEnumConstant(cls)

            var valProperty = property.trimToNull() ?: defaultPropertyName
            if (!valProperty.isBlank()) {
                val candidateProperty = propertyNameOfGetter(valProperty)
                if (candidateProperty != null && valProperty != candidateProperty) {
                    if (!jvmClassHelper.getAllFields(cls)
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
                val mappedVal = (enumConstant["params"] as? HashMap<String, Any?>?)
                    ?.get(valProperty) ?: continue

                var desc = enumConstant["desc"]
                if (desc == null) {
                    desc = (enumConstant["params"] as HashMap<String, Any?>?)!!
                        .filterKeys { k -> k != valProperty }
                        .map { entry -> entry.value.toString() }
                        .joinToString(" ")
                        .trimToNull()
                }
                if (desc.isNullOrBlank()) {
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
                    .set("desc", docHelper!!.getAttrOfField(field)?.trim())
            )
        }
        staticResolvedInfo[resourceClass] = res
        return res
    }

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
        kv[fieldName] = getTypeObject(fieldType, fieldOrMethod.psi(), option)
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

    open fun beforeParseType(psiClass: PsiClass, duckType: SingleDuckType, option: Int, kv: KV<String, Any?>) {

    }

    open fun afterParseType(psiClass: PsiClass, duckType: SingleDuckType, option: Int, kv: KV<String, Any?>) {

    }

    //return false to ignore current fieldOrMethod
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
