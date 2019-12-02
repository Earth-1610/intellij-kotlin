package com.itangcent.intellij.tip

import com.itangcent.common.utils.IDUtils
import com.itangcent.intellij.constant.EventKey
import com.itangcent.intellij.context.ActionContext


class OnlyOnceInContextTip(private val content: String) : Tip {

    private val tipId = IDUtils.shortUUID()

    override fun tipable(): Boolean {
        val context = ActionContext.getContext() ?: return false
        val instance = context.instance(OnlyOnceInContextTipCache::class)
        if (instance.tipCache[tipId] == true) {
            return false
        }
        instance.tipCache[tipId] = true
        return true
    }

    override fun content(): String {
        return content
    }

    companion object {
        init {
            ActionContext.addDefaultInject { actionContextBuilder ->
                actionContextBuilder.bindInstance(OnlyOnceInContextTipCache::class, OnlyOnceInContextTipCache.instance)
                actionContextBuilder.addAction {
                    it.on(EventKey.ONCOMPLETED) {
                        OnlyOnceInContextTipCache.instance.clear()
                    }
                }
            }
        }
    }
}

internal class OnlyOnceInContextTipCache {
    val tipCache: LinkedHashMap<String, Boolean> = LinkedHashMap()

    fun clear() {
        this.tipCache.clear()
    }

    companion object {
        val instance = OnlyOnceInContextTipCache()
    }
}