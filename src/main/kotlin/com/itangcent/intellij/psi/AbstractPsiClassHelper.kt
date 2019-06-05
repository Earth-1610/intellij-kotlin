package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.spring.MultipartFile
import com.itangcent.intellij.spring.SpringClassName
import com.itangcent.intellij.util.KV
import com.itangcent.intellij.util.invokeMethod
import com.sun.jmx.remote.internal.ArrayQueue
import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.reflect.full.createInstance

abstract class AbstractPsiClassHelper : PsiClassHelper {

    private val resolvedInfo: HashMap<Int, HashMap<Any, Any?>> = HashMap()

    @Inject
    protected val logger: Logger? = null

    @Inject
    protected val tmTypeHelper: DuckTypeHelper? = null
    @Inject
    protected val actionContext: ActionContext? = null

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
            isNormalType(castTo.canonicalText) -> return getDefaultValue(castTo.canonicalText)
            castTo is PsiArrayType -> {   //array type
                val deepType = castTo.getDeepComponentType()
                val list = java.util.ArrayList<Any>()
                cacheResolvedInfo(castTo, option, list)//cache
                when {
                    deepType is PsiPrimitiveType -> list.add(PsiTypesUtil.getDefaultValue(deepType))
                    isNormalType(deepType.canonicalText) -> list.add(getDefaultValue(deepType.canonicalText)!!)
                    else -> getTypeObject(deepType, context, option)?.let { list.add(it) }
                }
                return copy(list)
            }
            castTo.canonicalText == SpringClassName.MULTIPARTFILE -> {
                cacheResolvedInfo(castTo, option, multipartFileInstance)//cache
                return multipartFileInstance
            }
            isCollection(castTo) -> {   //list type

                val list = java.util.ArrayList<Any>()
                cacheResolvedInfo(castTo, option, list)//cache
                val iterableType = PsiUtil.extractIterableTypeParameter(castTo, false)
                val iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType)
                val classTypeName: String? = iterableClass?.qualifiedName
                when {
                    classTypeName != null && isNormalType(classTypeName) -> getDefaultValue(classTypeName)?.let {
                        list.add(
                            it
                        )
                    }
                    iterableType != null -> getTypeObject(iterableType, context, option)?.let { list.add(it) }
                }
                return copy(list)
            }
            isMap(castTo) -> {   //list type
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
            else -> {
                val typeCanonicalText = castTo.canonicalText
                if (typeCanonicalText.contains('<') && typeCanonicalText.endsWith('>')) {

                    val tmType = tmTypeHelper!!.resolve(castTo, context)

                    if (tmType != null) {
                        val result = getTypeObject(tmType, context, option)
                        cacheResolvedInfo(castTo, option, result)
                        return copy(result)
                    } else {
                        return null
                    }
                } else {
                    val paramCls = PsiUtil.resolveClassInClassTypeOnly(castTo)
                    try {
                        val result = getFields(paramCls, option)
                        cacheResolvedInfo(castTo, option, result)
                        return result
                    } catch (e: Throwable) {
                        logger!!.error("error to getTypeObject:$psiType" + ExceptionUtils.getStackTrace(e))
                        return null
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
                if (!kv.contains(fieldName)) {

                    if (!beforeParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, resourcePsiClass, option, kv)) {
                        return@foreachField
                    }

                    parseFieldOrMethod(fieldName, fieldType, fieldOrMethod, resourcePsiClass, option, kv)

                    afterParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, resourcePsiClass, option, kv)
                }
            }

            afterParseClass(resourcePsiClass, option, kv)
        }

        return copy(kv) as KV<String, Any?>
    }

    private fun getTypeObject(duckType: DuckType?, context: PsiElement, option: Int): Any? {

        val resolvedInfo = getResolvedInfo<Any>(duckType, option)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        if (duckType == null) return null

        if (duckType is ArrayDuckType) {
            val list = ArrayList<Any>()
            cacheResolvedInfo(duckType, option, list)
            getTypeObject(duckType.componentType, context, option)?.let { list.add(it) }
            return copy(list)
        }

        if (duckType is SingleDuckType) {
            return getTypeObject(duckType as SingleDuckType, context, option)
        }
        return null
    }

    private fun getTypeObject(clsWithParam: SingleDuckType?, context: PsiElement, option: Int): Any? {

        if (clsWithParam != null) {
            val resolvedInfo = getResolvedInfo<Any>(clsWithParam, option)
            if (resolvedInfo != null) {
                return resolvedInfo
            }
        }

        if (clsWithParam == null) return null
        val psiClass = if (JsonOption.needComment(option)) {
            getResourceClass(clsWithParam.psiCls)
        } else {
            clsWithParam.psiCls
        }
        val typeOfCls = PsiTypesUtil.getClassType(psiClass)

        val type = tryCastTo(typeOfCls, psiClass)
        if (type is PsiPrimitiveType) {       //primitive Type
            return PsiTypesUtil.getDefaultValueOfType(type)
        } else {    //reference Type
            val typeName = type.canonicalText
            when {
                isNormalType(typeName) -> //normal Type
                    return getDefaultValue(typeName)
                type is PsiArrayType -> {   //array type
                    val deepType = type.getDeepComponentType()
                    val list = ArrayList<Any>()
                    cacheResolvedInfo(clsWithParam, option, list)
                    when {
                        deepType is PsiPrimitiveType -> list.add(PsiTypesUtil.getDefaultValue(deepType))
                        isNormalType(deepType.canonicalText) -> getDefaultValue(deepType.canonicalText)?.let {
                            list.add(
                                it
                            )
                        }
                        else -> getTypeObject(
                            tmTypeHelper!!.ensureType(deepType, clsWithParam.typeParams),
                            context,
                            option
                        )?.let { list.add(it) }
                    }
                    return copy(list)
                }
                type.canonicalText == SpringClassName.MULTIPARTFILE -> {
                    return multipartFileInstance
                }
                isCollection(type) -> {   //list type
                    val list = ArrayList<Any>()

                    cacheResolvedInfo(clsWithParam, option, list)
                    val iterableType = PsiUtil.extractIterableTypeParameter(type, false)
                    if (iterableType == null) {//maybe generic type
                        val realIterableType = clsWithParam.typeParams?.get(ELEMENT_OF_COLLECTION)
                        if (realIterableType != null) {
                            getTypeObject(realIterableType, context, option)?.let { list.add(it) }
                            return copy(list)
                        }
                    }
                    val iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType)
                    val classTypeName: String? = iterableClass?.qualifiedName

                    when {
                        classTypeName != null && isNormalType(classTypeName) -> getDefaultValue(classTypeName)?.let {
                            list.add(
                                it
                            )
                        }
                        iterableType != null -> getTypeObject(
                            tmTypeHelper!!.ensureType(
                                iterableType,
                                clsWithParam.typeParams
                            ), context, option
                        )?.let { list.add(it) }
                    }

                    return copy(list)
                }
                isMap(type) -> {
                    //list type
                    val map: HashMap<Any, Any?> = HashMap()
                    cacheResolvedInfo(clsWithParam, option, map)
                    val keyType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                    val valueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                    var defaultKey: Any? = null

                    if (keyType == null) {
                        val realKeyType = clsWithParam.typeParams?.get(KEY_OF_MAP)
                        defaultKey = getTypeObject(realKeyType, context, option)
                    }

                    if (defaultKey == null) {
                        defaultKey = if (keyType != null) {
                            getTypeObject(
                                tmTypeHelper!!.ensureType(keyType, clsWithParam.typeParams),
                                context,
                                option
                            )
                        } else {
                            ""
                        }
                    }

                    var defaultValue: Any? = null

                    if (valueType == null) {
                        val realValueType = clsWithParam.typeParams?.get(VALUE_OF_MAP)
                        defaultValue = getTypeObject(realValueType, context, option)
                    }
                    if (defaultValue == null) {
                        if (valueType != null) {
                            defaultValue = getTypeObject(
                                tmTypeHelper!!.ensureType(valueType, clsWithParam.typeParams),
                                context,
                                option
                            )
                        } else {
                            defaultValue = null
                        }
                    }

                    if (defaultKey != null) {
                        map[defaultKey] = defaultValue
                    }

                    return copy(map)
                }
                else -> //class type
                {
                    val clsOfType = PsiUtil.resolveClassInType(type)

                    if (clsOfType is PsiTypeParameter) {
                        val typeParams = clsWithParam.typeParams
                        if (typeParams != null) {
                            val realType = typeParams[clsOfType.name]
                            if (realType != null) {
                                return getTypeObject(realType, context, option)
                            }
                        }
                    }
                    return getFields(clsWithParam, option)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getFields(clsWithParam: SingleDuckType, option: Int): KV<String, Any?> {

        val resolvedInfo = getResolvedInfo<Any>(clsWithParam, option)
        if (resolvedInfo != null) {
            return resolvedInfo as KV<String, Any?>
        }

        val psiClass = if (JsonOption.needComment(option)) {
            getResourceClass(clsWithParam.psiCls)
        } else {
            clsWithParam.psiCls
        }
        val kv: KV<String, Any?> = KV.create()
        cacheResolvedInfo(clsWithParam, option, kv)
        beforeParseType(psiClass, clsWithParam, option, kv)

        foreachField(psiClass, option) { fieldName, fieldType, fieldOrMethod ->
            if (!kv.contains(fieldName)) {

                if (!beforeParseFieldOrMethod(
                        fieldName,
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

                parseFieldOrMethod(fieldName, fieldType, fieldOrMethod, psiClass, clsWithParam, option, kv)

                afterParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, psiClass, clsWithParam, option, kv)
            }
        }

        afterParseType(psiClass, clsWithParam, option, kv)

        return copy(kv) as KV<String, Any?>
    }

    private fun foreachField(
        psiClass: PsiClass,
        option: Int,
        handle: (name: String, type: PsiType, fieldOrMethod: PsiElement) -> Unit
    ) {

        val readGetter = JsonOption.readGetter(option)
        var fieldNames: HashSet<String>? = null
        if (readGetter) {
            fieldNames = HashSet()
        }

        for (field in psiClass.allFields) {
            if (hasAnyModify(field, staticFinalFieldModifiers)) {
                continue
            }

            if (!hasAnyModify(field, fieldModifiers)) {
                continue
            }

            if (!readGetter || fieldNames!!.add(field.name)) {
                handle(getJsonFieldName(field), field.type, field)
            }
        }

        if (JsonOption.readGetter(option)) {
            for (method in psiClass.allMethods) {
                val methodName = method.name
                if (JAVA_OBJECT_METHODS.contains(methodName)) continue
                val propertyName = propertyNameOfGetter(methodName) ?: continue
                if (readGetter && !fieldNames!!.add(propertyName)) continue
                if (method.isConstructor) continue
                if (method.parameters.isNotEmpty()) continue
                if (method.hasModifierProperty(PsiModifier.STATIC)) continue
                if (!method.hasModifierProperty(PsiModifier.PUBLIC)) continue

                method.returnType?.let { handle(propertyName, it, method) }
            }
        }
        fieldNames?.clear()
    }

    protected fun hasAnyModify(modifierListOwner: PsiModifierListOwner, modifies: Set<String>): Boolean {
        val modifierList = modifierListOwner.modifierList ?: return false
        return modifies.any { modifierList.hasModifierProperty(it) }
    }

    protected fun propertyNameOfGetter(methodName: String): String? {
        return when {
            methodName.startsWith("get") -> methodName.removePrefix("get")
            methodName.startsWith("is") -> methodName.removePrefix("is")
            else -> null
        }?.decapitalize()
    }

    protected open fun tryCastTo(psiType: PsiType, context: PsiElement): PsiType {
        return psiType
    }

    override fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement? {
        if (propertyOrMethod.endsWith(")")) {
            if (!propertyOrMethod.contains("(")) {
                logger!!.warn("error to resolve:[$propertyOrMethod]")
                return null
            }

            val methodName = propertyOrMethod.substringBefore("(")
            var candidates = psiClass.allMethods.filter { it.name == methodName }.toList()
            when {
                candidates.isEmpty() -> return null
                candidates.size == 1 -> return candidates[0]
            }

            val params = propertyOrMethod.substringAfter("(")
                .removeSuffix(")")
                .split(",")
            candidates = candidates.filter { it.parameters.size == params.size }

            when {
                candidates.isEmpty() -> return null
                candidates.size == 1 -> return candidates[0]
            }


            candidates = candidates.filter { method ->
                return@filter method.parameterList.parameters
                    .filterIndexed { index, parameter -> !parameter.type.canonicalText.contains(params[index]) }
                    .none()
            }

            when {
                candidates.isEmpty() -> return null
                candidates.size == 1 -> return candidates[0]
            }

            return null
        } else {
            return psiClass.allFields.firstOrNull { it.name == propertyOrMethod }
                ?: psiClass.allMethods.firstOrNull { it.name == propertyOrMethod }
        }
    }

    override fun isNormalType(typeName: String): Boolean {
        return normalTypes.containsKey(typeName)
    }

    override fun getDefaultValue(typeName: String): Any? {
        return normalTypes[typeName]
    }

    override fun unboxArrayOrList(psiType: PsiType): PsiType {
        when {
            psiType is PsiPrimitiveType -> return psiType
            isNormalType(psiType.canonicalText) -> return psiType
            psiType is PsiArrayType -> {   //array type
                return psiType.getDeepComponentType()
            }
            isCollection(psiType) -> {   //list type
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

    open fun getJsonFieldName(psiField: PsiField): String {
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
        fieldName: String,
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
            val fieldTypeName = type.canonicalText
            when {
                isNormalType(fieldTypeName) -> //normal Type
                    kv[fieldName] = getDefaultValue(fieldTypeName)
                type is PsiArrayType -> {   //array type
                    val deepType = type.getDeepComponentType()

                    val list = ArrayList<Any>()
                    when {
                        deepType is PsiPrimitiveType -> list.add(PsiTypesUtil.getDefaultValue(deepType))
                        isNormalType(deepType.canonicalText) -> getDefaultValue(deepType.canonicalText)?.let {
                            list.add(
                                it
                            )
                        }
                        else -> getTypeObject(deepType, fieldOrMethod, option)?.let { list.add(it) }
                    }
                    kv[fieldName] = list
                }
                fieldTypeName == SpringClassName.MULTIPARTFILE -> {
                    kv[fieldName] = multipartFileInstance
                }
                isCollection(type) -> {   //list type
                    val iterableType = PsiUtil.extractIterableTypeParameter(type, false)
                    val iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType)
                    val list = ArrayList<Any>()
                    val classTypeName: String? = iterableClass?.qualifiedName
                    when {
                        classTypeName != null && isNormalType(classTypeName) -> getDefaultValue(
                            classTypeName
                        )?.let { list.add(it) }
                        iterableClass == resourcePsiClass -> list.add(Collections.emptyMap<Any, Any>())
                        else -> getTypeObject(iterableType, fieldOrMethod, option)?.let { list.add(it) }
                    }
                    kv[fieldName] = list
                }
                isMap(type) -> {   //map type
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
                else -> //class type
                {
                    val clsOfType = PsiUtil.resolveClassInType(type)
                    if (clsOfType != resourcePsiClass) {
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
        fieldName: String,
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
        val fieldTypeName = fieldType.canonicalText

        if (isNormalType(fieldTypeName)) {//normal Type
            kv[fieldName] = getDefaultValue(fieldTypeName)
            return
        }
        val clsOfType = PsiUtil.resolveClassInType(fieldType)

        if (clsOfType is PsiTypeParameter) {
            val typeParams = duckType.typeParams
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
                    isNormalType(deepType.canonicalText) -> getDefaultValue(deepType.canonicalText)?.let {
                        list.add(
                            it
                        )
                    }
                    else -> getTypeObject(
                        tmTypeHelper!!.ensureType(deepType, duckType.typeParams),
                        resourcePsiClass,
                        option
                    )?.let { list.add(it) }
                }
                kv[fieldName] = list
            }
            type.canonicalText == SpringClassName.MULTIPARTFILE -> {
                kv[fieldName] = multipartFileInstance
            }
            isCollection(type) -> {   //list type
                val list = ArrayList<Any>()
                val iterableType = PsiUtil.extractIterableTypeParameter(type, false)
                val iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType)
                val classTypeName: String? = iterableClass?.qualifiedName
                when {
                    classTypeName != null && isNormalType(classTypeName) -> list.add(
                        getDefaultValue(
                            classTypeName
                        )!!
                    )
                    iterableClass == resourcePsiClass -> list.add(Collections.emptyMap<Any, Any>())
                    iterableType != null -> getTypeObject(
                        tmTypeHelper!!.ensureType(
                            iterableType,
                            duckType.typeParams
                        ), resourcePsiClass, option
                    )?.let { list.add(it) }
                }
                kv[fieldName] = list
            }
            isMap(type) -> {   //list type
                val map: HashMap<Any, Any?> = HashMap()
                val keyType =
                    PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                val valueType =
                    PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                var defaultKey: Any? = null
                if (keyType != null) defaultKey = getTypeObject(
                    tmTypeHelper!!.ensureType(keyType, duckType.typeParams),
                    resourcePsiClass,
                    option
                )
                if (defaultKey == null) defaultKey = ""

                var defaultValue: Any? = null
                if (valueType != null) defaultValue = getTypeObject(
                    tmTypeHelper!!.ensureType(valueType, duckType.typeParams),
                    resourcePsiClass,
                    option
                )
                if (defaultValue == null) defaultValue = null

                map[defaultKey] = defaultValue

                kv[fieldName] = map
            }
            else -> //class type
            {
                if (clsOfType != resourcePsiClass) {
                    kv[fieldName] = getFields(clsOfType, option)
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

    }

    companion object {

        val JAVA_OBJECT_METHODS: Array<String> = arrayOf(
            "registerNatives",
            "getClass",
            "hashCode",
            "equals",
            "clone",
            "toString",
            "notify",
            "notifyAll",
            "wait",
            "finalize"
        )

        val ELEMENT_OF_COLLECTION = "E"
        val KEY_OF_MAP = "K"
        val VALUE_OF_MAP = "V"

        fun isCollection(psiType: PsiType): Boolean {
            if (collectionClasses!!.contains(psiType.presentableText)) {
                return true
            }

            val cls = PsiUtil.resolveClassInType(psiType)
            if (cls != null) {
                for (superCls in cls.supers) {
                    if (collectionClasses!!.contains(superCls.qualifiedName)) {
                        return true
                    }
                }
            }

            return false
        }

        fun isMap(psiType: PsiType): Boolean {
            if (mapClasses!!.contains(psiType.presentableText)) {
                return true
            }

            val cls = PsiUtil.resolveClassInType(psiType)
            if (cls != null) {
                if (mapClasses!!.contains(cls.qualifiedName)) {
                    return true
                }
                for (superCls in cls.supers) {
                    if (mapClasses!!.contains(superCls.qualifiedName)) {
                        return true
                    }
                }
            }

            return false
        }

        //represent spring MultipartFile
        val multipartFileInstance = MultipartFile()
//        val multipartFileInstance = "file"

        var fieldModifiers: Set<String> = HashSet(Arrays.asList(PsiModifier.PRIVATE, PsiModifier.PROTECTED))
        var staticFinalFieldModifiers: Set<String> =
            HashSet(Arrays.asList(PsiModifier.STATIC, PsiModifier.FINAL))
        var publicStaticFinalFieldModifiers: Set<String> = HashSet(
            Arrays.asList(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)
        )

        val normalTypes: HashMap<String, Any?> = HashMap()

        var collectionClasses: Set<String>? = null
        var mapClasses: Set<String>? = null
        var castToString: Set<String>? = null
        fun init() {
            if (normalTypes.isEmpty()) {
                normalTypes["Boolean"] = false
                normalTypes["Void"] = null
                normalTypes["Byte"] = 0
                normalTypes["Short"] = 0
                normalTypes["Integer"] = 0
                normalTypes["Long"] = 0L
                normalTypes["Float"] = 0.0F
                normalTypes["Double"] = 0.0
                normalTypes["String"] = ""
                normalTypes["BigDecimal"] = 0.0
                normalTypes["Class"] = null
                normalTypes["java.lang.Boolean"] = false
                normalTypes["java.lang.Void"] = null
                normalTypes["java.lang.Byte"] = 0
                normalTypes["java.lang.Short"] = 0
                normalTypes["java.lang.Integer"] = 0
                normalTypes["java.lang.Long"] = 0L
                normalTypes["java.lang.Float"] = 0.0F
                normalTypes["java.lang.Double"] = 0.0
                normalTypes["java.lang.String"] = ""
                normalTypes["java.math.BigDecimal"] = 0.0
                normalTypes["java.lang.Class"] = null
            }
            if (collectionClasses == null) {
                val collectionClasses = HashSet<String>()
                addClass(Collection::class.java, collectionClasses)
                addClass(List::class.java, collectionClasses)
                addClass(ArrayList::class.java, collectionClasses)
                addClass(LinkedList::class.java, collectionClasses)
                addClass(Set::class.java, collectionClasses)
                addClass(HashSet::class.java, collectionClasses)
                addClass(TreeSet::class.java, collectionClasses)
                addClass(SortedSet::class.java, collectionClasses)
                addClass(Queue::class.java, collectionClasses)
                addClass(Deque::class.java, collectionClasses)
                addClass(ArrayQueue::class.java, collectionClasses)
                addClass(ArrayBlockingQueue::class.java, collectionClasses)
                addClass(Stack::class.java, collectionClasses)
                this.collectionClasses = collectionClasses
            }
            if (mapClasses == null) {
                val mapClasses = HashSet<String>()
                addClass(Map::class.java, mapClasses)
                addClass(HashMap::class.java, mapClasses)
                addClass(LinkedHashMap::class.java, mapClasses)
                this.mapClasses = mapClasses
            }
            if (castToString == null) {
                val castToString = HashSet<String>()
                addClass(Date::class.java, castToString)
                castToString.add("ObjectId")
                castToString.add("org.bson.types.ObjectId")
                this.castToString = castToString
            }
        }

        init {
            init()
        }

        private fun addClass(cls: Class<*>, classSet: HashSet<String>) {
            classSet.add(cls.name!!)
            classSet.add(cls.simpleName!!)
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
