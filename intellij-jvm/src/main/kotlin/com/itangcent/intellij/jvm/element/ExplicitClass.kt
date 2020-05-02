package com.itangcent.intellij.jvm.element

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.DuckTypeHelper
import com.itangcent.intellij.jvm.JvmClassHelper
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
     * Returns the list of methods in the class and all its superclasses.
     *
     * @return the list of methods.
     */
    fun methods(): ArrayList<ExplicitMethod>

    /**
     * collect all methods in the class and all its superclasses.
     */
    fun collectMethods(jvmClassHelper: JvmClassHelper, action: (ExplicitMethod) -> Unit)

    /**
     * Returns the list of fields in the class and all its superclasses.
     *
     * @return the list of fields.
     */
    fun fields(): ArrayList<ExplicitField>

    /**
     * collect all fields in the class and all its superclasses.
     */
    fun collectFields(jvmClassHelper: JvmClassHelper, action: (ExplicitField) -> Unit)

    fun name(): String

    fun asDuckType(): DuckType
}

class ExplicitClassWithGenericInfo : ExplicitElementWithGenericInfo<PsiClass>, ExplicitClass {
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

    override fun name(): String {
        return psiClass.name ?: ""
    }

    override fun containClass(): ExplicitClass {
        return this
    }

    /**
     * Returns the list of classes that this class or interface extends.
     *
     * @return the extends list, or null for anonymous classes.
     */
    override fun extends(): Array<ExplicitClass>? {
        val extendsList = psiClass.extendsList ?: return null
        val referencedTypes = extendsList.referencedTypes
        if (referencedTypes.isEmpty()) return emptyArray()
        return referencedTypes
            .filter { it.name != CommonClassNames.JAVA_LANG_OBJECT }
            .mapNotNull {
                resolve(it)
            }.toTypedArray()
    }

    /**
     * Returns the list of methods in the class and all its superclasses.
     *
     * @return the list of methods.
     */
    override fun methods(): ArrayList<ExplicitMethod> {
        val jvmClassHelper = ActionContext.getContext()!!.instance(JvmClassHelper::class)
        val explicitMethods: ArrayList<ExplicitMethod> = ArrayList()
        collectMethods(jvmClassHelper) { explicitMethods.add(it) }
        return explicitMethods
    }

    /**
     * collect all methods in the class and all its superclasses.
     */
    override fun collectMethods(jvmClassHelper: JvmClassHelper, action: (ExplicitMethod) -> Unit) {
        val methods = jvmClassHelper.getMethods(psiClass)
        for (method in methods) {
            action(ExplicitMethodWithGenericInfo(this, method))
        }
        this.extends()?.forEach { it.collectMethods(jvmClassHelper, action) }
    }

    override fun fields(): ArrayList<ExplicitField> {
        val jvmClassHelper = ActionContext.getContext()!!.instance(JvmClassHelper::class)
        val explicitFields: ArrayList<ExplicitField> = ArrayList()
        val fieldName: HashSet<String> = HashSet()
        collectFields(jvmClassHelper) {
            if (fieldName.add(it.name())) {
                explicitFields.add(it)
            }
        }
        return explicitFields
    }

    override fun collectFields(jvmClassHelper: JvmClassHelper, action: (ExplicitField) -> Unit) {
        val fields = jvmClassHelper.getFields(psiClass)
        for (field in fields) {
            action(ExplicitFieldWithGenericInfo(this, field))
        }
        this.extends()?.forEach { it.collectFields(jvmClassHelper, action) }
    }

    private fun resolve(psiClassType: PsiClassType): ExplicitClass? {
        if (psiClassType.hasParameters()) {
            val subGenericInfo: HashMap<String, DuckType?> = HashMap()
            val psiClass = ActionContext.getContext()
                ?.instance(JvmClassHelper::class)
                ?.resolveClassInType(psiClassType) ?: return null
            val parameters = psiClassType.parameters
            for ((index, typeParameter) in psiClass.typeParameters.withIndex()) {
                subGenericInfo[typeParameter.name ?: ""] = ensureType(parameters[index])
            }
            return ExplicitClassWithGenericInfo(duckTypeHelper, subGenericInfo, psiClass)
        } else {
            return ExplicitClassWithOutGenericInfo(this, psiClass)
        }
    }

