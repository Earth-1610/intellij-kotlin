package com.itangcent.intellij.jvm

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.duck.ArrayDuckType
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.duck.SingleUnresolvedDuckType
import com.itangcent.intellij.jvm.element.*
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper
import com.itangcent.intellij.logger.Logger
import org.apache.commons.lang3.StringUtils

@Singleton
open class DuckTypeHelper {

    @Inject
    private val logger: Logger? = null

    @Inject
    private val jvmClassHelper: JvmClassHelper? = null

    @Inject
    private lateinit var psiResolver: PsiResolver

    @Inject
    private val sourceHelper: SourceHelper? = null

    private val classCanonicalTextCache: HashMap<String, PsiClass?> = HashMap()

    private val duckTypeCanonicalTextCache: HashMap<String, DuckType?> = HashMap()

    fun explicit(psiClass: PsiClass): ExplicitClass {
        return ExplicitClassWithOutGenericInfo(this, psiClass)
    }

    fun explicit(psiElement: PsiElement): ExplicitElement<*>? {
        return when (psiElement) {
            is PsiClass -> {
                explicit(psiElement)
            }
            is PsiMethod -> {
                ExplicitMethodWithOutGenericInfo(explicit(psiElement.containingClass!!), psiElement)
            }
            is PsiField -> {
                ExplicitFieldWithOutGenericInfo(explicit(psiElement.containingClass!!), psiElement)
            }
            else -> {
                logger!!.error("can not explicit PsiElement beyond class/method/field:$psiElement")
                null
            }
        }
    }

    fun explicit(singleDuckType: SingleDuckType): ExplicitClass {
        return if (singleDuckType.genericInfo.isNullOrEmpty()) {
            ExplicitClassWithOutGenericInfo(this, singleDuckType.psiClass())
        } else {
            ExplicitClassWithGenericInfo(
                this, singleDuckType.genericInfo,
                singleDuckType.psiClass()
            )
        }
    }

    fun ensureType(type: PsiType): DuckType? {
        if (type is PsiClassType) {
            val parameters = type.parameters
            if (parameters.isNullOrEmpty()) {
                return type.resolve()?.let { SingleDuckType(it) }
            } else {
                val psiClass = type.resolve() ?: return null
                val genericInfo: HashMap<String, DuckType?> = LinkedHashMap()
                for ((index, typeParameter) in psiClass.typeParameters.withIndex()) {
                    if (parameters.size > index) {
                        val typeParam = parameters[index]
                        genericInfo[typeParameter.name!!] = ensureType(typeParam)
                    } else {
                        genericInfo[typeParameter.name!!] = ensureTypeToClass(typeParameter, null)
                    }
                }
                return type.resolve()?.let { SingleDuckType(psiClass, genericInfo) }
            }
        }

        if (type is PsiArrayType) {
            return ensureType(type.componentType)?.let { ArrayDuckType(it) }
        }

        if (type is PsiDisjunctionType) {
            val lub = type.leastUpperBound
            if (lub is PsiClassType) {
                return lub.resolve()?.let { SingleDuckType(it) }
            }
        }

        if (type is PsiPrimitiveType) {
            return SingleUnresolvedDuckType(type)
        }

        if (type is PsiWildcardType) {
            type.bound?.let { return ensureType(it) }
        }

        if (type is PsiClass) {
            return ensureTypeToClass(type, null)
        }

        return SingleUnresolvedDuckType(type)

    }

    fun ensureType(psiType: PsiType, typeParams: Map<String, DuckType?>?): DuckType? {
        if (typeParams == null) {
            ensureType(psiType)
        }

        if (psiType is PsiClassType) {
            val parameters = psiType.parameters
            if (parameters.isNullOrEmpty()) {
                return psiType.resolve()?.let { ensureTypeToClass(it, typeParams) }
            } else {
                val psiClass = psiType.resolve() ?: return null
                val genericInfo: HashMap<String, DuckType?> = LinkedHashMap()
                for ((index, typeParameter) in psiClass.typeParameters.withIndex()) {
                    if (parameters.size > index) {
                        val typeParam = parameters[index]
                        genericInfo[typeParameter.name!!] = ensureType(typeParam, typeParams)
                    } else {
                        genericInfo[typeParameter.name!!] = ensureTypeToClass(typeParameter, typeParams)
                    }
                }
                return SingleDuckType(psiClass, genericInfo)
            }
        }

        if (psiType is PsiArrayType) {
            return ensureType(psiType.componentType, typeParams)?.let { ArrayDuckType(it) }
        }

        if (psiType is PsiDisjunctionType) {
            val lub = psiType.leastUpperBound
            if (lub is PsiClassType) {
                return lub.resolve()?.let { ensureTypeToClass(it, typeParams) }
            }
        }

        if (psiType is PsiPrimitiveType) {
            return SingleUnresolvedDuckType(psiType)
        }

        if (psiType is PsiWildcardType) {
            psiType.bound?.let { return ensureType(it, typeParams) }
        }

        if (psiType is PsiClass) {
            return ensureTypeToClass(psiType, null)
        }

        return SingleUnresolvedDuckType(psiType)
    }

