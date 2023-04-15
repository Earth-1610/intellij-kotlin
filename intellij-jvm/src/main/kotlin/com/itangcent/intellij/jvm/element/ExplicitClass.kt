package com.itangcent.intellij.jvm.element

import com.intellij.psi.*
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.DuckTypeHelper
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.SourceHelper
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType


interface ExplicitClass : DuckExplicitElement<PsiClass> {

    /**
     * Returns the list of classes that this class or interface extends.
     *
     * @return the extends list, or null for anonymous classes.
     */
    fun extends(): Array<ExplicitClass>?

    /**
     * Returns the list of interfaces that this class implements.
     *
     * @return the implements list, or null for anonymous classes
     */
    fun implements(): Array<ExplicitClass>?

    /**
     * Returns the list of methods in the class and all its superclasses.
     *
     * @return the list of methods.
     */
    fun methods(): ArrayList<ExplicitMethod>

    /**
     * collect all methods in the class and all its superclasses.
     */
    fun collectMethods(action: (ExplicitMethod) -> Unit)

    /**
     * Returns the list of fields in the class and all its superclasses.
     *
     * @return the list of fields.
     */
    fun fields(): ArrayList<ExplicitField>

    /**
     * collect all fields in the class and all its superclasses.
     */
    fun collectFields(action: (ExplicitField) -> Unit)

    fun asDuckType(): DuckType
}

val jvmClassHelper: JvmClassHelper = ActionContext.local()
val sourceHelper: SourceHelper = ActionContext.local()

private interface BaseExplicitClass : ExplicitClass {

    override fun name(): String {
        return psi().name ?: ""
    }

    override fun defineClass(): ExplicitClass {
        return this
    }

    fun PsiMethod.explicit(): ExplicitMethod

    fun PsiField.explicit(): ExplicitField

    fun ensureType(psiType: PsiType): DuckType?

    fun PsiClassType.explicit(): ExplicitClass? {
        val psiClass = ActionContext.getContext()
            ?.instance(JvmClassHelper::class)
            ?.resolveClassInType(this)
            ?.let { sourceHelper.getSourceClass(it) }
            ?: return null
        return when {
            hasParameters() -> {
                val subGenericInfo: HashMap<String, DuckType?> = HashMap()
                val parameters = parameters
                for ((index, typeParameter) in psiClass.typeParameters.withIndex()) {
                    subGenericInfo[typeParameter.name ?: ""] = ensureType(parameters[index])
                }
                ExplicitClassWithGenericInfo(this@BaseExplicitClass, subGenericInfo, psiClass)
            }

            else -> ExplicitClassWithOutGenericInfo(this@BaseExplicitClass, psiClass)
        }
    }

    /**
     * Returns the list of classes that this class or interface extends.
     *
     * @return the extends list, or null for anonymous classes.
     */
    override fun extends(): Array<ExplicitClass>? {
        val extendsList = psi().extendsList ?: return null
        val referencedTypes = extendsList.referencedTypes
        if (referencedTypes.isEmpty()) return emptyArray()
        return referencedTypes
            .filter { it.name != CommonClassNames.JAVA_LANG_OBJECT }
            .mapNotNull {
                it.explicit()
            }
            .toTypedArray()
    }

    /**
     * Returns the list of interfaces that this class implements.
     *
     * @return the implements list, or null for anonymous classes
     */
    override fun implements(): Array<ExplicitClass>? {
        val implementsList = psi().implementsList ?: return null
        val referencedTypes = implementsList.referencedTypes
        if (referencedTypes.isEmpty()) return emptyArray()
        return referencedTypes
            .mapNotNull {
                it.explicit()
            }
            .toTypedArray()
    }

    /**
     * Returns the list of methods in the class and all its superclasses.
     *
     * @return the list of methods.
     */
    override fun methods(): ArrayList<ExplicitMethod> {
        val explicitMethods: ArrayList<ExplicitMethod> = ArrayList()
        val methodSet = HashSet<PsiMethod>()
        collectMethods {
            if (methodSet.add(it.psi())) {
                explicitMethods.add(it)
            }
        }
        return explicitMethods
    }

