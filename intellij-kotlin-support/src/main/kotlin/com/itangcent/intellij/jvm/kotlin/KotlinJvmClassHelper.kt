package com.itangcent.intellij.jvm.kotlin

import com.google.common.collect.Sets
import com.intellij.lang.jvm.JvmParameter
import com.intellij.psi.*
import com.itangcent.common.utils.getPropertyValue
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper.Companion.normalTypes
import java.util.*
import kotlin.reflect.KClass

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

    override fun isPublicStaticFinal(field: PsiField): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isNormalType(typeName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

            normalTypes["Int"] = 0
            normalTypes["kotlin.Int"] = 0

            normalTypes["Byte"] = 0
            normalTypes["kotlin.Byte"] = 0

            normalTypes["Short"] = 0
            normalTypes["kotlin.Short"] = 0

            normalTypes["Long"] = 0
            normalTypes["kotlin.Long"] = 0

            normalTypes["Float"] = 0.0
            normalTypes["kotlin.Float"] = 0.0

            normalTypes["Double"] = 0.0
            normalTypes["kotlin.Double"] = 0.0

            normalTypes["kotlin.String"] = ""

            val collectionClasses = Sets.newHashSet(*StandardJvmClassHelper.collectionClasses!!)
            addClass(kotlin.collections.Collection::class, collectionClasses)
            addClass(MutableCollection::class, collectionClasses)
            addClass(kotlin.collections.Set::class, collectionClasses)
            addClass(MutableSet::class, collectionClasses)
            addClass(kotlin.collections.List::class, collectionClasses)
            addClass(MutableList::class, collectionClasses)
            StandardJvmClassHelper.collectionClasses = collectionClasses.toTypedArray()

            val mapClasses = Sets.newHashSet(*StandardJvmClassHelper.mapClasses!!)
            addClass(kotlin.collections.Map::class, mapClasses)
            addClass(MutableMap::class, mapClasses)
            StandardJvmClassHelper.mapClasses = mapClasses.toTypedArray()

        }

        private fun addClass(cls: KClass<*>, classSet: HashSet<String>) {
            classSet.add(cls.qualifiedName!!)
            classSet.add(cls.simpleName!!)
        }
    }
}