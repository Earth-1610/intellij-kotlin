package com.itangcent.intellij.context

import com.itangcent.intellij.constant.EventKey
import java.util.*

object ActionContextMonitor {

    private val actionContextList: LinkedList<ActionContext> = LinkedList()

    @Synchronized
    fun addActionContext(actionContext: ActionContext) {
        LOG.info("register ActionContextMonitor of context $actionContext")
        actionContextList.add(actionContext)
        startCheckActionContextThread(actionContext)
        actionContext.on(EventKey.ON_COMPLETED) {
            removeActionContext(it)
        }
    }

    @Synchronized
    fun removeActionContext(actionContext: ActionContext) {
        LOG.info("unregister ActionContextMonitor of context $actionContext")
        actionContextList.remove(actionContext)
        val monitorThread = actionContext.getCache<ActionContextMonitorThread>("_monitor_thread")
        monitorThread?.close()
    }

    private fun startCheckActionContextThread(actionContext: ActionContext) {
        val thread = ActionContextMonitorThread(actionContext)
        thread.start()
        actionContext.cache("_monitor_thread", thread)
    }

    private class ActionContextMonitorThread(private val actionContext: ActionContext) : Thread() {
        var running = true

        override fun run() {
            while (this.running) {
                try {
                    checkContext()
                } catch (e: Exception) {
                    LOG.error("failed check context", e)
                }
            }
        }

        fun checkContext() {
            LOG.info("Health check heartbeat $actionContext")
            val inactive = System.currentTimeMillis() - actionContext.lastActive()
            if (inactive > MAX_INACTIVE && actionContext.activeThreads(ThreadFlag.SWING) == 0) {
                LOG.warn("action was inactive for $inactive ms.")
                actionContext.stop()
            }
            if (actionContext.isStopped()) {
                LOG.info("stop check context $actionContext")
                running = false
            }

            val mainBoundary = actionContext.mainBoundary
            if (mainBoundary.count() > 0) {
                mainBoundary.waitComplete(MAX_INACTIVE, false)
            } else {
                sleep(1000)
            }
        }

        fun close() {
            this.running = false
        }

        companion object {
            private const val MAX_INACTIVE = 20000L
        }
    }

    //background idea log
    private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(ActionContextMonitor::class.java)
}
