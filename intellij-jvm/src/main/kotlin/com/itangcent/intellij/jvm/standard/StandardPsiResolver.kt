package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.itangcent.common.utils.firstOrNull
import com.itangcent.common.utils.mapNotNull
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.jvm.*
import com.itangcent.intellij.logger.Logger
import com.siyeh.ig.psiutils.ClassUtils


/**
 * {@link package.class#member label}
 * Introduced in JDK 1.2
 *
 * Inserts an inline link with a visible text label that points to the documentation for the specified package, class, or member name of a referenced class. This tag is valid in all documentation comments: overview, package, class, interface, constructor, method and field, including the text portion of any tag, such as the @return, @param and @deprecated tags. See @link in How to Write Doc Comments for the Javadoc Tool at
 * http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html#{@link
 *
 * This tag is similar to the @see tag. Both tags require the same references and accept the same syntax for package.class#member and label. The main difference is that the {@link} tag generates an inline link rather than placing the link in the See Also section. The {@link} tag begins and ends with braces to separate it from the rest of the inline text. If you need to use the right brace (}) inside the label, then use the HTML entity notation &#125;.
 *
 * There is no limit to the number of {@link} tags allowed in a sentence. You can use this tag in the main description part of any documentation comment or in the text portion of any tag, such as the @deprecated, @return or @param tags.
 *
 * For example, here is a comment that refers to the getComponentAt(int, int) method:
 *
 * Use the {@link #getComponentAt(int, int) getComponentAt} method.
 * From this code, the standard doclet generates the following HTML (assuming it refers to another class in the same package):
 *
 * Use the <a href="Component.html#getComponentAt(int, int)">getComponentAt</a> method.
 * The previous line appears on the web page as:
 *
 * Use the getComponentAt method.
 */
@Singleton
open class StandardPsiResolver : PsiResolver {

    @Inject
    protected val duckTypeHelper: DuckTypeHelper? = null

    @Inject
    private val sourceHelper: SourceHelper? = null

    @Inject
    private val jvmClassHelper: JvmClassHelper? = null

    @Inject
    protected val docHelper: DocHelper? = null

    @Inject
    protected val logger: Logger? = null

    private val nameToClassCache: java.util.HashMap<String, PsiClass?> = LinkedHashMap()

    private val nameToTypeCache: java.util.HashMap<String, PsiType?> = LinkedHashMap()

    override fun resolveClass(className: String, context: PsiElement): PsiClass? {
        return when {
            className.contains(".") -> findClass(className, context)
            else -> getContainingClass(context)?.let {
                resolveClassFromImport(it, className)
            } ?: findClass(className, context)
        }?.let { sourceHelper?.getSourceClass(it) }
    }

    override fun resolveClassOrType(className: String, context: PsiElement): Any? {
        return when {
            className.contains("<") -> findType(className, context)
            else -> resolveClass(className, context)
        }
    }

    override fun findClass(fqClassName: String, context: PsiElement): PsiClass? {
        if (fqClassName.isEmpty()) return null

        if (nameToClassCache.contains(fqClassName)) {
            return nameToClassCache[fqClassName]
        }

        if (fqClassName.contains("<")) {
            return findClass(fqClassName.substringBefore('<'), context)
        }

        var cls: PsiClass? = null
        try {
            cls = ClassUtils.findClass(fqClassName, context)
        } catch (e: Exception) {
        }

        if (cls == null) {
            if (fqClassName.contains(".")) return null

            try {
                cls = ClassUtils.findClass("java.lang." + fqClassName.capitalize(), context)
            } catch (e: Exception) {
            }
            if (cls == null) {
                try {
                    cls = ClassUtils.findClass("java.util." + fqClassName.capitalize(), context)
                } catch (e: Exception) {
                }
            }
        }
        nameToClassCache[fqClassName] = cls
        return cls
    }

    override fun findType(canonicalText: String, context: PsiElement): PsiType? {
        return nameToTypeCache.safeComputeIfAbsent(canonicalText) {
            return@safeComputeIfAbsent duckTypeHelper!!.buildPsiType(canonicalText, context)
        }
    }

    protected open fun resolveClassFromImport(psiClass: PsiClass, clsName: String): PsiClass? {

        //maybe it is this class
        if (clsName == psiClass.name || clsName == psiClass.qualifiedName) {
            return psiClass
        }

        //maybe it is an inner class
        for (innerClass in psiClass.allInnerClasses) {
            if (clsName == innerClass.name || clsName == innerClass.qualifiedName) {
                return innerClass
            }
        }

        //try find imports
        val imports = PsiTreeUtil.findChildrenOfType(psiClass.context, PsiImportStatement::class.java)

        var cls = imports
            .mapNotNull { it.qualifiedName }
            .firstOrNull { it.endsWith(".$clsName") }
            ?.let { findClass(it, psiClass) }
        if (cls != null) {
            return cls
        }

        //try find defaultPackage
        val defaultPackage = psiClass.qualifiedName!!.substringBeforeLast(".")
        cls = findClass("$defaultPackage.$clsName", psiClass)
        if (cls != null) {
            return cls
        }

        //try find in import xxx.*
        cls = imports
            .stream()
            .mapNotNull { it.qualifiedName }
            .filter { it.endsWith(".*") }
            .map { it -> it.removeSuffix("*") + clsName }
            .map { findClass(it, psiClass) }
            .firstOrNull()

        return cls
    }

