package com.itangcent.intellij.jvm.standard

import com.google.inject.Singleton
import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.JvmModifiersOwner
import com.intellij.lang.jvm.JvmParameter
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.itangcent.common.utils.getPropertyValue
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.duck.SingleUnresolvedDuckType
import com.sun.jmx.remote.internal.ArrayQueue
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

@Singleton
open class StandardJvmClassHelper : JvmClassHelper {

    private val typeCache: java.util.HashMap<PsiType, PsiClass?> = java.util.LinkedHashMap()

    private val classCache: java.util.HashMap<PsiClass?, PsiType> = java.util.LinkedHashMap()

    override fun isInheritor(duckType: DuckType, vararg baseClass: String): Boolean {

        if (baseClass.contains(duckType.canonicalText().substringBefore('<'))) {
            return true
        }

        if (!duckType.isSingle()) {
            return false
        }

        if (duckType is SingleDuckType) {
            return isInheritor(duckType.psiClass(), *baseClass)
        }

        if (duckType is SingleUnresolvedDuckType) {
            return isInheritor(duckType.psiType(), *baseClass)
        }

        return false
    }

    override fun isInheritor(psiType: PsiType, vararg baseClass: String): Boolean {

        if (baseClass.contains(psiType.canonicalText.substringBefore('<'))) {
            return true
        }

        val cls = resolveClassInType(psiType)
        if (cls != null) {
            return isInheritor(cls, *baseClass)
        }

        return false
    }

    override fun isInheritor(psiClass: PsiClass, vararg baseClass: String): Boolean {

        if (baseClass.contains(psiClass.qualifiedName)) {
            return true
        }

        for (superCls in psiClass.supers) {
            if (baseClass.contains(superCls.qualifiedName)) {
                return true
            }
        }

        return false
    }

    override fun isCollection(psiType: PsiType): Boolean {
        return isInheritor(psiType, *collectionClasses!!)
    }

    override fun isCollection(psiClass: PsiClass): Boolean {
        return isInheritor(psiClass, *collectionClasses!!)
    }

    override fun isCollection(duckType: DuckType): Boolean {
        return isInheritor(duckType, *collectionClasses!!)
    }

    override fun isMap(psiClass: PsiClass): Boolean {
        return isInheritor(psiClass, *mapClasses!!)
    }

    override fun isMap(psiType: PsiType): Boolean {
        return isInheritor(psiType, *mapClasses!!)
    }

    override fun isMap(duckType: DuckType): Boolean {
        return isInheritor(duckType, *mapClasses!!)
    }

    /**
     * Checks if the class is an enumeration.
     *
     * @return true if the class is an enumeration, false otherwise.
     */
    override fun isEnum(psiType: PsiType): Boolean {

        val cls = resolveClassInType(psiType)

        return cls?.isEnum ?: false
    }

    /**
     * Checks if the class is an enumeration.
     *
     * @return true if the class is an enumeration, false otherwise.
     */
    override fun isEnum(psiClass: PsiClass): Boolean {
        return psiClass.isEnum
    }

    /**
     * Checks if the class is an enumeration.
     *
     * @return true if the class is an enumeration, false otherwise.
     */
    override fun isEnum(duckType: DuckType): Boolean {
        if (!duckType.isSingle()) {
            return false
        }

        if (duckType is SingleDuckType) {
            return isEnum(duckType.psiClass())
        }

        if (duckType is SingleUnresolvedDuckType) {
            return isEnum(duckType.psiType())
        }

        return false
    }

    /**
     * Checks if the class is an interface.
     *
     * @return true if the class is an interface, false otherwise.
     */
    override fun isInterface(psiType: PsiType): Boolean {
        val cls = resolveClassInType(psiType)

        return cls?.isInterface ?: false
    }

    /**
     * Checks if the class is an interface.
     *
     * @return true if the class is an interface, false otherwise.
     */
    override fun isInterface(psiClass: PsiClass): Boolean {
        return psiClass.isInterface
    }

    /**
     * Checks if the class is an interface.
     *
     * @return true if the class is an interface, false otherwise.
     */
    override fun isInterface(duckType: DuckType): Boolean {
        if (!duckType.isSingle()) {
            return false
        }

        if (duckType is SingleDuckType) {
            return isInterface(duckType.psiClass())
        }

        if (duckType is SingleUnresolvedDuckType) {
            return isInterface(duckType.psiType())
        }

        return false
    }

    override fun isStaticFinal(field: PsiField): Boolean {
        return hasAnyModify(field, staticFinalFieldModifiers)
    }

    override fun isPublicStaticFinal(field: PsiField): Boolean {
        return hasAnyModify(field, publicStaticFinalFieldModifiers)
    }

    override fun isAccessibleField(field: PsiField): Boolean {
        return hasAnyModify(field, fieldModifiers)
    }

    override fun isNormalType(typeName: String): Boolean {
        return normalTypes.containsKey(typeName)
    }

    override fun getDefaultValue(typeName: String): Any? {
        return normalTypes[typeName]
    }

