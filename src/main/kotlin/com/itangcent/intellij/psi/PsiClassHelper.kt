package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.containers.stream
import com.itangcent.common.utils.CollectionUtils
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.spring.MultipartFile
import com.itangcent.intellij.spring.SpringClassName
import com.itangcent.intellij.util.DocCommentUtils
import com.itangcent.intellij.util.KV
import com.itangcent.intellij.util.invokeMethod
import com.itangcent.intellij.util.reduceSafely
import com.sun.jmx.remote.internal.ArrayQueue
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.reflect.full.createInstance

class PsiClassHelper {

    private val resolvedInfo: HashMap<Int, HashMap<Any, Any?>> = HashMap()

    @Inject
    private val logger: Logger? = null

    @Inject
    private val tmTypeHelper: TmTypeHelper? = null

    @Inject(optional = true)
    private val classRuleConfig: ClassRuleConfig? = null

    @Suppress("UNCHECKED_CAST")
    private fun <T> getResolvedInfo(key: Any?, option: Int): T? {
        if (key == null) return null
        val value = getCache(option)[key] ?: return null

        return copy(value) as T?
    }

    @Suppress("UNCHECKED_CAST")
    private fun copy(obj: Any?): Any? {
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

    private fun cacheResolvedInfo(key: Any?, value: Any?, option: Int) {
        if (key != null) {
            getCache(option)[key] = value
        }
    }

    private fun getCache(option: Int): HashMap<Any, Any?> {
        return resolvedInfo.computeIfAbsent(option, { HashMap() })
    }

    fun getTypeObject(psiType: PsiType?, context: PsiElement): Any? {
        return getTypeObject(psiType, context, JsonOption.NONE)
    }

    fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any? {

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
                cacheResolvedInfo(castTo, list, option)//cache
                when {
                    deepType is PsiPrimitiveType -> list.add(PsiTypesUtil.getDefaultValue(deepType))
                    isNormalType(deepType.canonicalText) -> list.add(getDefaultValue(deepType.canonicalText)!!)
                    else -> getTypeObject(deepType, context, option)?.let { list.add(it) }
                }
                return copy(list)
            }
            castTo.canonicalText == SpringClassName.MULTIPARTFILE -> {
                cacheResolvedInfo(castTo, multipartFileInstance, option)//cache
                return multipartFileInstance
            }
            isCollection(castTo) -> {   //list type

                val list = java.util.ArrayList<Any>()
                cacheResolvedInfo(castTo, list, option)//cache
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
                cacheResolvedInfo(castTo, map, option)//cache
                val keyType = PsiUtil.substituteTypeParameter(castTo, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                val valueType = PsiUtil.substituteTypeParameter(castTo, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                var defaultKey: Any? = null
                if (keyType != null) {
                    defaultKey = if (keyType == psiType) {
                        "嵌套类型"
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
                        cacheResolvedInfo(castTo, result, option)
                        return copy(result)
                    } else {
                        return null
                    }
                } else {
                    val paramCls = PsiUtil.resolveClassInClassTypeOnly(castTo)
                    try {
                        val result = getFields(paramCls, option)
                        cacheResolvedInfo(castTo, result, option)
                        return result
                    } catch (e: Throwable) {
                        logger!!.error("error to getTypeObject:$psiType" + ExceptionUtils.getStackTrace(e))
                        return null
                    }
                }
            }
        }
    }

    fun getFields(psiClass: PsiClass?): KV<String, Any?> {
        return getFields(psiClass, JsonOption.NONE)
    }

    @Suppress("UNCHECKED_CAST")
    fun getFields(psiClass: PsiClass?, option: Int): KV<String, Any?> {

        if (psiClass != null) {
            val resolvedInfo = getResolvedInfo<Any>(psiClass, option)
            if (resolvedInfo != null) {
                return resolvedInfo as KV<String, Any?>
            }
        }

        val kv: KV<String, Any?> = KV.create()
        var commentKV: KV<String, Any?>? = null

        if (psiClass != null) {
            cacheResolvedInfo(psiClass, kv, option)//cache

            foreachField(psiClass, option, { fieldName, fieldType, fieldOrMethod ->
                if (!kv.contains(fieldName)) {

                    //doc comment
                    if (JsonOption.needComment(option)) {
                        if (commentKV == null) {
                            commentKV = KV.create()
                        }
                        if (fieldOrMethod is PsiField) {
                            val field: PsiField = fieldOrMethod
                            commentKV!![fieldName] = getAttrOfField(field)
                            resolveDeprecatedDoc(field, commentKV!!)
                            resolveSeeDoc(field, commentKV!!)
                        } else if (fieldOrMethod is PsiMethod) {
                            val attrInDoc = DocCommentUtils.getAttrOfDocComment(fieldOrMethod.docComment)
                            if (StringUtils.isNotBlank(attrInDoc)) {
                                commentKV!![fieldName] = attrInDoc
                            }
                        }
                    }

//                kv[name] = getTypeObject(type, psiClass.context!!, option)

                    if (fieldType is PsiPrimitiveType) {       //primitive Type
                        kv[fieldName] = PsiTypesUtil.getDefaultValue(fieldType)
                    } else {    //reference Type

                        if (fieldType == PsiType.NULL) {
                            kv[fieldName] = null
                            return@foreachField
                        }

                        val type = tryCastTo(fieldType, psiClass)
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
                                    iterableClass == psiClass -> list.add(Collections.emptyMap<Any, Any>())
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
                                        keyType == type -> "嵌套类型"
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
                                if (clsOfType != psiClass) {
                                    kv[fieldName] = getFields(clsOfType, option)
                                }
                            }

                        }
                    }
                }
            })

            if (JsonOption.needComment(option) && commentKV != null && commentKV!!.size > 0) {
                kv["@comment"] = commentKV
            }
        }