    override fun resolveClassWithPropertyOrMethod(
        classNameWithProperty: String,
        context: PsiElement
    ): Pair<Any?, PsiElement?>? {

        //{@link #method(args...)}
        //{@link #property}
        if (classNameWithProperty.startsWith("#")) {
            val linkClass = getContainingClass(context) ?: return null
            resolvePropertyOrMethodOfClass(linkClass, classNameWithProperty.removePrefix("#"))?.let {
                return linkClass to it
            }
            return null
        }

        //{@link java.class#method(args...)}
        //{@link java.class#property}
        val linkClassName = classNameWithProperty.substringBefore("#")
        val linkMethodOrProperty = classNameWithProperty.substringAfter("#", "").trim()
        return if (linkMethodOrProperty.isBlank()) {
            resolveClassOrType(linkClassName, context)
                ?.let { it to null }
        } else {
            resolveClassOrType(linkClassName, context)
                ?.let { it to resolvePropertyOrMethodOfClass(it.asPsiClass(jvmClassHelper!!)!!, linkMethodOrProperty) }
        }
    }

    override fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement? {

        if (propertyOrMethod.endsWith(")")) {
            if (!propertyOrMethod.contains("(")) {
                return null
            }

            val methodName = propertyOrMethod.substringBefore("(")
            var candidates = jvmClassHelper!!.getAllMethods(psiClass).filter { it.name == methodName }.toList()
            when {
                candidates.isEmpty() -> return null
                candidates.size == 1 -> return candidates[0]
            }

            val params = propertyOrMethod.substringAfter("(")
                .removeSuffix(")")
                .split(",")
            candidates = candidates.filter { it.parameters.size == params.size }

            when {
                candidates.isEmpty() -> return null
                candidates.size == 1 -> return candidates[0]
            }


            candidates = candidates.filter { method ->
                return@filter method.parameterList.parameters
                    .filterIndexed { index, parameter -> !parameter.type.canonicalText.contains(params[index]) }
                    .none()
            }

            when {
                candidates.isEmpty() -> return null
                candidates.size == 1 -> return candidates[0]
            }

            return null
        } else {
            return jvmClassHelper!!.getAllFields(psiClass).firstOrNull { it.name == propertyOrMethod }
                ?: jvmClassHelper.getAllMethods(psiClass).firstOrNull { it.name == propertyOrMethod }
        }
    }

    override fun getContainingClass(psiElement: PsiElement): PsiClass? {
        if (psiElement is PsiClass) return psiElement
        if (psiElement is PsiMember) {
            psiElement.containingClass?.let { return it }
        }
        return null
    }

    override fun resolveRefText(psiExpression: PsiElement?): String? {
        when (psiExpression) {
            null -> return null
            is PsiLiteralExpression -> return psiExpression.value?.toString()
            is PsiReferenceExpression -> {
                val value = psiExpression.resolve()
                return resolveRefText(value)
            }
            is PsiField -> {
                val constantValue = psiExpression.computeConstantValue()
                if (constantValue != null) {
                    if (constantValue is PsiExpression) {
                        return resolveRefText(constantValue)
                    }
                    return constantValue.toString()
                }
                return psiExpression.text
            }
            else -> return psiExpression.text
        }
    }

    override fun resolveEnumFields(index: Int, psiField: PsiField): Map<String, Any?>? {
        val value =
            (psiField as? PsiEnumConstant) ?: (psiField.computeConstantValue() as? PsiEnumConstant) ?: return null

        val constant: HashMap<String, Any?> = LinkedHashMap<String, Any?>()
        val params = HashMap<String, Any?>()
        val construct = value.resolveConstructor()
        val expressions = value.argumentList?.expressions
        val parameters = construct?.parameterList?.parameters
        if (expressions != null && parameters != null && parameters.isNotEmpty()) {
            if (parameters.last().isVarArgs) {
                for (i in 0 until parameters.size - 1) {
                    params[parameters[i].name!!] = PsiUtils.resolveExpr(expressions[i])
                }
                try {
                    //resolve varArgs
                    val lastVarArgParam: ArrayList<Any?> = ArrayList(1)
                    params[parameters[parameters.size - 1].name!!] = lastVarArgParam
                    for (i in parameters.size - 1..expressions.size) {
                        lastVarArgParam.add(PsiUtils.resolveExpr(expressions[i]))
                    }
                } catch (e: Throwable) {
                }
            } else {
                for ((i, parameter) in parameters.withIndex()) {
                    try {
                        params[parameter.name!!] = PsiUtils.resolveExpr(expressions[i])
                    } catch (e: Throwable) {
                    }
                }
            }
        }
        constant["params"] = params
        constant["name"] = psiField.name
        constant["ordinal"] = index
        constant["desc"] = docHelper!!.getAttrOfField(psiField)?.trim()
        return constant
    }

    override fun visit(psiElement: Any, visitor: (Any) -> Unit) {
        if (psiElement is PsiElement) {
            psiElement.acceptChildren(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement?) {
                    if (element != null) {
                        visitor(element)
                    }
                    super.visitElement(element)
                }
            })
        }
    }
}