    private fun ensureTypeToClass(psiClass: PsiClass, typeParams: Map<String, DuckType?>?): DuckType? {

        if (psiClass is PsiTypeParameter) {
            val realType = typeParams?.get(psiClass.name)
            if (realType != null) {
                return realType
            }
        } else if (psiClass.hasTypeParameters()) {
            val genericInfo: HashMap<String, DuckType?> = LinkedHashMap()
            for (typeParameter in psiClass.typeParameters) {
                genericInfo[typeParameter.name!!] = ensureTypeToClass(typeParameter, typeParams)
            }
            return SingleDuckType(psiClass, genericInfo)
        }
        return SingleDuckType(psiClass)
    }

    /**
     * resolve canonical representation of the type to DuckType
     */
    fun resolve(psiType: PsiType, context: PsiElement): DuckType? {
        val typeCanonicalText = psiType.canonicalText
        if (typeCanonicalText.contains('<') && typeCanonicalText.endsWith('>')
            || typeCanonicalText.endsWith("[]")
        ) {
            val clsWithParam = resolve(typeCanonicalText, context)
            if (clsWithParam != null) {
                return clsWithParam
            }
        }
        if (psiType is PsiPrimitiveType) {
            return SingleUnresolvedDuckType(psiType)
        }
        val paramCls = PsiUtil.resolveClassInClassTypeOnly(psiType)
        return when (paramCls) {
            null -> null
            else -> SingleDuckType(paramCls)
        }
    }

    fun resolve(typeCanonicalText: String, context: PsiElement): DuckType? {
        return duckTypeCanonicalTextCache.safeComputeIfAbsent(typeCanonicalText) {
            doResolve(typeCanonicalText, context)
        }
    }

    private fun doResolve(typeCanonicalText: String, context: PsiElement): DuckType? {

        when {
            typeCanonicalText == "?" -> {
                return javaLangObjectType(context)
            }
            typeCanonicalText.endsWith("[]") -> {
                val componentTypeCanonicalText = typeCanonicalText.removeSuffix(ARRAY_SUFFIX)
                return resolve(componentTypeCanonicalText, context)?.let {
                    ArrayDuckType(
                        it
                    )
                }
            }
            typeCanonicalText.startsWith("? extends") -> return resolve(
                typeCanonicalText.removePrefix("? extends").trim(),
                context
            )
            typeCanonicalText.contains('<') && typeCanonicalText.endsWith('>') -> {
                val paramCls = extractClassFromCanonicalText(typeCanonicalText, context)
                if (paramCls != null) {

                    val typeParams = extractTypeParams(typeCanonicalText)

                    if (typeParams != null) {
                        val typeParameterMap: HashMap<String, DuckType?> = LinkedHashMap()
                        for ((index, typeParameter) in paramCls.typeParameters.withIndex()) {
                            if (typeParams.size > index) {
                                val typeParam = typeParams[index]
                                typeParameterMap[typeParameter.name!!] = resolve(typeParam, context)
                            }
                        }
                        return SingleDuckType(paramCls, typeParameterMap)
                    }
                    return SingleDuckType(paramCls)
                }
                logger!!.error("error to find class:$typeCanonicalText")
                return null
            }
            StandardJvmClassHelper.isPrimitive(typeCanonicalText) -> {
                return SingleUnresolvedDuckType(
                    StandardJvmClassHelper.getPrimitiveType(
                        typeCanonicalText
                    )!!
                )
            }
            else -> {
                val paramCls = resolveClass(typeCanonicalText, context)
                return when (paramCls) {
                    null -> {
                        if (typeCanonicalText.length == 1) {//maybe generic
                            return javaLangObjectType(context)
                        }
                        logger!!.warn("error to resolve class:$typeCanonicalText")
                        null
                    }
                    else -> {
                        SingleDuckType(paramCls)
                    }
                }
            }
        }
    }

    fun resolveClass(fqClassName: String, context: PsiElement): PsiClass? {
        return when {
            fqClassName.contains(".") -> psiResolver.findClass(fqClassName, context)
            else -> psiResolver.resolveClass(fqClassName, context)
        }?.let { sourceHelper?.getSourceClass(it) }
    }

