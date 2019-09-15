package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.itangcent.intellij.jvm.DuckTypeHelper
import com.itangcent.intellij.jvm.PsiResolver


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
    private val duckTypeHelper: DuckTypeHelper? = null

    override fun resolveClass(className: String, psiMember: PsiMember): PsiClass? {
        return when {
            className.contains(".") -> duckTypeHelper!!.findClass(className, psiMember)
            else -> getContainingClass(psiMember)?.let { resolveClassFromImport(it, className) }
                ?: duckTypeHelper!!.findClass(className, psiMember)
        }
    }

    protected open fun resolveClassFromImport(psiClass: PsiClass, clsName: String): PsiClass? {

        val imports = PsiTreeUtil.findChildrenOfType(psiClass.context, PsiImportStatement::class.java)

        var cls = imports
            .mapNotNull { it.qualifiedName }
            .firstOrNull { it.endsWith(".$clsName") }
            ?.let { duckTypeHelper!!.findClass(it, psiClass) }
        if (cls != null) {
            return cls
        }

        val defaultPackage = psiClass.qualifiedName!!.substringBeforeLast(".")
        cls = duckTypeHelper!!.findClass("$defaultPackage.$clsName", psiClass)
        if (cls != null) {
            return cls
        }
        cls = imports
            .mapNotNull { it.qualifiedName }
            .filter { it.endsWith(".*") }
            .map { it -> it.removeSuffix("*") + clsName }
            .map { duckTypeHelper.findClass(it, psiClass) }
            .firstOrNull()

        return cls
    }

    override fun resolveClassWithPropertyOrMethod(
        classNameWithProperty: String,
        psiMember: PsiMember
    ): Pair<PsiClass?, PsiElement?>? {

        //{@link #method(args...)}
        //{@link #property}
        if (classNameWithProperty.startsWith("#")) {
            val linkClass = getContainingClass(psiMember) ?: return null
            resolvePropertyOrMethodOfClass(linkClass, classNameWithProperty.removePrefix("#"))?.let {
                return linkClass to it
            }
            return null
        }

        //{@link java.class#method(args...)}
        //{@link java.class#property}
        val linkClassName = classNameWithProperty.substringBefore("#")
        val linkMethodOrProperty = classNameWithProperty.substringAfter("#", "").trim()
        val linkClass = resolveClass(linkClassName, psiMember) ?: return null
        return if (linkMethodOrProperty.isBlank()) {
            linkClass to null
        } else {
            linkClass to resolvePropertyOrMethodOfClass(linkClass, linkMethodOrProperty)
        }
    }

    override fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement? {

        if (propertyOrMethod.endsWith(")")) {
            if (!propertyOrMethod.contains("(")) {
                return null
            }

            val methodName = propertyOrMethod.substringBefore("(")
            var candidates = psiClass.allMethods.filter { it.name == methodName }.toList()
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
            return psiClass.allFields.firstOrNull { it.name == propertyOrMethod }
                ?: psiClass.allMethods.firstOrNull { it.name == propertyOrMethod }
        }
    }

    override fun getContainingClass(psiMember: PsiMember): PsiClass? {
        if (psiMember is PsiClass) return psiMember
        psiMember.containingClass?.let { return it }
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

}