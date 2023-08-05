package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfTypes
import com.itangcent.common.logger.Log
import com.itangcent.common.utils.capitalize
import com.itangcent.common.utils.mapToTypedArray
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.*
import com.itangcent.intellij.logger.Logger
import com.siyeh.ig.psiutils.ClassUtils
import kotlin.reflect.KClass


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

    companion object : Log()

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

    @Inject
    protected val standardEnumFieldResolver: StandardEnumFieldResolver? = null

    @Inject
    protected lateinit var actionContext: ActionContext

    private val nameToClassCache: java.util.HashMap<String, PsiClass?> = LinkedHashMap()

    private val nameToTypeCache: java.util.HashMap<String, PsiType?> = LinkedHashMap()

    override fun resolveClass(className: String, context: PsiElement): PsiClass? {
        return when {
            className.contains(".") -> findClass(className, context)
                ?: getContainingClass(context)?.let {
                    resolveClassFromImport(it, className)
                }

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
            LOG.info("find class:$fqClassName")
            cls = actionContext.callInReadUI { ClassUtils.findClass(fqClassName, context) }
        } catch (_: Exception) {
        }

        if (cls == null) {
            if (fqClassName.contains(".")) return null

            for (defaultPackage in defaultPackages()) {
                try {
                    LOG.info("find class:$defaultPackage${fqClassName.capitalize()}")
                    cls = actionContext.callInReadUI {
                        ClassUtils.findClass(
                            "$defaultPackage${fqClassName.capitalize()}",
                            context
                        )
                    }
                    if (cls != null) {
                        break
                    }
                } catch (_: Exception) {
                }
            }
        }
        nameToClassCache[fqClassName] = cls
        return cls
    }

    protected open fun defaultPackages(): Array<String> {
        return arrayOf("java.lang.", "java.util.")
    }

    override fun findType(canonicalText: String, context: PsiElement): PsiType? {
        return nameToTypeCache.safeComputeIfAbsent(canonicalText) {
            return@safeComputeIfAbsent duckTypeHelper!!.buildPsiType(canonicalText, context)
        }
    }

    protected open fun resolveClassFromImport(psiClass: PsiClass, clsName: String): PsiClass? {
        return actionContext.callInReadUI {
            resolveReferenceFromClass(psiClass, clsName) as? PsiClass
        }
    }

    protected open fun resolveReferenceFromClass(psiClass: PsiClass, clsName: String): Any? {
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

        resolveReferenceFromImport(psiClass, clsName)?.let { return it }

        psiClass.containingClass?.let { resolveReferenceFromClass(it, clsName) }?.let { return it }

        return null
    }

    protected open fun resolveReferenceFromImport(psiClass: PsiClass, clsName: String): Any? {

        //try to find imports
        val imports = PsiTreeUtil.findChildrenOfType(psiClass.context, PsiImportStatement::class.java)

        for (importStatement in imports) {
            //import xxx.xxx.Xxx
            importStatement.qualifiedName
                ?.takeIf { it.endsWith(".$clsName") || it == clsName }
                ?.let { findClass(it, psiClass) }
                ?.let { return it }

            //import xxx.xxx.*
            importStatement.text.removeSuffix(";")
                .takeIf { it.endsWith(".*") }
                ?.let { it.substringAfterLast(' ').removeSuffix("*") + clsName }
                ?.let { findClass(it, psiClass) }
                ?.let { return it }
        }

        //try to find import static
        val staticImports = PsiTreeUtil.findChildrenOfType(psiClass.context, PsiImportStaticStatement::class.java)

        for (importStatement in staticImports) {
            val qualifiedName = importStatement.text.removeSuffix(";").substringAfterLast(' ')
            //import xxx.xxx.Xxx
            if (qualifiedName == clsName || qualifiedName.endsWith(".$clsName")) {
                importStatement.resolve()?.let { return it }
                findClass(qualifiedName, psiClass)?.let { return it }
                findClass(qualifiedName.substringBeforeLast('.'), psiClass)?.let {
                    return resolvePropertyOrMethodOfClass(it, qualifiedName.substringAfterLast('.'))
                }
                continue
            }

            //import xxx.xxx.*
            importStatement.text.removeSuffix(";")
                .takeIf { it.endsWith(".*") }
                ?.let { it.substringAfterLast(' ').removeSuffix("*") + clsName }
                ?.let { findClass(it, psiClass) }
                ?.let { return it }
        }

        //try find defaultPackage
        val defaultPackage = psiClass.qualifiedName!!.substringBeforeLast(".")
        findClass("$defaultPackage.$clsName", psiClass)
            ?.let { return it }

        //maybe OutClass.InnerClass
        if (clsName.contains(".")) {
            val outClassName = clsName.substringBefore('.')
            val outClass = resolveClassFromImport(psiClass, outClassName) ?: return null
            return findClass(outClass.qualifiedName!! + "." + clsName.substringAfter('.'), psiClass)
        }

        return null
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
        if (linkMethodOrProperty.isNotBlank()) {
            return resolveClassOrType(linkClassName, context)
                ?.let { it to resolvePropertyOrMethodOfClass(it.asPsiClass(jvmClassHelper!!)!!, linkMethodOrProperty) }
        }

        resolveClassOrType(linkClassName, context)
            ?.let { return it to null }

        val resolveReference = getContainingClass(context)?.let { resolveReferenceFromClass(it, linkClassName) }
        if (resolveReference is PsiElement) {
            getContainingClass(resolveReference)?.let { return it to resolveReference }
        }
        return null
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

            val paramStr = propertyOrMethod.substringAfter("(")
                .removeSuffix(")")
            val params: List<String> = if (paramStr.isBlank()) {
                emptyList()
            } else {
                paramStr.split(",")
            }
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

            return candidates.firstOrNull()
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
        return psiElement.parentOfTypes(PsiClass::class)
    }

    override fun resolveRefText(psiElement: PsiElement?): String? {
        when (psiElement) {
            null -> return null
            is PsiLiteralExpression -> return psiElement.value?.toString()
            is PsiReferenceExpression -> {
                val value = psiElement.resolve()
                return resolveRefText(value)
            }

            is PsiField -> {
                val constantValue = psiElement.computeConstantValue()
                if (constantValue != null) {
                    if (constantValue is PsiExpression) {
                        return resolveRefText(constantValue)
                    }
                    return constantValue.toString()
                }
                return psiElement.text
            }

            else -> return psiElement.text
        }
    }

    override fun resolveEnumFields(index: Int, psiField: PsiField): Map<String, Any?>? {
        val value =
            (psiField as? PsiEnumConstant) ?: (psiField.computeConstantValue() as? PsiEnumConstant) ?: return null

        val params = standardEnumFieldResolver!!.resolveEnumFields(value) ?: emptyMap()
        val constant: HashMap<String, Any?> = LinkedHashMap<String, Any?>()
        constant["params"] = params
        constant["name"] = psiField.name
        constant["ordinal"] = index
        constant["desc"] = docHelper!!.getAttrOfField(psiField)?.trim()
        return constant
    }

    override fun visit(psiElement: Any, visitor: (Any) -> Unit) {
        if (psiElement is PsiElement) {
            psiElement.acceptChildren(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    visitor(element)
                    super.visitElement(element)
                }
            })
        }
    }

    override fun getChildren(psiElement: PsiElement): Array<PsiElement> {
        return psiElement.children
    }

    override fun getReturnType(psiMethod: PsiMethod): PsiType? {
        return psiMethod.returnType
    }

    override fun <T : PsiElement> getContextOfType(element: PsiElement, vararg classes: KClass<out T>): T? {
        return PsiTreeUtil.getContextOfType(element, *classes.mapToTypedArray { it.java })
    }
}