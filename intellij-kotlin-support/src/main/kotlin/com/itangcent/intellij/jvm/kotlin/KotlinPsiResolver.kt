package com.itangcent.intellij.jvm.kotlin

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.util.PsiTreeUtil
import com.itangcent.common.utils.firstOrNull
import com.itangcent.intellij.jvm.standard.StandardPsiResolver
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassImpl
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClass


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

    override fun resolveClassWithPropertyOrMethod(
        classNameWithProperty: String,
        context: PsiElement
    ): Pair<PsiClass?, PsiElement?>? {
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

        //[kotlin.reflect.KClass.properties]
        if (cwp.contains('.')) {
            val linkClassName = cwp.substringBeforeLast(".")
            val linkMethodOrProperty = cwp.substringAfterLast(".", "").trim()
            linkClass = resolveClass(linkClassName, context) ?: return null
            return linkClass to resolvePropertyOrMethodOfClass(linkClass, linkMethodOrProperty)
        }

        //[properties]
        linkClass = getContainingClass(context) ?: return null
        resolvePropertyOrMethodOfClass(linkClass, cwp)?.let {
            return linkClass to it
        }
        return null
    }

    override fun getContainingClass(psiElement: PsiElement): PsiClass? {
        return (psiElement.originalElement as? KtElement)?.containingClass()?.let { KtLightClassImpl(it) }
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
            .stream()
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
                override fun visitElement(element: PsiElement?) {
                    element?.let {
                        visitor(it)
                        visit(it, visitor)
                    }
                    super.visitElement(element)
                }
            }, 1)

            psiElement.acceptChildren(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement?) {
                    if (element != null) {
                        visitor(element)
                    }
                    super.visitElement(element)
                }
            })

            return
        }
        super.visit(psiElement, visitor)
    }
}