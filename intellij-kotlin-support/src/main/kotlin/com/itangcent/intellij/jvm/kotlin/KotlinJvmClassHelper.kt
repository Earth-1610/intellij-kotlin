package com.itangcent.intellij.jvm.kotlin

import com.google.common.collect.Sets
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper.Companion.normalTypes
import java.util.*
import kotlin.reflect.KClass

class KotlinJvmClassHelper : JvmClassHelper {
    override fun isAccessibleField(field: PsiField): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isStaticFinal(field: PsiField): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(psiType: PsiType): Boolean {
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

    override fun isEnum(psiType: PsiType): Boolean {
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