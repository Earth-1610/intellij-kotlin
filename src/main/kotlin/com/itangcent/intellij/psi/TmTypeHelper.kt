package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.itangcent.intellij.logger.Logger
import com.siyeh.ig.psiutils.ClassUtils
import org.apache.commons.lang3.StringUtils
import java.util.*

class TmTypeHelper {

    @Inject
    private val logger: Logger? = null

    private val classCanonicalTextCache: HashMap<String, PsiClass?> = HashMap()
    private val tmTypeCanonicalTextCache: HashMap<String, TmType?> = HashMap()

    fun ensureType(type: PsiType): TmType? {
        if (type is PsiClassType) {
            return type.resolve()?.let { SingleTmType(it) }
        }
        if (type is PsiArrayType) {
            return ensureType(type.componentType)?.let { ArrayTmType(it) }
        }
        if (type is PsiDisjunctionType) {
            val lub = type.leastUpperBound
            if (lub is PsiClassType) {
                return lub.resolve()?.let { SingleTmType(it) }
            }
        }
        return null

    }

    fun ensureType(psiType: PsiType, typeParams: Map<String, TmType?>?): TmType? {
        if (typeParams == null) {
            ensureType(psiType)
        }

        if (psiType is PsiClassType) {
            return psiType.resolve()?.let { ensureTypeToClass(it, typeParams) }
        }

        if (psiType is PsiArrayType) {
            return ensureType(psiType.componentType, typeParams)?.let { ArrayTmType(it) }
        }

        if (psiType is PsiDisjunctionType) {
            val lub = psiType.leastUpperBound
            if (lub is PsiClassType) {
                return lub.resolve()?.let { ensureTypeToClass(it, typeParams) }
            }
        }

        return null
    }

    private fun ensureTypeToClass(psiType: PsiClass, typeParams: Map<String, TmType?>?): TmType? {

        if (psiType is PsiTypeParameter) {
            val realType = typeParams?.get(psiType.name)
            if (realType != null) {
                return realType
            }
        }
        return SingleTmType(psiType)
    }

    /**
     * resolve canonical representation of the type to TmType
     */
    fun resolve(psiType: PsiType, context: PsiElement): TmType? {
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
            else -> SingleTmType(paramCls)
        }
    }

    fun resolve(typeCanonicalText: String, context: PsiElement): TmType? {
        return tmTypeCanonicalTextCache.computeIfAbsent(typeCanonicalText, {
            doResolve(typeCanonicalText, context)
        })
    }

    private fun doResolve(typeCanonicalText: String, context: PsiElement): TmType? {

        when {
            typeCanonicalText == "?" -> {
                return SingleTmType(findClass(CommonClassNames.JAVA_LANG_OBJECT, context)!!)
            }
            typeCanonicalText.endsWith("[]") -> {
                val componentTypeCanonicalText = typeCanonicalText.removeSuffix(ARRAY_SUFFIX)
                return resolve(componentTypeCanonicalText, context)?.let { ArrayTmType(it) }
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
                        val typeParameterMap: HashMap<String, TmType?> = HashMap()
                        for ((index, typeParameter) in paramCls.typeParameters.withIndex()) {
                            if (typeParams.size > index) {
                                val typeParam = typeParams[index]
                                typeParameterMap[typeParameter.name!!] = resolve(typeParam, context)
                            }
                        }
                        return SingleTmType(paramCls, typeParameterMap)
                    }
                    return SingleTmType(paramCls)
                }
                logger!!.error("error to find class:$typeCanonicalText")
                return null
            }
            else -> {
                val paramCls = findClass(typeCanonicalText, context)
                return when (paramCls) {
                    null -> {
                        if (typeCanonicalText.length == 1) {//maybe generic
                            return SingleTmType(findClass(CommonClassNames.JAVA_LANG_OBJECT, context)!!)
                        }
                        logger!!.warn("error to resolve class:" + typeCanonicalText)
                        null
                    }
                    else -> {
                        SingleTmType(paramCls)
                    }
                }
            }
        }
    }

    private val classCache: HashMap<String, PsiClass?> = HashMap()

    fun findClass(fqClassName: String, context: PsiElement): PsiClass? {
        if (fqClassName.isEmpty()) return null

        if (classCache.contains(fqClassName)) {
            return classCache[fqClassName]
        }
        var cls: PsiClass? = null
        try {
            cls = ClassUtils.findClass(fqClassName, context)
        } catch (e: Exception) {
        }

        if (cls == null) {
            if (fqClassName.contains(".")) return null

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
        classCache[fqClassName] = cls
        return cls
    }

    fun extractClassFromCanonicalText(typeCanonicalText: String, context: PsiElement): PsiClass? {
        val classCanonicalText = StringUtils.substringBefore(typeCanonicalText, "<")
        return classCanonicalTextCache.computeIfAbsent(
            classCanonicalText
        ) { _ -> findClass(classCanonicalText, context) }
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

    //region isQualified--------------------------------------------------------
    private val qualifiedInfoCache: HashMap<PsiType, Boolean> = HashMap()

    fun isQualified(psiType: PsiType, context: PsiElement): Boolean {
        return qualifiedInfoCache.computeIfAbsent(psiType) {
            val tmType = resolve(psiType, context) ?: return@computeIfAbsent true
            return@computeIfAbsent isQualified(tmType)
        }
    }

    private fun isQualified(tmType: TmType): Boolean {
        if (tmType is SingleTmType) {
            if (tmType.psiCls.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT) {
                return false
            }
            val typeParameterCount = tmType.psiCls.typeParameters.size
            if (typeParameterCount == 0) return true
            if (typeParameterCount < tmType.typeParams?.size ?: 0) {
                return false
            }
            if (tmType.typeParams == null) return false

            for (value in tmType.typeParams.values) {
                if (value == null) return false
                if (!isQualified(value)) {
                    return false
                }
            }

            return true
        } else if (tmType is ArrayTmType) {
            return isQualified(tmType.componentType)
        }
        return true
    }
    //endregion isQualified--------------------------------------------------------


    companion object {
        val ARRAY_SUFFIX = "[]"
    }
}