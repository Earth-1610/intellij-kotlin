package com.itangcent.intellij.jvm.kotlin

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiElement
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.standard.StandardDocHelper
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.*

@Singleton
class KotlinDocHelper : StandardDocHelper() {

    @Inject
    private val actionContext: ActionContext? = null

    override fun hasTag(psiElement: PsiElement?, tag: String?): Boolean {

        if (psiElement == null || tag == null) {
            return false
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {

            val kTag = KDocKnownTag.findByTagName(tag)
            if (kTag != null) {
                if (actionContext!!.callInReadUI {
                        kDoc.findSectionByTag(kTag) != null
                    } == true) {
                    return true
                }
            }

            return actionContext!!.callInReadUI {
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .any()
            } ?: false
        }

        return super.hasTag(psiElement, tag)
    }

    override fun findDocByTag(psiElement: PsiElement?, tag: String?): String? {

        if (psiElement == null || tag == null) {
            return null
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            val kTag = KDocKnownTag.findByTagName(tag)
            if (kTag != null) {
                val content = actionContext!!.callInReadUI {
                    kDoc.findSectionByTag(kTag)?.getContent()
                }
                if (content != null) {
                    return content
                }
            }

            return actionContext!!.callInReadUI {
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .map { it.getContent() }
                    .firstOrNull()
            }
        }

        return super.findDocByTag(psiElement, tag)
    }

    override fun findDocsByTag(psiElement: PsiElement?, tag: String?): List<String>? {

        if (psiElement == null || tag == null) {
            return null
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .map { it.getContent() }
                    .toList()
            }
        }


        return super.findDocsByTag(psiElement, tag)
    }

    override fun findDocsByTagAndName(psiElement: PsiElement?, tag: String, name: String): String? {


        if (psiElement == null) {
            return null
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            val kTag = KDocKnownTag.findByTagName(tag)
            if (kTag != null) {
                val content = actionContext!!.callInReadUI {
                    kDoc.findSectionByTag(kTag, name)?.getContent()
                }
                if (content != null) {
                    return content
                }
            }

            return actionContext!!.callInReadUI {
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .filter { it.getSubjectName() == name }
                    .map { it.getContent() }
                    .firstOrNull()
            }
        }

        return super.findDocsByTagAndName(psiElement, tag, name)
    }

    override fun getAttrOfDocComment(psiElement: PsiElement?): String? {

        if (psiElement == null) {
            return null
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                kDoc.getDefaultSection().getContent()
            }
        }

        return super.getAttrOfDocComment(psiElement)
    }

    override fun getSubTagMapOfDocComment(psiElement: PsiElement?, tag: String): Map<String, String?> {

        if (psiElement == null) {
            return Collections.emptyMap()
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                val tagMap: HashMap<String, String?> = HashMap()
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .forEach { tagMap[it.getSubjectName() ?: ""] = it.getContent() }
                return@callInReadUI tagMap
            } ?: Collections.emptyMap()
        }

        return super.getSubTagMapOfDocComment(psiElement, tag)

    }

    override fun getTagMapOfDocComment(psiElement: PsiElement?): Map<String, String?> {

        if (psiElement == null) {
            return Collections.emptyMap()
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                val tagMap: HashMap<String, String?> = HashMap()
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.children.toList() }
                    .filter { it is KDocTag }
                    .map { it as KDocTag }
                    .forEach {
                        tagMap[it.name ?: ""] = it.getContent()
                    }
                return@callInReadUI tagMap
            } ?: Collections.emptyMap()
        }

        return super.getTagMapOfDocComment(psiElement)
    }

    private fun findKDoc(psiElement: PsiElement): KDoc? {

        //kotlin doc
        if (psiElement is KtLightElement<*, *>) {
            return actionContext!!.callInReadUI {
                val kotlinOrigin = (psiElement).kotlinOrigin ?: return@callInReadUI null
                if (kotlinOrigin is KtDeclaration) {
                    return@callInReadUI kotlinOrigin.docComment
                }
                return@callInReadUI null
            }
        }

        if (psiElement is KtDeclaration) {
            return actionContext!!.callInReadUI { psiElement.docComment }
        }

        return null
    }
}
