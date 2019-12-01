package com.itangcent.intellij.tip

import com.google.inject.ImplementedBy

@ImplementedBy(DefaultTipsHelper::class)
interface TipsHelper {

    fun showTips(tip: Tip)
}