    override fun isBasicMethod(methodName: String): Boolean {
        return JAVA_OBJECT_METHODS.contains(methodName)
    }

    override fun resolveClassInType(psiType: PsiType): PsiClass? {
        if (psiType is PsiArrayType) {
            return null
        }
        return typeCache.safeComputeIfAbsent(psiType) {
            PsiUtil.resolveClassInType(psiType)
        }
    }

    override fun resolveClassToType(psiClass: PsiClass): PsiType? {
        return classCache.safeComputeIfAbsent(psiClass) {
            PsiTypesUtil.getClassType(psiClass)
        }
    }

    /**
     * Returns the list of fields in the class and all its superclasses.
     *
     * @return the list of fields.
     */
    override fun getAllFields(psiClass: PsiClass): Array<PsiField> {
        return psiClass.allFields
    }

    /**
     * Returns the list of methods in the class and all its superclasses.
     *
     * @return the list of methods.
     */
    override fun getAllMethods(psiClass: PsiClass): Array<PsiMethod> {
        return psiClass.allMethods
    }

    /**
     * Returns the list of methods in the class.
     *
     * @return the list of methods.
     */
    override fun getMethods(psiClass: PsiClass): Array<PsiMethod> {
        return psiClass.methods
    }

    /**
     * Returns the list of fields in the class.
     *
     * @return the list of fields.
     */
    override fun getFields(psiClass: PsiClass): Array<PsiField> {
        return psiClass.fields
    }

    override fun extractModifiers(psiElement: PsiElement): List<String> {
        if (psiElement is PsiModifierListOwner) {
            val modifierList = psiElement.modifierList ?: return emptyList()
            return PsiModifier.MODIFIERS
                .filter { modifierList.hasModifierProperty(it) }
                .map { it.toLowerCase() }
                .toList()
        }
        if (psiElement is JvmModifiersOwner) {
            return JvmModifier.values()
                .filter { psiElement.hasModifier(it) }
                .map { it.name.toLowerCase() }
                .toList()
        }
        return emptyList()
    }

    override fun defineClassCode(psiClass: PsiClass): String {
        val sb = StringBuilder()
        //modifiers
        extractModifiers(psiClass).forEach {
            sb.append(it).append(" ")
        }
        when {
            psiClass.isInterface -> sb.append("interface ")
            psiClass.isEnum -> sb.append("enum ")
            psiClass.isAnnotationType -> sb.append("@interface ")
            else -> sb.append("class ")
        }
        sb.append(psiClass.name)
        psiClass.extendsListTypes
            .takeIf { !it.isNullOrEmpty() }
            ?.let {
                sb.append(" extends ")
                    .append(it.joinToString(separator = " ,") { type -> type.canonicalText })
                    .append(" ")
            }
        psiClass.implementsListTypes
            .takeIf { !it.isNullOrEmpty() }
            ?.let {
                sb.append(" implements ")
                    .append(it.joinToString(separator = " ,") { type -> type.canonicalText })
                    .append(" ")
            }
        return sb.append(";").toString()
    }

    override fun defineMethodCode(psiMethod: PsiMethod): String {
        val sb = StringBuilder()
        //modifiers
        extractModifiers(psiMethod).forEach {
            sb.append(it).append(" ")
        }
        if (!psiMethod.isConstructor) {
            psiMethod.returnType?.let {
                sb.append(it.canonicalText).append(" ")
            }
        }
        sb.append(psiMethod.name)
            .append("(")
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
        return sb.append(")").append(";").toString()
    }

