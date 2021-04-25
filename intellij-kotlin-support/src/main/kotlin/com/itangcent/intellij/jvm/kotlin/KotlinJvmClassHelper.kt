package com.itangcent.intellij.jvm.kotlin

import com.intellij.lang.jvm.JvmParameter
import com.intellij.psi.*
import com.itangcent.common.utils.getPropertyValue
import com.itangcent.intellij.jvm.ClassMateDataStorage
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.duck.DuckType

class KotlinJvmClassHelper(val jvmClassHelper: JvmClassHelper) : JvmClassHelper {

    override fun isInheritor(duckType: DuckType, vararg baseClass: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isAccessibleField(field: PsiField): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isStaticFinal(field: PsiField): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(duckType: DuckType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCollection(duckType: DuckType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCollection(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCollection(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isString(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isString(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isString(duckType: DuckType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isPublicStaticFinal(field: PsiField): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isNormalType(typeName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isPrimitive(typeName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun isPrimitiveWrapper(typeName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getDefaultValue(typeName: String): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isBasicMethod(methodName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEnum(duckType: DuckType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEnum(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEnum(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInterface(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInterface(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInterface(duckType: DuckType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resolveClassInType(psiType: PsiType): PsiClass? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resolveClassToType(psiClass: PsiClass): PsiType? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInheritor(psiClass: PsiClass, vararg baseClass: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInheritor(psiType: PsiType, vararg baseClass: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllFields(psiClass: PsiClass): Array<PsiField> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllMethods(psiClass: PsiClass): Array<PsiMethod> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMethods(psiClass: PsiClass): Array<PsiMethod> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getFields(psiClass: PsiClass): Array<PsiField> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun extractModifiers(psiElement: PsiElement): List<String> {
        return jvmClassHelper.extractModifiers(psiElement)
    }

    override fun defineCode(psiElement: PsiElement): String {
        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            throw NotImplementedError("not implemented")
        }
        return super.defineCode(psiElement)
    }

    override fun defineClassCode(psiClass: PsiClass): String {
        if (!KtPsiUtils.isKtPsiInst(psiClass)) {
            throw NotImplementedError("not implemented")
        }
        val sb = StringBuilder()
        //modifiers
        extractModifiers(psiClass).forEach {
            sb.append(it).append(" ")
        }
        when {
            psiClass.isInterface -> sb.append("interface ")
            psiClass.isEnum -> sb.append("enum class ")
            psiClass.isAnnotationType -> sb.append("annotation class ")
            else -> sb.append("class ")
        }
        var colon = false
        sb.append(psiClass.name)
        psiClass.extendsListTypes
            .takeIf { !it.isNullOrEmpty() }
            ?.let {
                if (!colon) {
                    sb.append(" : ")
                    colon = true
                }
                sb.append(it.joinToString(separator = " ,") { type -> type.canonicalText })
            }
        psiClass.implementsListTypes
            .takeIf { !it.isNullOrEmpty() }
            ?.let {
                if (!colon) {
                    sb.append(" : ")
                    colon = true
                } else {
                    sb.append(" ")
                }
                sb.append(it.joinToString(separator = " ,") { type -> type.canonicalText })
            }
        return sb.append(";").toString()
    }

    override fun defineMethodCode(psiMethod: PsiMethod): String {
        if (!KtPsiUtils.isKtPsiInst(psiMethod)) {
            throw NotImplementedError("not implemented")
        }
        val sb = StringBuilder()
        //modifiers
        extractModifiers(psiMethod).forEach {
            sb.append(it).append(" ")
        }
        if (psiMethod.isConstructor) {
            sb.append("constructor")
                .append("(")
        } else {
            sb.append("fun ")
                .append(psiMethod.name)
                .append("(")
        }
        for ((index, parameter) in psiMethod.parameters.withIndex()) {
            if (index > 0) {
                sb.append(", ")
            }
            if (parameter is PsiParameter) {
                sb.append(defineParamCode(parameter as PsiParameter))
            } else {
                sb.append(defineParamCode(parameter))
            }
        }
        sb.append(")")
        if (!psiMethod.isConstructor) {
            psiMethod.returnType?.let {
                sb.append(": ")
                    .append(it.canonicalText).append(" ")
            }
        }
        return sb.append(";").toString()
    }

    override fun defineFieldCode(psiField: PsiField): String {
        if (!KtPsiUtils.isKtPsiInst(psiField)) {
            throw NotImplementedError("not implemented")
        }
        val sb = StringBuilder()
        //modifiers
        extractModifiers(psiField).forEach {
            sb.append(it).append(" ")
        }
        if (psiField is PsiEnumConstant) {
            sb.append(psiField.name)
            psiField.argumentList?.expressions
                ?.takeIf { !it.isNullOrEmpty() }
                ?.joinToString(separator = ", ") { it.text }
                ?.let {
                    sb.append("(")
                        .append(it)
                        .append(")")
                }
        } else {
            sb.append(psiField.name)
                .append(": ")
                .append(psiField.type.canonicalText)
        }
        return sb.append(";").toString()
    }

    override fun defineParamCode(psiParameter: PsiParameter): String {
        if (!KtPsiUtils.isKtPsiInst(psiParameter)) {
            throw NotImplementedError("not implemented")
        }
        val sb = StringBuilder()
        sb.append(psiParameter.type.canonicalText)
            .append(" ")
            .append(psiParameter.name)
        return sb.toString()
    }

    private fun defineParamCode(psiParameter: JvmParameter): String {
        val sb = StringBuilder()
        sb.append(psiParameter.name)
            .append(": ")
            .append(psiParameter.type.getPropertyValue("text"))
        return sb.toString()
    }

    override fun defineOtherCode(psiElement: PsiElement): String {
        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            throw NotImplementedError("not implemented")
        }
        return psiElement.text
    }

    companion object {

        init {
            ClassMateDataStorage.addTag(kotlin.Byte::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(kotlin.Byte::class, 0.toByte())

            ClassMateDataStorage.addTag(kotlin.Int::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(kotlin.Int::class, 0)

            ClassMateDataStorage.addTag(kotlin.Short::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(kotlin.Short::class, 0.toShort())

            ClassMateDataStorage.addTag(kotlin.Float::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(kotlin.Float::class, 0.0f)

            ClassMateDataStorage.addTag(kotlin.Double::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(kotlin.Double::class, 0.0)

            ClassMateDataStorage.addTag(kotlin.collections.Collection::class, "collection")
            ClassMateDataStorage.addTag(kotlin.collections.MutableCollection::class, "collection")
            ClassMateDataStorage.addTag(kotlin.collections.Set::class, "collection")
            ClassMateDataStorage.addTag(kotlin.collections.MutableSet::class, "collection")
            ClassMateDataStorage.addTag(kotlin.collections.List::class, "collection")
            ClassMateDataStorage.addTag(kotlin.collections.MutableList::class, "collection")

            ClassMateDataStorage.addTag(kotlin.collections.Map::class, "map")
            ClassMateDataStorage.addTag(kotlin.collections.MutableMap::class, "map")

            ClassMateDataStorage.addTag(kotlin.String::class, "string")
        }
    }
}