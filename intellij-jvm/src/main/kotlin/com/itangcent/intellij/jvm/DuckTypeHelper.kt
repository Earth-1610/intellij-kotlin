package com.itangcent.intellij.jvm

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.logger.Logger
import com.siyeh.ig.psiutils.ClassUtils
import org.apache.commons.lang3.StringUtils
import java.util.*

@Singleton
class DuckTypeHelper {

    @Inject
    private val logger: Logger? = null

    private val classCanonicalTextCache: HashMap<String, PsiClass?> = HashMap()

    private val duckTypeCanonicalTextCache: HashMap<String, DuckType?> = HashMap()

    private val nameToClassCache: HashMap<String, PsiClass?> = LinkedHashMap()
    private val nameToTypeCache: HashMap<String, PsiType?> = LinkedHashMap()

    fun ensureType(type: PsiType): DuckType? {
        if (type is PsiClassType) {
            return type.resolve()?.let { SingleDuckType(it) }
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
            return SinglePrimitiveDuckType(type)
        }
        return null

    }

    fun ensureType(psiType: PsiType, typeParams: Map<String, DuckType?>?): DuckType? {
        if (typeParams == null) {
            ensureType(psiType)
        }

        if (psiType is PsiClassType) {
            return psiType.resolve()?.let { ensureTypeToClass(it, typeParams) }
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
            return SinglePrimitiveDuckType(psiType)
        }
        return null
    }

    private fun ensureTypeToClass(psiType: PsiClass, typeParams: Map<String, DuckType?>?): DuckType? {

        if (psiType is PsiTypeParameter) {
            val realType = typeParams?.get(psiType.name)
            if (realType != null) {
                return realType
            }
        }
        return SingleDuckType(psiType)
    }

    /**
     * resolve canonical representation of the type to TmType
     */
    fun resolve(psiType: PsiType, context: PsiElement): DuckType? {
        val typeCanonicalText = psiType.canonicalText
        if (typeCanonicalText.contains('<') && typeCanonicalText.endsWith('>')) {
            val clsWithParam = resolve(typeCanonicalText, context)
            if (clsWithParam != null) {
                return clsWithParam
            }
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
                return SingleDuckType(
                    findClass(
                        CommonClassNames.JAVA_LANG_OBJECT,
                        context
                    )!!
                )
            }
            typeCanonicalText.endsWith("[]") -> {
                val componentTypeCanonicalText = typeCanonicalText.removeSuffix(ARRAY_SUFFIX)
                return resolve(componentTypeCanonicalText, context)?.let { ArrayDuckType(it) }
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
            else -> {
                val paramCls = findClass(typeCanonicalText, context)
                return when (paramCls) {
                    null -> {
                        if (typeCanonicalText.length == 1) {//maybe generic
                            return SingleDuckType(
                                findClass(
                                    CommonClassNames.JAVA_LANG_OBJECT,
                                    context
                                )!!
                            )
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

    fun findClass(fqClassName: String, context: PsiElement): PsiClass? {
        if (fqClassName.isEmpty()) return null

        if (nameToClassCache.contains(fqClassName)) {
            return nameToClassCache[fqClassName]
        }

        if (fqClassName.contains("<")) {
            return findClass(fqClassName.substringBefore('<'), context)
        }

        var cls: PsiClass? = null
        try {
            cls = ClassUtils.findClass(fqClassName, context)
        } catch (e: Exception) {
        }

        if (cls == null) {
            if (fqClassName.contains("")) return null

            try {
                cls = ClassUtils.findClass("java.lang." + fqClassName.capitalize(), context)
            } catch (e: Exception) {
            }
            if (cls == null) {
                try {
                    cls = ClassUtils.findClass("java.util." + fqClassName.capitalize(), context)
                } catch (e: Exception) {
                }
            }
        }
        nameToClassCache[fqClassName] = cls
        return cls
    }

    fun extractClassFromCanonicalText(typeCanonicalText: String, context: PsiElement): PsiClass? {
        val classCanonicalText = StringUtils.substringBefore(typeCanonicalText, "<")
        return classCanonicalTextCache.safeComputeIfAbsent(
            classCanonicalText
        ) { findClass(classCanonicalText, context) }
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
            val cls = findClass(type, context)
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

    fun findType(canonicalText: String, context: PsiElement): PsiType? {
        return nameToTypeCache.safeComputeIfAbsent(canonicalText) {
            return@safeComputeIfAbsent buildPsiType(canonicalText, context)
        }
    }

    //region isQualified--------------------------------------------------------
    private val qualifiedInfoCache: HashMap<PsiType, Boolean> = HashMap()

    fun isQualified(psiType: PsiType, context: PsiElement): Boolean {
        return qualifiedInfoCache.safeComputeIfAbsent(psiType) {
            val tmType = resolve(psiType, context) ?: return@safeComputeIfAbsent true
            return@safeComputeIfAbsent isQualified(tmType)
        } ?: false
    }

    private fun isQualified(tmType: DuckType): Boolean {
        if (tmType is SingleDuckType) {
            if (tmType.psiClass().qualifiedName == CommonClassNames.JAVA_LANG_OBJECT) {
                return false
            }
            if (tmType.psiClass().isInterface) {
                return false
            }
            val typeParameterCount = tmType.psiClass().typeParameters.size
            if (typeParameterCount == 0) return true
            if (typeParameterCount < tmType.genericInfo?.size ?: 0) {
                return false
            }
            if (tmType.genericInfo == null) return false

            for (value in tmType.genericInfo.values) {
                if (value == null) return false
                if (!isQualified(value)) {
                    return false
                }
            }

            return true
        } else if (tmType is ArrayDuckType) {
            return isQualified(tmType.componentType())
        }
        return true
    }
    //endregion isQualified--------------------------------------------------------

    companion object {
        const val ARRAY_SUFFIX = "[]"
    }
}