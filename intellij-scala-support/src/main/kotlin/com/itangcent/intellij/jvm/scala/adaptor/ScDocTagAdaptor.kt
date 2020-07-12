package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocTag
import com.itangcent.common.utils.invokeMethod
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocTag
import scala.Function1
import kotlin.reflect.full.functions

interface ScDocTagAdaptor : ScAdaptor<ScDocTag>, PsiDocTag {

    /**
     * Get allText by invoke `getAllText` or get from  PsiElement.
     * org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocTag.getAllText has be removed at
     * https://github.com/JetBrains/intellij-scala/commit/4b9048f77eabaa7efc980c32c4caffca3626e52d#diff-40f22199e9a828dd77895abced478bef
     */
    fun getAllText(): String?
}

fun ScDocTag.scalaAdaptor(): ScDocTagAdaptor {
    return if (this::class.functions.any { it.name == "getAllText" }) {
        InvokeAbleScDocTagAdaptor(this)
    } else {
        OriginalScDocTagAdaptor(this)
    }
}

/**
 * read only
 */
class InvokeAbleScDocTagAdaptor(private val scDocTag: ScDocTag) : ScDocTagAdaptor,
    PsiDocTag by scDocTag {

    override fun adaptor(): ScDocTag {
        return scDocTag
    }

    /**
     * Get allText by invoke `getAllText` or get from  PsiElement.
     * org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocTag.getAllText has be removed at
     * https://github.com/JetBrains/intellij-scala/commit/4b9048f77eabaa7efc980c32c4caffca3626e52d#diff-40f22199e9a828dd77895abced478bef
     */
    override fun getAllText(): String? {
        return try {
            this.scDocTag.invokeMethod(
                "getAllText",
                (Function1<PsiElement, String> { v1 -> v1?.text ?: "" })
            ) as? String
        } catch (e: Exception) {
            scDocTag.text
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InvokeAbleScDocTagAdaptor

        if (scDocTag != other.scDocTag) return false

        return true
    }

    override fun hashCode(): Int {
        return scDocTag.hashCode()
    }
}

/**
 * read only
 */
class OriginalScDocTagAdaptor(private val scDocTag: ScDocTag) : ScDocTagAdaptor,
    PsiDocTag by scDocTag {

    override fun adaptor(): ScDocTag {
        return scDocTag
    }

    /**
     * Get allText from  PsiElement.
     * org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocTag.getAllText has be removed at
     * https://github.com/JetBrains/intellij-scala/commit/4b9048f77eabaa7efc980c32c4caffca3626e52d#diff-40f22199e9a828dd77895abced478bef
     */
    override fun getAllText(): String? {
        return this.scDocTag.text
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OriginalScDocTagAdaptor

        if (scDocTag != other.scDocTag) return false

        return true
    }

    override fun hashCode(): Int {
        return scDocTag.hashCode()
    }
}