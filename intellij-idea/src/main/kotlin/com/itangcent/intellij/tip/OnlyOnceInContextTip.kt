package com.itangcent.intellij.tip

import com.google.inject.Singleton
import com.itangcent.common.SetupAble
import com.itangcent.common.utils.IDUtils
import com.itangcent.intellij.constant.EventKey
import com.itangcent.intellij.context.ActionContext


class OnlyOnceInContextTip(private val content: String) : Tip {

    private val tipId = IDUtils.shortUUID()

    override fun tipable(): Boolean {
        val context = ActionContext.getContext() ?: return false
        val instance = context.instance(OnlyOnceInContextTipCache::class)
        return instance.trySetCache(tipId)
    }

    override fun content(): String {
        return content
    }
}

@Singleton
internal class OnlyOnceInContextTipCache {
    var tipCache: LinkedHashMap<String, Boolean>? = null

    fun clear() {
        this.tipCache?.clear()
    }

    fun shareWith(cache: OnlyOnceInContextTipCache) {
        this.tipCache = cache.tipCache
    }

    fun trySetCache(id: String): Boolean {
        if (tipCache == null) {
            tipCache = LinkedHashMap()
            tipCache!![id] = true
            return true
        }
        if (tipCache!!.containsKey(id)) {
            return false
        }

        tipCache!![id] = true
        return true
    }
}

internal class OnlyOnceInContextTipSetup : SetupAble {
    override fun init() {
        ActionContext.addDefaultInject { actionContextBuilder ->
            //                actionContextBuilder.bindInstance(OnlyOnceInContextTipCache::class, OnlyOnceInContextTipCache.instance)
            actionContextBuilder.addAction { it ->
                it.on(EventKey.ON_START) { context ->
                    context.parentActionContext()?.let { parentContext ->
                        val parentCache = parentContext.instance(OnlyOnceInContextTipCache::class)
                        context.instance(OnlyOnceInContextTipCache::class).shareWith(parentCache)
                    }
                }
            }
        }
    }

}