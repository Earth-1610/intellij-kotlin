package com.itangcent.intellij.jvm.kotlin

import com.google.inject.Singleton
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper
import java.util.*
import kotlin.reflect.KClass

@Singleton
class KotlinJvmClassHelper : StandardJvmClassHelper() {

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

            val collectionClasses = StandardJvmClassHelper.collectionClasses as HashSet<String>

            addClass(kotlin.collections.Collection::class, collectionClasses)
            addClass(MutableCollection::class, collectionClasses)
            addClass(kotlin.collections.Set::class, collectionClasses)
            addClass(MutableSet::class, collectionClasses)
            addClass(kotlin.collections.List::class, collectionClasses)
            addClass(MutableList::class, collectionClasses)

            val mapClasses = StandardJvmClassHelper.mapClasses as HashSet<String>

            addClass(kotlin.collections.Map::class, mapClasses)
            addClass(MutableMap::class, mapClasses)
        }

        private fun addClass(cls: KClass<*>, classSet: HashSet<String>) {
            classSet.add(cls.qualifiedName!!)
            classSet.add(cls.simpleName!!)
        }
    }
}