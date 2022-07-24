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
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.ClassMateDataStorage
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.duck.SingleUnresolvedDuckType

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
        return ActionContext.getContext()?.callInReadUI {

            if (baseClass.contains(psiClass.qualifiedName)) {
                return@callInReadUI true
            }

            for (superCls in psiClass.supers) {
                if (baseClass.contains(superCls.qualifiedName)) {
                    return@callInReadUI true
                }
                for (inter in superCls.interfaces) {
                    if (baseClass.contains(inter.qualifiedName)) {
                        return@callInReadUI true
                    }
                }
            }

            for (inter in psiClass.interfaces) {
                if (baseClass.contains(inter.qualifiedName)) {
                    return@callInReadUI true
                }
            }

            return@callInReadUI false
        } ?: false
    }

    override fun isCollection(psiType: PsiType): Boolean {
        return isInheritor(psiType, *ClassMateDataStorage.classWithTag("collection"))
    }

    override fun isCollection(psiClass: PsiClass): Boolean {
        return isInheritor(psiClass, *ClassMateDataStorage.classWithTag("collection"))
    }

    override fun isCollection(duckType: DuckType): Boolean {
        return isInheritor(duckType, *ClassMateDataStorage.classWithTag("collection"))
    }

    override fun isMap(psiClass: PsiClass): Boolean {
        return isInheritor(psiClass, *ClassMateDataStorage.classWithTag("map"))
    }

    override fun isMap(psiType: PsiType): Boolean {
        return isInheritor(psiType, *ClassMateDataStorage.classWithTag("map"))
    }

    override fun isMap(duckType: DuckType): Boolean {
        return isInheritor(duckType, *ClassMateDataStorage.classWithTag("map"))
    }

    override fun isString(psiClass: PsiClass): Boolean {
        return psiClass.qualifiedName?.let { ClassMateDataStorage.hasTag(it, "string") } ?: false
    }

    override fun isString(psiType: PsiType): Boolean {
        return ClassMateDataStorage.hasTag(psiType.canonicalText, "string")
    }

    override fun isString(duckType: DuckType): Boolean {
        return ClassMateDataStorage.hasTag(duckType.canonicalText(), "string")
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
        return hasAllModify(field, staticFinalFieldModifiers)
    }

    override fun isPublicStaticFinal(field: PsiField): Boolean {
        return hasAllModify(field, publicStaticFinalFieldModifiers)
    }

    override fun isAccessibleField(field: PsiField): Boolean {
        return hasModify(field, PsiModifier.PUBLIC) || (field.containingClass?.isInterface ?: false)
    }

    override fun isNormalType(typeName: String): Boolean {
        return ClassMateDataStorage.hasTag(typeName, "normal")
    }

    override fun isPrimitive(typeName: String): Boolean {
        return ClassMateDataStorage.hasTag(typeName, "primitive")
    }

    override fun isPrimitiveWrapper(typeName: String): Boolean {
        return ClassMateDataStorage.hasTag(typeName, "wrapper")
    }

    override fun isAccepted(oneClass: String, anotherClass: String): Boolean {
        if (oneClass == anotherClass) {
            return true
        }
        return ClassMateDataStorage.isAccepted(oneClass, anotherClass)
    }

    override fun getDefaultValue(typeName: String): Any? {
        return ClassMateDataStorage.getDefaultValue(typeName)
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
        extractModifiers(psiClass)
            .filter { it != "abstract" || !psiClass.isInterface }
            .forEach {
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
                if (sb.last() != ' ') {
                    sb.append(' ')
                }
                sb.append("extends ")
                    .append(it.joinToString(separator = ", ") { type -> type.canonicalText })
            }
        psiClass.implementsListTypes
            .takeIf { !it.isNullOrEmpty() }
            ?.let {
                if (sb.last() != ' ') {
                    sb.append(' ')
                }
                sb.append("implements ")
                    .append(it.joinToString(separator = ", ") { type -> type.canonicalText })
            }
        return sb.append(";").toString()
    }

    override fun defineMethodCode(psiMethod: PsiMethod): String {
        val sb = StringBuilder()
        //modifiers
        val notInterface = psiMethod.containingClass?.isInterface != true
        extractModifiers(psiMethod)
            .filter { it != "abstract" || notInterface }
            .forEach {
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
        val notInterface = psiField.containingClass?.isInterface != true
        extractModifiers(psiField)
            .filter { it != "static" || notInterface }
            .forEach {
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

    @Suppress("UnstableApiUsage")
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

        var staticFinalFieldModifiers: Set<String> =
            setOf(PsiModifier.STATIC, PsiModifier.FINAL)
        var publicStaticFinalFieldModifiers: Set<String> =
            setOf(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)


        private val PRIMITIVE_TYPES = HashMap<String, PsiType>(9)

        fun init() {

            ClassMateDataStorage.addTag("boolean", "normal", "primitive")
            ClassMateDataStorage.setDefaultValue("boolean", false)

            ClassMateDataStorage.addTag(java.lang.Boolean::class.java, "normal", "wrapper")
            ClassMateDataStorage.setDefaultValue(java.lang.Boolean::class.java, false)

            ClassMateDataStorage.addTag(java.lang.Void::class.java, "normal", "primitive")
            ClassMateDataStorage.addTag("void", "normal", "primitive")
            ClassMateDataStorage.addAcceptedType(java.lang.Void::class.java, "void")

            ClassMateDataStorage.addTag("char", "normal", "primitive")
            ClassMateDataStorage.setDefaultValue("char", 'a')

            ClassMateDataStorage.addTag(java.lang.Character::class.java, "normal", "wrapper")
            ClassMateDataStorage.setDefaultValue(java.lang.Character::class.java, 'a')
            ClassMateDataStorage.addAcceptedType(java.lang.Character::class.java, "char")

            ClassMateDataStorage.addTag("null", "normal", "primitive")

            ClassMateDataStorage.addTag("byte", "normal", "primitive")
            ClassMateDataStorage.setDefaultValue("byte", 0.toByte())

            ClassMateDataStorage.addTag(java.lang.Byte::class.java, "normal", "wrapper")
            ClassMateDataStorage.setDefaultValue(java.lang.Byte::class.java, 0.toByte())
            ClassMateDataStorage.addAcceptedType(java.lang.Byte::class.java, "byte")


            ClassMateDataStorage.addTag("short", "normal", "primitive")
            ClassMateDataStorage.setDefaultValue("short", 0.toShort())

            ClassMateDataStorage.addTag(java.lang.Short::class.java, "normal", "wrapper")
            ClassMateDataStorage.setDefaultValue(java.lang.Short::class.java, 0.toShort())
            ClassMateDataStorage.addAcceptedType(java.lang.Short::class.java, "short")

            ClassMateDataStorage.addTag("int", "normal", "primitive")
            ClassMateDataStorage.setDefaultValue("int", 0)

            ClassMateDataStorage.addTag(java.lang.Integer::class.java, "normal", "wrapper")
            ClassMateDataStorage.setDefaultValue(java.lang.Integer::class.java, 0)
            ClassMateDataStorage.addAcceptedType(java.lang.Integer::class.java, "int")

            ClassMateDataStorage.addTag("long", "normal", "primitive")
            ClassMateDataStorage.setDefaultValue("long", 0L)

            ClassMateDataStorage.addTag(java.lang.Long::class.java, "normal", "wrapper")
            ClassMateDataStorage.setDefaultValue(java.lang.Long::class.java, 0L)
            ClassMateDataStorage.addAcceptedType(java.lang.Long::class.java, "long")

            ClassMateDataStorage.addTag("float", "normal", "primitive")
            ClassMateDataStorage.setDefaultValue("float", 0.0F)

            ClassMateDataStorage.addTag(java.lang.Float::class.java, "normal", "wrapper")
            ClassMateDataStorage.setDefaultValue(java.lang.Float::class.java, 0.0F)
            ClassMateDataStorage.addAcceptedType(java.lang.Float::class.java, "float")

            ClassMateDataStorage.addTag("double", "normal", "primitive")
            ClassMateDataStorage.setDefaultValue("double", 0.0)

            ClassMateDataStorage.addTag(java.lang.Double::class.java, "normal", "wrapper")
            ClassMateDataStorage.setDefaultValue(java.lang.Double::class.java, 0.0)
            ClassMateDataStorage.addAcceptedType(java.lang.Double::class.java, "double")

            ClassMateDataStorage.addTag(java.lang.Object::class.java, "normal")
            ClassMateDataStorage.setDefaultValue(java.lang.Object::class.java, emptyMap<Any, Any>())

            ClassMateDataStorage.addTag(java.lang.String::class.java, "normal", "string")
            ClassMateDataStorage.setDefaultValue(java.lang.String::class.java, "")

            ClassMateDataStorage.addTag(java.math.BigDecimal::class.java, "normal")
            ClassMateDataStorage.setDefaultValue(java.math.BigDecimal::class.java, 0.0)

            ClassMateDataStorage.addTag("class", "normal")
            ClassMateDataStorage.addTag(java.lang.Class::class.java, "normal")

            ClassMateDataStorage.addTag(java.lang.Iterable::class.java, "collection")
            ClassMateDataStorage.addTag(java.util.Collection::class.java, "collection")
            ClassMateDataStorage.addTag(java.util.List::class.java, "collection", "list")
            ClassMateDataStorage.addTag(java.util.ArrayList::class.java, "collection", "list")
            ClassMateDataStorage.addTag(java.util.LinkedList::class.java, "collection", "list")

            ClassMateDataStorage.addTag(java.util.Set::class.java, "collection", "set")
            ClassMateDataStorage.addTag(java.util.HashSet::class.java, "collection", "set")
            ClassMateDataStorage.addTag(java.util.TreeSet::class.java, "collection", "set")
            ClassMateDataStorage.addTag(java.util.SortedSet::class.java, "collection", "set")

            ClassMateDataStorage.addTag(java.util.Queue::class.java, "collection", "queue")
            ClassMateDataStorage.addTag(java.util.Deque::class.java, "collection", "queue")
            ClassMateDataStorage.addTag("com.sun.jmx.remote.internal.ArrayQueue", "collection", "queue")
            ClassMateDataStorage.addTag(java.util.concurrent.BlockingQueue::class.java, "collection", "queue")
            ClassMateDataStorage.addTag(java.util.concurrent.ArrayBlockingQueue::class.java, "collection", "queue")
            ClassMateDataStorage.addTag(java.util.Stack::class.java, "collection", "stack")

            ClassMateDataStorage.addTag(java.util.Map::class.java, "map")
            ClassMateDataStorage.addTag(java.util.HashMap::class.java, "map")
            ClassMateDataStorage.addTag(java.util.LinkedHashMap::class.java, "map")

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

        fun hasModify(modifierListOwner: PsiModifierListOwner, modify: String): Boolean {
            val modifierList = modifierListOwner.modifierList ?: return false
            return modifierList.hasModifierProperty(modify)
        }

        fun hasAnyModify(modifierListOwner: PsiModifierListOwner, modifies: Set<String>): Boolean {
            val modifierList = modifierListOwner.modifierList ?: return false
            return modifies.any { modifierList.hasModifierProperty(it) }
        }


        fun hasAllModify(modifierListOwner: PsiModifierListOwner, modifies: Set<String>): Boolean {
            val modifierList = modifierListOwner.modifierList ?: return false
            return modifies.all { modifierList.hasModifierProperty(it) }
        }

        fun addClass(cls: Class<*>, classSet: HashSet<String>) {
            classSet.add(cls.name)
            classSet.add(cls.simpleName)
        }
    }
}