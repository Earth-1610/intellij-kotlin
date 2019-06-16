package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.psi.PsiClassHelper.Companion.ELEMENT_OF_COLLECTION
import com.itangcent.intellij.psi.PsiClassHelper.Companion.JAVA_OBJECT_METHODS
import com.itangcent.intellij.psi.PsiClassHelper.Companion.KEY_OF_MAP
import com.itangcent.intellij.psi.PsiClassHelper.Companion.VALUE_OF_MAP
import com.itangcent.intellij.psi.PsiClassHelper.Companion.fieldModifiers
import com.itangcent.intellij.psi.PsiClassHelper.Companion.hasAnyModify
import com.itangcent.intellij.psi.PsiClassHelper.Companion.isCollection
import com.itangcent.intellij.psi.PsiClassHelper.Companion.isMap
import com.itangcent.intellij.psi.PsiClassHelper.Companion.multipartFileInstance
import com.itangcent.intellij.psi.PsiClassHelper.Companion.normalTypes
import com.itangcent.intellij.psi.PsiClassHelper.Companion.staticFinalFieldModifiers
import com.itangcent.intellij.spring.SpringClassName
import com.itangcent.intellij.util.DocCommentUtils
import com.itangcent.intellij.util.KV
import com.itangcent.intellij.util.invokeMethod
import com.itangcent.intellij.util.reduceSafely
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
                        logger!!.error("error to getTypeObject:$psiType")
                        logger.trace(ExceptionUtils.getStackTrace(e))
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

    protected open fun getTypeObject(clsWithParam: SingleDuckType?, context: PsiElement, option: Int): Any? {

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
    protected open fun getFields(clsWithParam: SingleDuckType, option: Int): KV<String, Any?> {

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
                handle({ getJsonFieldName(field) }, field.type, field)
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

    @Suppress("UNCHECKED_CAST")
    override fun resolveEnumOrStatic(
        classNameWithProperty: String,
        psiMember: PsiMember,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>? {
        val options: ArrayList<HashMap<String, Any?>> = ArrayList()
        var clsName: String? = null
        var property: String? = null
        var cls: PsiClass? = null

        if (classNameWithProperty.contains("#")) {
            clsName = classNameWithProperty.substringBefore("#")
            property = classNameWithProperty.substringAfter("#").trim()
        } else {
            clsName = classNameWithProperty
        }
        cls = resolveClass(clsName, psiMember)
        if (cls == null) return null

        if (cls.isEnum) {
            val enumConstants = parseEnumConstant(cls)

            if (property == null) {
                property = defaultPropertyName
            }

            for (enumConstant in enumConstants) {
                val mappedVal = (enumConstant["params"] as HashMap<String, Any?>?)
                    ?.get(property) ?: continue

                var desc = enumConstant["desc"]
                if (desc == null) {
                    desc = (enumConstant["params"] as HashMap<String, Any?>?)!!
                        .filterKeys { k -> k != property }
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

            if (property.isNullOrBlank()) {
                for (constant in constants) {
                    val mappedVal = constant["value"]
                    val desc = constant["desc"] ?: constant["name"]
                    options.add(
                        KV.create<String, Any?>()
                            .set("value", mappedVal)
                            .set("desc", desc)
                    )
                }
            } else {
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
            }
        }
        return options
    }

    override fun resolveClass(className: String, psiMember: PsiMember): PsiClass? {
        return when {
            className.contains(".") -> tmTypeHelper!!.findClass(className, psiMember)
            else -> getContainingClass(psiMember)?.let { resolveClassFromImport(it, className) }
                ?: tmTypeHelper!!.findClass(className, psiMember)
        }
    }

    override fun getContainingClass(psiMember: PsiMember): PsiClass? {
        psiMember.containingClass?.let { return it }
        if (psiMember is PsiClass) return psiMember
        return null
    }

    protected open fun resolveClassFromImport(psiClass: PsiClass, clsName: String): PsiClass? {

        val imports = PsiTreeUtil.findChildrenOfType(psiClass.context, PsiImportStatement::class.java)

        var cls = imports
            .mapNotNull { it.qualifiedName }
            .firstOrNull { it.endsWith(".$clsName") }
            ?.let { tmTypeHelper!!.findClass(it, psiClass) }
        if (cls != null) {
            return cls
        }

        val defaultPackage = psiClass.qualifiedName!!.substringBeforeLast(".")
        cls = tmTypeHelper!!.findClass("$defaultPackage.$clsName", psiClass)
        if (cls != null) {
            return cls
        }
        cls = imports
            .mapNotNull { it.qualifiedName }
            .filter { it.endsWith(".*") }
            .map { it -> it.removeSuffix("*") + clsName }
            .map { tmTypeHelper.findClass(it, psiClass) }
            .firstOrNull()

        return cls
    }

    protected open val staticResolvedInfo: HashMap<PsiClass, List<Map<String, Any?>>> = HashMap()

    override fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>> {
        val resourceClass = getResourceClass(psiClass)
        if (staticResolvedInfo.containsKey(resourceClass)) {
            return staticResolvedInfo[resourceClass]!!
        }
        val res = ArrayList<Map<String, Any?>>()
        val checkModifier: Set<String> =
            if (resourceClass.isInterface) {
                staticFinalFieldModifiers
            } else {
                PsiClassHelper.publicStaticFinalFieldModifiers
            }
        for (field in resourceClass.allFields) {

            if (!hasAnyModify(field, checkModifier)) {
                continue
            }

            val value = field.computeConstantValue() ?: continue

            val constant = HashMap<String, Any?>()
            constant.put("name", field.name)
            constant.put("value", value.toString())
            constant.put("desc", getAttrOfField(field))
            res.add(constant)
        }
        staticResolvedInfo[resourceClass] = res
        return res
    }

    override fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>> {
        val psiClass = getResourceClass(psiClass)
        if (!psiClass.isEnum) return ArrayList()

        if (staticResolvedInfo.containsKey(psiClass)) {
            return staticResolvedInfo[psiClass]!!
        }

        val res = ArrayList<Map<String, Any?>>()
        for (field in psiClass.allFields) {

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

        staticResolvedInfo[psiClass] = res
        return res
    }

    protected open fun resolveExpr(psiExpression: PsiExpression): Any? {
        if (psiExpression is PsiLiteralExpression) {
            return psiExpression.value
        } else if (psiExpression is PsiReferenceExpression) {
            val value = psiExpression.resolve()
            if (value is PsiField) {
                val constantValue = value.computeConstantValue()
                if (constantValue != null) return constantValue
            }
        } else if (psiExpression is PsiField) {
            return psiExpression.name
        }
        return psiExpression.text
    }

    override fun getAttrOfField(field: PsiField): String? {

        var result: String? = null

        val attrInDoc = DocCommentUtils.getAttrOfDocComment(field.docComment)
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
