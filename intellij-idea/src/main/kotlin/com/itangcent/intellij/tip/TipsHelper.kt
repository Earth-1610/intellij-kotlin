package com.itangcent.intellij.tip

import com.google.inject.ImplementedBy

@ImplementedBy(DefaultLogTipsHelper::class)
interface TipsHelper {

    fun showTips(tip: Tip)
}