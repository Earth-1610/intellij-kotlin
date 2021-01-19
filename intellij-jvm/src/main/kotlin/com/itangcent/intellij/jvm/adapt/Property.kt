package com.itangcent.intellij.jvm.adapt

import com.intellij.psi.*

/**
 * may be a field or a method.
 */
interface Property {

    fun pis(): PsiElement

    fun name(): String

    fun type(): PsiType

    companion object {
        fun of(member: PsiMember): Property? {
            return when (member) {
                is PsiField -> FieldProperty(member)
                is PsiMethod -> MethodProperty(member)
                else -> null
            }
        }
    }
}

class FieldProperty(private val field: PsiField) : Property {
    override fun pis(): PsiElement {
        return field
    }

    override fun name(): String {
        return field.name
    }

    override fun type(): PsiType {
        return field.type
    }
}

typealias MethodProperty = GetMethodProperty;

class GetMethodProperty(private val method: PsiMethod) : Property {
    override fun pis(): PsiElement {
        return method
    }

    override fun name(): String {
        return method.name.getterPropertyName()
    }

    override fun type(): PsiType {
        return method.returnType ?: PsiType.VOID
    }
}

class SetMethodProperty(private val method: PsiMethod) : Property {
    override fun pis(): PsiElement {
        return method
    }

    override fun name(): String {
        return method.name.getterPropertyName()
    }

    override fun type(): PsiType {
        val firstOrNull: PsiParameter? = method.parameterList.parameters.firstOrNull()
        return firstOrNull?.type ?: PsiType.VOID
    }
}

fun String.maybeMethodPropertyName(): Boolean {
    return when {
        this.startsWith("get") -> true
        this.startsWith("is") -> true
        this.startsWith("set") -> true
        else -> false
    }
}

fun String.maybeGetterMethodPropertyName(): Boolean {
    return when {
        this.startsWith("get") -> true
        this.startsWith("is") -> true
        else -> false
    }
}

fun String.maybeSetterMethodPropertyName(): Boolean {
    return when {
        this.startsWith("set") -> true
        else -> false
    }
}

fun String.propertyName(): String {
    return when {
        this.startsWith("get") -> this.removePrefix("get")
        this.startsWith("is") -> this.removePrefix("is")
        this.startsWith("set") -> this.removePrefix("set")
        else -> this
    }.decapitalize().removeSuffix("()")
}

fun String.getterPropertyName(): String {
    return when {
        this.startsWith("get") -> this.removePrefix("get")
        this.startsWith("is") -> this.removePrefix("is")
        else -> this
    }.decapitalize().removeSuffix("()")
}

fun String.setterPropertyName(): String {
    return when {
        this.startsWith("set") -> this.removePrefix("set")
        else -> this
    }.decapitalize().removeSuffix("()")
}
