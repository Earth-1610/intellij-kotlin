package com.itangcent.intellij.jvm.standard;

import com.google.inject.Singleton;
import com.intellij.psi.PsiClass;
import com.itangcent.intellij.jvm.SourceHelper;
import org.jetbrains.annotations.NotNull;

@Singleton
public class FakeSourceHelper implements SourceHelper {
    @NotNull
    @Override
    public PsiClass getSourceClass(@NotNull PsiClass original) {
        return original;
    }
}
