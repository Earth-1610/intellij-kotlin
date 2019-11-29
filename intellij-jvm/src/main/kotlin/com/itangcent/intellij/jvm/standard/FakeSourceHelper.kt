package com.itangcent.intellij.jvm.standard

import com.google.inject.Singleton
import com.intellij.psi.PsiClass
import com.itangcent.intellij.jvm.SourceHelper

@Singleton
class FakeSourceHelper : SourceHelper {
    override fun getSourceClass(original: PsiClass): PsiClass {
        return original
    }
}
