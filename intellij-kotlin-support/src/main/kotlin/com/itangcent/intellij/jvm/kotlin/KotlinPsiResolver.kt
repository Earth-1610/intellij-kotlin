package com.itangcent.intellij.jvm.kotlin

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.itangcent.common.utils.cast
import com.itangcent.common.utils.firstOrNull
import com.itangcent.intellij.jvm.asPsiClass
import com.itangcent.intellij.jvm.standard.StandardPsiResolver
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassImpl
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import kotlin.reflect.KClass


/**
 *
 * see https://kotlinlang.org/docs/reference/kotlin-doc.html
 * Linking to Elements
 * To link to another element (class, method, property or parameter), simply put its name in square brackets:
 *
 * Use the method [foo] for this purpose.
 * If you want to specify a custom label for the link, use the Markdown reference-style syntax:
 *
 * Use [this method][foo] for this purpose.
 * You can also use qualified names in the links. Note that, unlike JavaDoc, qualified names always use the dot character to separate the components, even before a method name:
 *
 * Use [kotlin.reflect.KClass.properties] to enumerate the properties of the class.
 * Names in links are resolved using the same rules as if the name was used inside the element being documented. In particular, this means that if you have imported a name into the current file, you don't need to fully qualify it when you use it in a KDoc comment.
 *
 * Note that KDoc does not have any syntax for resolving overloaded members in links. Since the Kotlin documentation generation tool puts the documentation for all overloads of a function on the same page, identifying a specific overloaded function is not required for the link to work.
 */
open class KotlinPsiResolver : StandardPsiResolver() {
    /**
     * ref: {@link https://kotlinlang.org/docs/reference/packages.html#default-imports}
     * Default Imports
     * A number of packages are imported into every Kotlin file by default:
     * - kotlin.*
     * - kotlin.annotation.*
     * - kotlin.collections.*
     * - kotlin.comparisons.* (since 1.1)
     * - kotlin.io.*
     * - kotlin.ranges.*
     * - kotlin.sequences.*
     * - kotlin.text.*
     * Additional packages are imported depending on the target platform:
     * JVM:
     * - java.lang.*
     * - kotlin.jvm.*
     * JS:
     * - kotlin.js.*
     */
    override fun defaultPackages(): Array<String> {
        return arrayOf(
            "kotlin.",
            "kotlin.annotation.",
            "kotlin.collections.",
            "kotlin.comparisons.",
            "kotlin.io.",
            "kotlin.ranges.",
            "kotlin.sequences.",
            "kotlin.text.",
            "java.lang.",
            "kotlin.jvm.",
            "kotlin.js."
        )
    }

    override fun resolveClassWithPropertyOrMethod(
        classNameWithProperty: String,
        context: PsiElement
    ): Pair<Any?, PsiElement?>? {
        if (!KtPsiUtils.isKtPsiInst(context)) {
            throw NotImplementedError()
        }

        val cwp: String = if (classNameWithProperty.startsWith('[')) {
            classNameWithProperty.trim('[', ']')
        } else {
            classNameWithProperty
        }

        //[kotlin.reflect.KClass]
        var linkClass = resolveClass(cwp, context)
        if (linkClass != null) {
            return linkClass to null
        }

        val separator = cwp.lastIndexOfAny(charArrayOf('#', '.'))
        //[kotlin.reflect.KClass.properties]
        if (separator != -1) {
            val linkClassName = cwp.substring(0, separator)
            val linkMethodOrProperty = cwp.substring(separator + 1).trim()
            val cls: Any = (resolveClassOrType(linkClassName, context)
                ?: resolveClass(linkClassName, context)) ?: return null
            return cls to resolvePropertyOrMethodOfClass(
                cls.asPsiClass(com.itangcent.intellij.jvm.element.jvmClassHelper)!!,
                linkMethodOrProperty
            )
        }

        //[properties]
        linkClass = getContainingClass(context) ?: return null
        resolvePropertyOrMethodOfClass(linkClass, cwp)?.let {
            return linkClass to it
        }
        return null
    }