    override fun asDuckType(): DuckType {
        return SingleDuckType(psiClass, genericInfo())
    }

    override fun psi(): PsiClass {
        return psiClass
    }
}

class ExplicitClassWithOutGenericInfo : ExplicitElementWithOutGenericInfo<PsiClass>, ExplicitClass {
    private val psiClass: PsiClass

    constructor(duckTypeHelper: DuckTypeHelper, psiClass: PsiClass) : super(duckTypeHelper) {
        this.psiClass = psiClass
    }

    constructor(parent: DuckExplicitElement<*>, psiClass: PsiClass) : super(parent) {
        this.psiClass = psiClass
    }

    override fun name(): String {
        return psiClass.name ?: ""
    }

    override fun containClass(): ExplicitClass {
        return this
    }

    /**
     * Returns the list of classes that this class or interface extends.
     *
     * @return the extends list, or null for anonymous classes.
     */
    override fun extends(): Array<ExplicitClass>? {
        val extendsList = psiClass.extendsList ?: return null
        val referencedTypes = extendsList.referencedTypes
        if (referencedTypes.isEmpty()) return emptyArray()
        return referencedTypes
            .filter { it.name != CommonClassNames.JAVA_LANG_OBJECT }
            .mapNotNull {
                resolve(it)
            }
            .toTypedArray()
    }

    /**
     * Returns the list of methods in the class and all its superclasses.
     *
     * @return the list of methods.
     */
    override fun methods(): ArrayList<ExplicitMethod> {
        val jvmClassHelper = ActionContext.getContext()!!.instance(JvmClassHelper::class)
        val explicitMethods: ArrayList<ExplicitMethod> = ArrayList()
        collectMethods(jvmClassHelper) { explicitMethods.add(it) }
        return explicitMethods
    }

    /**
     * collect all methods in the class and all its superclasses.
     */
    override fun collectMethods(jvmClassHelper: JvmClassHelper, action: (ExplicitMethod) -> Unit) {
        val methods = jvmClassHelper.getMethods(psiClass)
        for (method in methods) {
            action(ExplicitMethodWithOutGenericInfo(this, method))
        }
        this.extends()?.forEach { it.collectMethods(jvmClassHelper, action) }
    }

    override fun fields(): ArrayList<ExplicitField> {
        val jvmClassHelper = ActionContext.getContext()!!.instance(JvmClassHelper::class)
        val explicitFields: ArrayList<ExplicitField> = ArrayList()
        val fieldName: HashSet<String> = HashSet()
        collectFields(jvmClassHelper) {
            if (fieldName.add(it.name())) {
                explicitFields.add(it)
            }
        }
        return explicitFields
    }

    override fun collectFields(jvmClassHelper: JvmClassHelper, action: (ExplicitField) -> Unit) {
        val fields = jvmClassHelper.getFields(psiClass)
        for (field in fields) {
            action(ExplicitFieldWithOutGenericInfo(this, field))
        }
        this.extends()?.forEach { it.collectFields(jvmClassHelper, action) }
    }

    private fun resolve(psiClassType: PsiClassType): ExplicitClass? {
        val psiClass = ActionContext.getContext()
            ?.instance(JvmClassHelper::class)
            ?.resolveClassInType(psiClassType) ?: return null
        return when {
            psiClassType.hasParameters() -> {
                val subGenericInfo: HashMap<String, DuckType?> = HashMap()
                val parameters = psiClassType.parameters
                for ((index, typeParameter) in psiClass.typeParameters.withIndex()) {
                    subGenericInfo[typeParameter.name ?: ""] = ensureType(parameters[index])
                }
                ExplicitClassWithGenericInfo(this.duckTypeHelper(), subGenericInfo, psiClass)
            }
            else -> ExplicitClassWithOutGenericInfo(this, psiClass)
        }
    }

    override fun asDuckType(): DuckType {
        return SingleDuckType(psiClass)
    }

    override fun psi(): PsiClass {
        return psiClass
    }
}