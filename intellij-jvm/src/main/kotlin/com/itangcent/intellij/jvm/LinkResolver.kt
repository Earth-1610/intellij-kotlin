package com.itangcent.intellij.jvm

import com.intellij.psi.PsiElement

interface LinkResolver {

    /**
     * @param linkTo will be null if [plainText] cannot be parsed
     */
    fun linkToPsiElement(
        plainText: String,
        linkTo: Any?
    ): String?
}