    override fun defineFieldCode(psiField: PsiField): String {
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
            sb.append(psiField.type.canonicalText)
                .append(" ")
                .append(psiField.name)
        }
        return sb.append(";").toString()
    }

    override fun defineParamCode(psiParameter: PsiParameter): String {
        val sb = StringBuilder()
        sb.append(psiParameter.type.canonicalText)
            .append(" ")
            .append(psiParameter.name)
        return sb.toString()
    }

    private fun defineParamCode(psiParameter: JvmParameter): String {
        val sb = StringBuilder()
        sb.append(psiParameter.type.getPropertyValue("text"))
            .append(" ")
            .append(psiParameter.name)
        return sb.toString()
    }

    override fun defineOtherCode(psiElement: PsiElement): String {
        return psiElement.text
    }

    companion object {

        val JAVA_OBJECT_METHODS: Array<String> = arrayOf(
            "registerNatives",
            "getClass",
            "hashCode",
            "equals",
            "clone",
            "toString",
            "notify",
            "notifyAll",
            "wait",
            "finalize"
        )

        const val ELEMENT_OF_COLLECTION = "E"
        const val KEY_OF_MAP = "K"
        const val VALUE_OF_MAP = "V"

        var fieldModifiers: Set<String> = HashSet(Arrays.asList(PsiModifier.PRIVATE, PsiModifier.PROTECTED))
        var staticFinalFieldModifiers: Set<String> =
            HashSet(Arrays.asList(PsiModifier.STATIC, PsiModifier.FINAL))
        var publicStaticFinalFieldModifiers: Set<String> = HashSet(
            Arrays.asList(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)
        )

        val normalTypes: HashMap<String, Any?> = HashMap()

        var collectionClasses: Array<String>? = null
        var mapClasses: Array<String>? = null

        private val PRIMITIVE_TYPES = HashMap<String, PsiType>(9)

        fun init() {
            if (normalTypes.isEmpty()) {
                normalTypes["boolean"] = false
                normalTypes["Boolean"] = false
                normalTypes["Void"] = null
                normalTypes["void"] = null
                normalTypes["char"] = 'a'
                normalTypes["Character"] = 'a'
                normalTypes["null"] = null
                normalTypes["Byte"] = 0
                normalTypes["byte"] = 0
                normalTypes["Short"] = 0
                normalTypes["short"] = 0
                normalTypes["Integer"] = 0
                normalTypes["int"] = 0
                normalTypes["Long"] = 0L
                normalTypes["long"] = 0L
                normalTypes["Float"] = 0.0F
                normalTypes["float"] = 0.0F
                normalTypes["Double"] = 0.0
                normalTypes["double"] = 0.0
                normalTypes["String"] = ""
                normalTypes["BigDecimal"] = 0.0
                normalTypes["class"] = null
                normalTypes["Class"] = null
                normalTypes["java.lang.Boolean"] = false
                normalTypes["java.lang.Object"] = emptyMap<Any, Any>()
                normalTypes["java.lang.Character"] = 'a'
                normalTypes["java.lang.Void"] = null
                normalTypes["java.lang.Byte"] = 0
                normalTypes["java.lang.Short"] = 0
                normalTypes["java.lang.Integer"] = 0
                normalTypes["java.lang.Long"] = 0L
                normalTypes["java.lang.Float"] = 0.0F
                normalTypes["java.lang.Double"] = 0.0
                normalTypes["java.lang.String"] = ""
                normalTypes["java.math.BigDecimal"] = 0.0
                normalTypes["java.lang.Class"] = null
            }
            if (collectionClasses == null) {
                val collectionClasses = HashSet<String>()
                addClass(Collection::class.java, collectionClasses)
                addClass(List::class.java, collectionClasses)
                addClass(ArrayList::class.java, collectionClasses)
                addClass(LinkedList::class.java, collectionClasses)
                addClass(Set::class.java, collectionClasses)
                addClass(HashSet::class.java, collectionClasses)
                addClass(TreeSet::class.java, collectionClasses)
                addClass(SortedSet::class.java, collectionClasses)
                addClass(Queue::class.java, collectionClasses)
                addClass(Deque::class.java, collectionClasses)
                addClass(ArrayQueue::class.java, collectionClasses)
                addClass(ArrayBlockingQueue::class.java, collectionClasses)
                addClass(Stack::class.java, collectionClasses)
                this.collectionClasses = collectionClasses.toTypedArray()
            }
            if (mapClasses == null) {
                val mapClasses = HashSet<String>()
                addClass(Map::class.java, mapClasses)
                addClass(HashMap::class.java, mapClasses)
                addClass(LinkedHashMap::class.java, mapClasses)
                this.mapClasses = mapClasses.toTypedArray()
            }

            PRIMITIVE_TYPES[PsiType.VOID.canonicalText] = PsiType.VOID;
            PRIMITIVE_TYPES[PsiType.BYTE.canonicalText] = PsiType.BYTE;
            PRIMITIVE_TYPES[PsiType.CHAR.canonicalText] = PsiType.CHAR;
            PRIMITIVE_TYPES[PsiType.DOUBLE.canonicalText] = PsiType.DOUBLE;
            PRIMITIVE_TYPES[PsiType.FLOAT.canonicalText] = PsiType.FLOAT;
            PRIMITIVE_TYPES[PsiType.LONG.canonicalText] = PsiType.LONG;
            PRIMITIVE_TYPES[PsiType.INT.canonicalText] = PsiType.INT;
            PRIMITIVE_TYPES[PsiType.SHORT.canonicalText] = PsiType.SHORT;
            PRIMITIVE_TYPES[PsiType.BOOLEAN.canonicalText] = PsiType.BOOLEAN;
        }

        fun isPrimitive(typeName: String): Boolean {
            return PRIMITIVE_TYPES.contains(typeName)
        }

        fun getPrimitiveType(typeName: String): PsiType? {
            return PRIMITIVE_TYPES[typeName]
        }

        init {
            init()
        }

        fun hasAnyModify(modifierListOwner: PsiModifierListOwner, modifies: Set<String>): Boolean {
            val modifierList = modifierListOwner.modifierList ?: return false
            return modifies.any { modifierList.hasModifierProperty(it) }
        }

        fun addClass(cls: Class<*>, classSet: HashSet<String>) {
            classSet.add(cls.name!!)
            classSet.add(cls.simpleName!!)
        }
    }
}