    /**
     * collect all methods in the class and all its superclasses.
     */
    override fun collectMethods(
        action: (ExplicitMethod) -> Unit
    ) {
        val methods = jvmClassHelper.getMethods(psi()).mapTo(mutableListOf()) {
            it.explicit()
        }
        val superToSubMethods = methods.flatMap {
            it.psi().findSuperMethods().map { superMethod -> superMethod to it }
        }.associate { it }

        val preferMethod = { method: ExplicitMethod ->
            val subMethod = superToSubMethods[method.psi()]
            if (subMethod != null && methods.remove(subMethod)) {
                action(subMethod)
            }
            action(method)
        }
        this.extends()?.forEach { it.collectMethods(preferMethod) }
        this.implements()?.forEach { it.collectMethods(preferMethod) }
        methods.forEach(action)
    }

    override fun fields(): ArrayList<ExplicitField> {
        val explicitFields: ArrayList<ExplicitField> = ArrayList()
        val fieldName: HashSet<String> = HashSet()
        collectFields {
            if (fieldName.add(it.name())) {
                explicitFields.add(it)
            }
        }
        return explicitFields
    }

    override fun collectFields(action: (ExplicitField) -> Unit) {
        this.extends()?.forEach { it.collectFields(action) }
        jvmClassHelper.getFields(psi())
            .map { it.explicit() }
            .forEach(action)
    }
}

class ExplicitClassWithGenericInfo : ExplicitElementWithGenericInfo<PsiClass>, BaseExplicitClass {
    private val psiClass: PsiClass

    constructor(duckTypeHelper: DuckTypeHelper, genericInfo: Map<String, DuckType?>?, psiClass: PsiClass) : super(
        duckTypeHelper,
        genericInfo
    ) {
        this.psiClass = psiClass
    }

    constructor(parent: ExplicitElementWithGenericInfo<*>, psiClass: PsiClass) : super(parent) {
        this.psiClass = psiClass
    }

    constructor(
        parent: DuckExplicitElement<*>,
        genericInfo: Map<String, DuckType?>?,
        psiClass: PsiClass
    ) : super(parent, genericInfo) {
        this.psiClass = psiClass
    }

    override fun containClass(): ExplicitClass {
        return (parent as? ExplicitClass)?.containClass() ?: this
    }

    override fun PsiMethod.explicit(): ExplicitMethod =
        ExplicitMethodWithGenericInfo(this@ExplicitClassWithGenericInfo, this)

    override fun PsiField.explicit(): ExplicitField =
        ExplicitFieldWithGenericInfo(this@ExplicitClassWithGenericInfo, this)

    override fun asDuckType(): DuckType {
        return SingleDuckType(psiClass, genericInfo())
    }

    override fun psi(): PsiClass {
        return psiClass
    }

    override fun toString(): String {
        return psiClass.qualifiedName ?: ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExplicitClassWithGenericInfo

        if (psiClass != other.psiClass) return false

        return true
    }

    override fun hashCode(): Int {
        return psiClass.hashCode()
    }
}

class ExplicitClassWithOutGenericInfo : ExplicitElementWithOutGenericInfo<PsiClass>, BaseExplicitClass {
    private val psiClass: PsiClass

    constructor(duckTypeHelper: DuckTypeHelper, psiClass: PsiClass) : super(duckTypeHelper) {
        this.psiClass = psiClass
    }

    constructor(parent: DuckExplicitElement<*>, psiClass: PsiClass) : super(parent) {
        this.psiClass = psiClass
    }

    override fun containClass(): ExplicitClass {
        return (parent as? ExplicitClass)?.containClass() ?: this
    }

    override fun PsiMethod.explicit(): ExplicitMethod =
        ExplicitMethodWithOutGenericInfo(this@ExplicitClassWithOutGenericInfo, this)

    override fun PsiField.explicit(): ExplicitField =
        ExplicitFieldWithOutGenericInfo(this@ExplicitClassWithOutGenericInfo, this)

    override fun asDuckType(): DuckType {
        return SingleDuckType(psiClass)
    }

    override fun psi(): PsiClass {
        return psiClass
    }

    override fun toString(): String {
        return psiClass.qualifiedName ?: ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExplicitClassWithOutGenericInfo

        if (psiClass != other.psiClass) return false

        return true
    }

    override fun hashCode(): Int {
        return psiClass.hashCode()
    }
}