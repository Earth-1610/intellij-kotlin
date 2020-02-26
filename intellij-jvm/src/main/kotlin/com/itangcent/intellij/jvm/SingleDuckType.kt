package com.itangcent.intellij.jvm

import com.intellij.psi.PsiClass

class SingleDuckType : DuckType {

    fun psiClass(): PsiClass {
        return psiCls
    }

    private val psiCls: PsiClass

    val genericInfo: Map<String, DuckType?>?//generic type info

    constructor(psiCls: PsiClass) {
        this.psiCls = psiCls
        this.genericInfo = null
    }

    constructor(psiCls: PsiClass, genericInfo: Map<String, DuckType?>?) {
        this.psiCls = psiCls
        this.genericInfo = genericInfo
    }

    private var canonicalText: String? = null

    override fun canonicalText(): String {
        if (canonicalText != null) {
            return canonicalText!!
        }
        canonicalText = buildCanonicalText()
        return canonicalText!!
    }

    private fun buildCanonicalText(): String {
        return when {
            genericInfo.isNullOrEmpty() -> psiCls.qualifiedName ?: ""
            else -> {
                val sb = StringBuilder()
                sb.append(psiCls.qualifiedName)
                    .append("<")
                for ((index, typeParameter) in psiCls.typeParameters.withIndex()) {
                    if (index > 0) {
                        sb.append(",")
                    }
                    sb.append(genericInfo[typeParameter.name]?.canonicalText() ?: "java.lang.Object")
                }
                sb.append(">").toString()
            }
        }
    }

    override fun toString(): String {
        return if (genericInfo == null) {
            "$psiCls"
        } else {
            val sb = StringBuilder()
            genericInfo.forEach { t, u ->
                if (sb.isNotEmpty()) sb.append(",")
                sb.append(t).append("->").append(u)
            }
            "$psiCls with [$sb]"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SingleDuckType

        if (psiCls != other.psiCls) return false
        if (genericInfo != other.genericInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = psiCls.hashCode()
        result = 31 * result + (genericInfo?.hashCode() ?: 0)
        return result
    }


}