    override fun getContainingClass(psiElement: PsiElement): PsiClass? {
        val ktElement = psiElement.originalElement as? KtElement
        return ktElement?.containingClass()
            ?.let { KtLightClassImpl(it, JvmDefaultMode.ENABLE, false) }
            ?: super.getContainingClass(psiElement)
    }

    override fun resolveClassFromImport(psiClass: PsiClass, clsName: String): PsiClass? {

        val imports: Collection<String>?
        imports = if (psiClass is KtLightClass) {
            psiClass.kotlinOrigin?.containingKtFile?.importList?.imports?.mapNotNull {
                it.importPath?.fqName?.toString()
            }
        } else {
            PsiTreeUtil.findChildrenOfType(psiClass.context, PsiImportStatement::class.java)
                .mapNotNull { it.qualifiedName }
        } ?: return null

        var cls = imports
            .firstOrNull { it.endsWith(".$clsName") }
            ?.let { findClass(it, psiClass) }
        if (cls != null) {
            return cls
        }

        val defaultPackage = psiClass.qualifiedName!!.substringBeforeLast(".")
        cls = findClass("$defaultPackage.$clsName", psiClass)
        if (cls != null) {
            return cls
        }
        cls = imports
            .asSequence()
            .filter { it.endsWith(".*") }
            .map { it.removeSuffix("*") + clsName }
            .map { findClass(it, psiClass) }
            .firstOrNull()

        return cls
    }

    override fun visit(psiElement: Any, visitor: (Any) -> Unit) {
        if (psiElement is KtLightElement<*, *>) {
            psiElement.kotlinOrigin?.let {
                visit(it, visitor)
                return
            }
        }
        if (psiElement is KtElement) {
            psiElement.acceptChildren(object : KtVisitor<Void, Any>() {
                override fun visitElement(element: PsiElement) {
                    visitor(element)
                    visit(element, visitor)
                    super.visitElement(element)
                }
            }, 1)

            psiElement.acceptChildren(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    visitor(element)
                    super.visitElement(element)
                }
            })

            return
        }
        super.visit(psiElement, visitor)
    }

    override fun getChildren(psiElement: PsiElement): Array<PsiElement> {
        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            throw NotImplementedError()
        }
        val descendant = HashSet<PsiElement>()
        val children = LinkedHashSet<PsiElement>()
        visit(psiElement) { element ->
            element.cast(PsiElement::class)
                ?.also { descendant.add(it) }
                ?.takeIf { !descendant.contains(it.parent) }
                ?.let { children.add(it) }
        }
        return children.toTypedArray()
    }

    override fun getReturnType(psiMethod: PsiMethod): PsiType? {
        if (!KtPsiUtils.isKtPsiInst(psiMethod)) {
            throw NotImplementedError()
        }
        if (!psiMethod.modifierList.text.contains("suspend") || psiMethod.returnType?.canonicalText != "java.lang.Object") {
            return super.getReturnType(psiMethod)
        }
        return getReturnTypeOfSuspendFun(psiMethod) ?: super.getReturnType(psiMethod)
    }

    private fun getReturnTypeOfSuspendFun(psiMethod: PsiMethod): PsiType? {
        var typeReference: KtTypeReference? = null
        val children = getChildren(psiMethod)
        for (element in children) {
            typeReference = if (element is KtTypeReference) {
                element
            } else if (element is KtBlockExpression) {
                break
            } else {
                null
            }
        }
        typeReference ?: return null
        typeReference.text?.let { findType(it, psiMethod) }?.let { return it }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : PsiElement> getContextOfType(element: PsiElement, vararg classes: KClass<out T>): T? {
        super.getContextOfType(element, *classes)?.let {
            return it
        }
        classes as Array<KClass<*>>
        if (PsiClass::class in classes) {
            val ktClass = PsiTreeUtil.getContextOfType(element, KtClass::class.java)
            if (ktClass != null) {
                return KtPsiUtils.ktClassToPsiClass(ktClass) as T
            }
        }
        return null
    }
}