        return copy(kv) as KV<String, Any?>
    }

    private fun getTypeObject(tmType: TmType?, context: PsiElement, option: Int): Any? {

        val resolvedInfo = getResolvedInfo<Any>(tmType, option)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        if (tmType == null) return null

        if (tmType is ArrayTmType) {
            val list = ArrayList<Any>()
            cacheResolvedInfo(tmType, list, option)
            getTypeObject(tmType.componentType, context, option)?.let { list.add(it) }
            return copy(list)
        }

        if (tmType is SingleTmType) {
            return getTypeObject(tmType as SingleTmType, context, option)
        }
        return null
    }

    private fun getTypeObject(clsWithParam: SingleTmType?, context: PsiElement, option: Int): Any? {

        if (clsWithParam != null) {
            val resolvedInfo = getResolvedInfo<Any>(clsWithParam, option)
            if (resolvedInfo != null) {
                return resolvedInfo
            }
        }

        if (clsWithParam == null) return null
        val psiClass: PsiClass = clsWithParam.psiCls
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
                    cacheResolvedInfo(clsWithParam, list, option)
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

                    cacheResolvedInfo(clsWithParam, list, option)
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
                    cacheResolvedInfo(clsWithParam, map, option)
                    val keyType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                    val valueType = PsiUtil.substituteTypeParameter(type, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                    var defaultKey: Any? = null

                    if (keyType == null) {
                        val realKeyType = clsWithParam.typeParams?.get(KEY_OF_MAP)
                        defaultKey = getTypeObject(realKeyType, context, option)
                    }

                    if (defaultKey == null) {
                        if (keyType != null) {
                            defaultKey = getTypeObject(
                                tmTypeHelper!!.ensureType(keyType, clsWithParam.typeParams),
                                context,
                                option
                            )
                        } else {
                            defaultKey = ""
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
                    return getFields(clsWithParam, context, option)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getFields(clsWithParam: SingleTmType, context: PsiElement, option: Int): KV<String, Any?> {

        val resolvedInfo = getResolvedInfo<Any>(clsWithParam, option)
        if (resolvedInfo != null) {
            return resolvedInfo as KV<String, Any?>
        }

        val psiClass: PsiClass? = clsWithParam.psiCls
        val kv: KV<String, Any?> = KV.create()
        cacheResolvedInfo(clsWithParam, kv, option)
        var commentKV: KV<String, Any?>? = null

        if (psiClass != null) {

            foreachField(psiClass, option, { fieldName, fieldType, fieldOrMethod ->
                if (!kv.contains(fieldName)) {
                    //doc comment
                    if (JsonOption.needComment(option)) {
                        if (commentKV == null) {
                            commentKV = KV.create()
                        }
                        if (fieldOrMethod is PsiField) {
                            val field: PsiField = fieldOrMethod
                            commentKV!![fieldName] = getAttrOfField(field)
                            resolveDeprecatedDoc(field, commentKV!!)
                            resolveSeeDoc(field, commentKV!!)
                        } else if (fieldOrMethod is PsiMethod) {
                            val attrInDoc = DocCommentUtils.getAttrOfDocComment(fieldOrMethod.docComment)
                            if (StringUtils.isNotBlank(attrInDoc)) {
                                commentKV!![fieldName] = attrInDoc
                            }
                        }
                    }

                    if (fieldType is PsiPrimitiveType) {//primitive Type
                        kv[fieldName] = PsiTypesUtil.getDefaultValue(fieldType)
                        return@foreachField
                    }

                    //reference Type
                    val fieldTypeName = fieldType.canonicalText

                    if (isNormalType(fieldTypeName)) {//normal Type
                        kv[fieldName] = getDefaultValue(fieldTypeName)
                        return@foreachField
                    }
                    val clsOfType = PsiUtil.resolveClassInType(fieldType)

                    if (clsOfType is PsiTypeParameter) {
                        val typeParams = clsWithParam.typeParams
                        if (typeParams != null) {
                            val realType = typeParams[clsOfType.name]
                            if (realType != null) {
                                kv[fieldName] = getTypeObject(realType, context, option)
                                return@foreachField
                            }
                        }
                    }

                    val type = tryCastTo(fieldType, psiClass)
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
                                    tmTypeHelper!!.ensureType(deepType, clsWithParam.typeParams),
                                    context,
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
                                iterableClass == psiClass -> list.add(Collections.emptyMap<Any, Any>())
                                iterableType != null -> getTypeObject(
                                    tmTypeHelper!!.ensureType(
                                        iterableType,
                                        clsWithParam.typeParams
                                    ), context, option
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
                                tmTypeHelper!!.ensureType(keyType, clsWithParam.typeParams),
                                context,
                                option
                            )
                            if (defaultKey == null) defaultKey = ""

                            var defaultValue: Any? = null
                            if (valueType != null) defaultValue = getTypeObject(
                                tmTypeHelper!!.ensureType(valueType, clsWithParam.typeParams),
                                context,
                                option
                            )
                            if (defaultValue == null) defaultValue = null

                            map[defaultKey] = defaultValue

                            kv[fieldName] = map
                        }
                        else -> //class type
                        {
                            if (clsOfType != psiClass) {
                                kv[fieldName] = getFields(clsOfType, option)
                            }
                        }
                    }


                }
            })

            if (JsonOption.needComment(option) && commentKV != null && commentKV!!.size > 0) {
                kv["@comment"] = commentKV
            }
        }

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
            if (CollectionUtils.containsAny(field.modifiers, jvmStaticFinalFieldModifiers)) {
                continue
            }

            if (!CollectionUtils.containsAny(field.modifiers, jvmFieldModifiers)) {
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
                if (method.hasModifier(JvmModifier.STATIC)) continue
                if (!method.hasModifier(JvmModifier.PUBLIC)) continue

                method.returnType?.let { handle(propertyName, it, method) }
            }
        }
        fieldNames?.clear()
    }

    private fun propertyNameOfGetter(methodName: String): String? {
        return when {
            methodName.startsWith("get") -> methodName.removePrefix("get")
            methodName.startsWith("is") -> methodName.removePrefix("is")
            else -> null
        }?.decapitalize()
    }

    fun getAttrOfField(field: PsiField): String? {

        val attrInDoc = DocCommentUtils.getAttrOfDocComment(field.docComment)
        if (StringUtils.isNotBlank(attrInDoc))
            return attrInDoc

        if (classRuleConfig != null) {
            val docByRule = classRuleConfig.findDoc(field)
            if (docByRule != null) return docByRule
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
            if (StringUtils.isNotBlank(innerDoc))
                return innerDoc
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveDeprecatedDoc(field: PsiField, comment: HashMap<String, Any?>) {
        val deprecatedInfo = DocCommentUtils.findDocsByTag(field.docComment, "deprecated")
        if (deprecatedInfo != null) {
            val fieldName = field.name
            val oldComment = comment[fieldName]
            if (oldComment == null) {
                comment[fieldName] = "[字段已废弃]$deprecatedInfo"
            } else {
                comment[fieldName] = "$oldComment\n[字段已废弃]$deprecatedInfo"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveSeeDoc(field: PsiField, comment: HashMap<String, Any?>) {
        val sees = getSees(field)
        if (sees.isNullOrEmpty()) return

        val options: ArrayList<HashMap<String, Any?>> = ArrayList()

        sees!!.forEach { see ->
            resolveEnumOrStatic(see, field, field.name)?.let { options.addAll(it) }
        }

        if (options.isNotEmpty()) {
            comment["${field.name}@options"] = options
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveEnumOrStatic(
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
                        .reduceSafely { s1, s2 -> s1 + " " + s2 }
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

    fun resolveClass(className: String, psiMember: PsiMember): PsiClass? {
        return when {
            className.contains(".") -> tmTypeHelper!!.findClass(className, psiMember)
            else -> getContainingClass(psiMember)?.let { resolveClassFromImport(it, className) }
                ?: tmTypeHelper!!.findClass(className, psiMember)
        }
    }

    fun getContainingClass(psiMember: PsiMember): PsiClass? {
        psiMember.containingClass?.let { return it }
        if (psiMember is PsiClass) return psiMember
        return null
    }

    fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement? {
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

    private fun resolveClassFromImport(psiClass: PsiClass, clsName: String): PsiClass? {

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

    private fun getSees(field: PsiJavaDocumentedElement): List<String>? {
        val docComment = field.docComment ?: return null

        val sees: ArrayList<String> = ArrayList()
        docComment.findTagsByName("see")
            .map { psiDocTag ->
                psiDocTag.dataElements.stream()
                    .map { it.text }
                    .filter { it != null }
                    .reduce { s1, s2 -> s1 + s2 }
                    .orElse(null)
            }
            .forEach { it?.let { sees.add(it) } }
        if (sees.isEmpty()) return null
        return sees
    }

    private val staticResolvedInfo: HashMap<PsiClass, List<Map<String, Any?>>> = HashMap()

    fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>> {
        if (staticResolvedInfo.containsKey(psiClass)) {
            return staticResolvedInfo[psiClass]!!
        }
        val res = ArrayList<Map<String, Any?>>()
        val checkModifier: Set<JvmModifier> =
            if (psiClass.isInterface) {
                jvmStaticFinalFieldModifiers
            } else {
                jvmPublicStaticFinalFieldModifiers
            }
        for (field in psiClass.allFields) {

            if (!CollectionUtils.containsAll(field.modifiers, checkModifier)) {
                continue
            }

            val value = field.computeConstantValue() ?: continue

            val constant = HashMap<String, Any?>()
            constant.put("name", field.name)
            constant.put("value", value.toString())
            constant.put("desc", getAttrOfField(field))
            res.add(constant)
        }
        staticResolvedInfo[psiClass] = res
        return res
    }

    fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>> {
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
            val exps = value.argumentList?.expressions
            val parameters = construct?.parameterList?.parameters
            if (exps != null && parameters != null) {
                for ((index, parameter) in parameters.withIndex()) {
                    params[parameter.name!!] = resolveExpr(exps[index])
                }
            }
            constant.put("params", params)
            constant.put("name", field.name)
            constant.put("desc", getAttrOfField(field))
            res.add(constant)
        }

        staticResolvedInfo[psiClass] = res
        return res
    }

    private fun resolveExpr(psiExpression: PsiExpression): Any? {
        if (psiExpression is PsiLiteralExpression) {
            return psiExpression.value
        } else if (psiExpression is PsiReferenceExpression) {
            val value = psiExpression.resolve()
            if (value is PsiField) {
                val constantValue = value.computeConstantValue()
                if (constantValue != null) return constantValue
            }
        }
        return psiExpression.text
    }

    private val castCache: HashMap<String, PsiType?> = HashMap()

    private fun tryCastTo(psiType: PsiType, context: PsiElement): PsiType {
        if (classRuleConfig == null) return psiType

        val canonicalText = psiType.canonicalText

        if (castCache.contains(canonicalText)) {
            return castCache[canonicalText] ?: psiType
        } else {
            val convertTo = classRuleConfig.tryConvert(canonicalText)

            val convertToType: PsiType
            when (convertTo) {
                null -> convertToType = psiType
                "null" -> convertToType = PsiType.NULL
                else -> {
                    val cls = tmTypeHelper!!.findClass(convertTo, context)
                    if (cls != null) {
                        convertToType = PsiTypesUtil.getClassType(cls)
                    } else {
                        convertToType = psiType
                    }
                }
            }
            castCache[canonicalText] = convertToType
            return convertToType
        }
    }

    fun isNormalType(typeName: String): Boolean {
        return Companion.normalTypes.containsKey(classRuleConfig?.tryConvert(typeName) ?: typeName)
    }

    fun getDefaultValue(typeName: String): Any? {
        return Companion.normalTypes[classRuleConfig?.tryConvert(typeName) ?: typeName]
    }

    fun unboxArrayOrList(psiType: PsiType): PsiType {
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

    fun getJsonFieldName(psiField: PsiField): String {
        if (classRuleConfig != null) {
            val nameByRule = classRuleConfig.getFieldName(psiField)
            if (!nameByRule.isNullOrBlank()) {
                return nameByRule!!
            }
        }
        return psiField.name
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

        private val ELEMENT_OF_COLLECTION = "E"
        private val KEY_OF_MAP = "K"
        private val VALUE_OF_MAP = "V"

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

        var jvmFieldModifiers: Set<JvmModifier> = HashSet(Arrays.asList(JvmModifier.PRIVATE, JvmModifier.PROTECTED))
        var jvmStaticFinalFieldModifiers: Set<JvmModifier> =
            HashSet(Arrays.asList(JvmModifier.STATIC, JvmModifier.FINAL))
        var jvmPublicStaticFinalFieldModifiers: Set<JvmModifier> = HashSet(
            Arrays.asList(JvmModifier.PUBLIC, JvmModifier.STATIC, JvmModifier.FINAL)
        )

        private val normalTypes: HashMap<String, Any?> = HashMap()

        private var collectionClasses: Set<String>? = null
        private var mapClasses: Set<String>? = null
        private var castToString: Set<String>? = null
        private fun init() {
            if (Companion.normalTypes.isEmpty()) {
                Companion.normalTypes["Boolean"] = false
                Companion.normalTypes["Void"] = null
                Companion.normalTypes["Byte"] = 0
                Companion.normalTypes["Short"] = 0
                Companion.normalTypes["Integer"] = 0
                Companion.normalTypes["Long"] = 0L
                Companion.normalTypes["Float"] = 0.0F
                Companion.normalTypes["Double"] = 0.0
                Companion.normalTypes["String"] = ""
                Companion.normalTypes["BigDecimal"] = 0.0
                Companion.normalTypes["Class"] = null
                Companion.normalTypes["java.lang.Boolean"] = false
                Companion.normalTypes["java.lang.Void"] = null
                Companion.normalTypes["java.lang.Byte"] = 0
                Companion.normalTypes["java.lang.Short"] = 0
                Companion.normalTypes["java.lang.Integer"] = 0
                Companion.normalTypes["java.lang.Long"] = 0L
                Companion.normalTypes["java.lang.Float"] = 0.0F
                Companion.normalTypes["java.lang.Double"] = 0.0
                Companion.normalTypes["java.lang.String"] = ""
                Companion.normalTypes["java.math.BigDecimal"] = 0.0
                Companion.normalTypes["java.lang.Class"] = null
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
    public val NONE = 0b0000//没有任何附加选项
    public val READ_COMMENT = 0b0001//解析注释
    public val READ_GETTER = 0b0010//尝试读取可用getter放啊
    public val ALL = READ_COMMENT or READ_GETTER//没有任何附加选项

    public fun needComment(flag: Int): Boolean {
        return (flag and READ_COMMENT) != 0
    }

    public fun readGetter(flag: Int): Boolean {
        return (flag and READ_GETTER) != 0
    }

}