    fun extractClassFromCanonicalText(typeCanonicalText: String, context: PsiElement): PsiClass? {
        val classCanonicalText = StringUtils.substringBefore(typeCanonicalText, "<")
        return classCanonicalTextCache.safeComputeIfAbsent(
            classCanonicalText
        ) { resolveClass(classCanonicalText, context) }
    }

    fun extractTypeParams(typeCanonicalText: String): Array<String>? {
        val typeParamsStr = StringUtils.substringAfter(typeCanonicalText, "<")
            .trim()
            .removePrefix("<")
            .removeSuffix(">")
        if (StringUtils.isBlank(typeParamsStr))
            return null
        val params: ArrayList<String> = ArrayList()
        var param = ""
        var waitGt = 0
        for (ch in typeParamsStr) {
            when {
                ch == '<' -> {
                    param += ch
                    ++waitGt
                }
                ch == '>' -> {
                    param += ch
                    --waitGt
                }
                waitGt > 0 -> {
                    param += ch
                }
                ch == ',' -> {
                    when {
                        waitGt > 0 -> param += ch
                        else -> {
                            if (StringUtils.isNotBlank(param)) {
                                params.add(param.trim())
                            }
                            param = ""
                        }
                    }
                }
                else -> param += ch
            }
        }
        if (StringUtils.isNotBlank(param)) {
            params.add(param.trim())
        }
        return params.toTypedArray()
    }

    fun buildPsiType(type: String, context: PsiElement): PsiType? {

        if (!type.contains('<')) {
            val cls = resolveClass(type, context)
            if (cls != null) {
                return PsiTypesUtil.getClassType(cls)
            }
        }

        val duckType: DuckType = resolve(type, context) ?: return null

        return buildPsiType(duckType, context)
    }

    fun buildPsiType(duckType: DuckType, context: PsiElement): PsiType? {
        when (duckType) {
            is ArrayDuckType -> {
                return buildPsiType(duckType.componentType(), context)?.let { PsiArrayType(it) }
            }
            is SingleDuckType -> {
                return when {
                    duckType.genericInfo.isNullOrEmpty() -> PsiTypesUtil.getClassType(duckType.psiClass())
                    else -> {
                        try {
                            val params = duckType.genericInfo.values.map { dt ->
                                buildPsiType(dt!!, context)!!
                            }.toTypedArray()
                            createType(duckType.psiClass(), context, *params)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
            else -> {
                return null
            }

        }
    }

    fun createType(clazz: PsiClass, context: PsiElement?, vararg parameters: PsiType): PsiClassType {
        return JavaPsiFacade.getInstance(
            (context ?: clazz).project
        ).elementFactory.createType(clazz, *parameters)
    }

    fun createType(psiType: PsiType, context: PsiElement?, vararg parameters: PsiType): PsiClassType? {
        return jvmClassHelper!!.resolveClassInType(psiType)?.let { createType(it, context, *parameters) }
    }

    fun findType(canonicalText: String, context: PsiElement): PsiType? {
        return ActionContext.instance(PsiResolver::class).findType(canonicalText, context)
    }

    //region isQualified--------------------------------------------------------

    private val qualifiedInfoCache: HashMap<PsiType, Boolean> = HashMap()

    fun isQualified(psiType: PsiType, context: PsiElement): Boolean {
        return qualifiedInfoCache.safeComputeIfAbsent(psiType) {
            val duckType = resolve(psiType, context) ?: return@safeComputeIfAbsent true
            return@safeComputeIfAbsent isQualified(duckType)
        } ?: false
    }

    fun isQualified(duckType: DuckType): Boolean {
        if (duckType is SingleDuckType) {
            if (duckType.psiClass().qualifiedName == CommonClassNames.JAVA_LANG_OBJECT) {
                return false
            }
            if (duckType.psiClass().isInterface) {
                if (!jvmClassHelper!!.isCollection(duckType.psiClass()) && !jvmClassHelper.isMap(duckType.psiClass())) {
                    return false
                }
            }

            val typeParameterCount = duckType.psiClass().typeParameters.size
            if (typeParameterCount == 0) return true
            if (typeParameterCount < duckType.genericInfo?.size ?: 0) {
                return false
            }
            if (duckType.genericInfo == null) return false

            for (value in duckType.genericInfo.values) {
                if (value == null) return false
                if (!isQualified(value)) {
                    return false
                }
            }

            return true
        } else if (duckType is ArrayDuckType) {
            return isQualified(duckType.componentType())
        }
        return true
    }

    fun javaLangObjectType(context: PsiElement): SingleDuckType {
        return SingleDuckType(
            psiResolver.findClass(
                CommonClassNames.JAVA_LANG_OBJECT,
                context
            )!!
        )
    }

    //endregion isQualified--------------------------------------------------------

    companion object {
        const val ARRAY_SUFFIX = "[]"
    }
}