package com.itangcent.intellij.psi

import com.intellij.psi.PsiClass

class SingleDuckType : DuckType {
    val psiCls: PsiClass

    val genericInfo: Map<String, DuckType?>?//generic type info

    constructor(psiCls: PsiClass) {
        this.psiCls = psiCls
        this.genericInfo = null
    }

    constructor(psiCls: PsiClass, genericInfo: Map<String, DuckType?>?) {
        this.psiCls = psiCls
        this.genericInfo = genericInfo
    }

    override fun toString(): String {
        if (genericInfo == null) {
            return "$psiCls"
        } else {
            val sb = StringBuilder()
            genericInfo.forEach { t, u ->
                if (sb.isNotEmpty()) sb.append(",")
                sb.append(t).append("->").append(u)
            }
            return "$psiCls with [$sb]"
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