package com.itangcent.intellij

import com.itangcent.common.spi.SetupAble
import com.itangcent.common.spi.SpiUtils
import com.itangcent.intellij.context.ActionContext

/**
 * Interface for providing custom plugin information.
 * Implementations of this interface can provide plugin-specific details.
 */
interface CustomInfo {
    /**
     * Returns the name of the plugin.
     * @return String representing the plugin name
     */
    fun pluginName(): String
}

/**
 * Constant key used for storing and retrieving the plugin name in the ActionContext.
 */
const val PLUGIN_NAME = "plugin.name"

/**
 * Extension function for ActionContextBuilder to bind a plugin name.
 * 
 * @param pluginName The name of the plugin to be bound
 */
fun ActionContext.ActionContextBuilder.bindPluginName(pluginName: String) {
    bindInstance(PLUGIN_NAME, pluginName)
}

/**
 * Supporter class that initializes custom plugin information
 */
class CustomInfoSupporter : SetupAble {
    override fun init() {
        // Load CustomInfo service implementation if available
        SpiUtils.loadService(CustomInfo::class)?.let { customInfo ->
            // Add default injection to ActionContext with the plugin name
            ActionContext.addDefaultInject {
                it.bindPluginName(customInfo.pluginName())
            }
        }
    }
}