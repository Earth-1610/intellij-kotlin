package com.itangcent.intellij.logger

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.itangcent.common.exception.ProcessCanceledException
import com.itangcent.intellij.constant.CacheKey
import com.itangcent.intellij.constant.EventKey
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.io.PipedProcess
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Singleton
class ConsoleRunnerLogger : AbstractLogger() {

    private var pipedProcess: PipedProcess? = null

    private var logConsoleRunner: LogConsoleRunner? = null

    @Volatile
    private var close = false

    @Inject
    private val actionContext: ActionContext? = null

    @Inject
    private lateinit var logConfig: LogConfig

    @Inject(optional = true)
    @Named("plugin.name")
    private val pluginName: String = "run"

    private val lock = ReentrantLock()

    private fun checkProcess(): PipedProcess {
        if (pipedProcess == null || logConsoleRunner == null) {
            lock.withLock {
                if (close) {
                    throw ProcessCanceledException("logger closed")
                }
                if (pipedProcess == null) {
                    pipedProcess = PipedProcess()
                    actionContext!!.cache(CacheKey.LOGPROCESS, pipedProcess!!)
                    actionContext.on(EventKey.ON_COMPLETED) {
                        close = true
                        try {
                            Thread.sleep(100)
                        } catch (_: Exception) {
                        }
                        pipedProcess?.setExitValue(0)
//                        logConsoleRunner?.close()
                        clear()
                    }
                }

                if (logConsoleRunner == null) {
                    val project = actionContext!!.instance(Project::class)

                    try {
                        logConsoleRunner = LogConsoleRunner(project, pluginName, project.basePath!!, pipedProcess!!)
                        logConsoleRunner!!.initAndRun()
                    } catch (ex: ExecutionException) {
                        actionContext.runInWriteUI {
                            Messages.showMessageDialog(
                                project, "Error at:" + ex.message
                                        + "trace:" + ExceptionUtils.getStackTrace(ex),
                                "Error", Messages.getInformationIcon()
                            )
                        }
                    }
                }

            }
        }

        return pipedProcess!!
    }

    private fun clear() {
        lock.withLock {
            this.pipedProcess = null
            this.logConsoleRunner = null
        }
    }

    override fun processLog(logData: String?) {
        if (logData == null || close) return
        try {
            val pipedProcess = checkProcess()
            val bytes = logData.toByteArray(logConfig.charset())
            if (bytes.size > 1024) {
                //split?
                pipedProcess.getOutForInputStream()!!.write(bytes)
            } else {
                LOG.info(logData)
                pipedProcess.getOutForInputStream()!!.write(bytes)
            }
        } catch (ex: IOException) {
            LOG.warn("Error processLog:", ex)
        }
    }

    companion object {
        //background idea log
        private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(ConsoleRunnerLogger::class.java)
    }
}
