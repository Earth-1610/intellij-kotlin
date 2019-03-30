package com.itangcent.intellij.logger

import com.google.inject.Inject
import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.itangcent.intellij.constant.CacheKey
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.io.PipedProcess
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ConsoleRunnerLogger : AbstractLogger() {

    private var pipedProcess: PipedProcess? = null

    private var logConsoleRunner: LogConsoleRunner? = null

    @Inject
    private val actionContext: ActionContext? = null

    private val lock = ReentrantLock()

    private fun checkProcess(): PipedProcess {
        if (pipedProcess == null || logConsoleRunner == null) {
            lock.withLock {
                if (pipedProcess == null) {

//                    actionContext?.runInUi(Runnable {
//                        Messages.showMessageDialog(actionContext.getCache<Project>(CacheKey.PROJECT),
//                                "create pipedProcess!", "Create", Messages.getInformationIcon())
//                    })

                    pipedProcess = PipedProcess()
                    actionContext!!.cache(CacheKey.LOGPROCESS, pipedProcess!!)
                    actionContext.on(CacheKey.ONCOMPLETED, Runnable {
                        pipedProcess?.setExitValue(0)
                        clear()
                    })
                }

                if (logConsoleRunner == null) {
                    val project = actionContext!!.instance(Project::class)

                    try {
                        val baseDir = project.baseDir
                        if (baseDir != null) {
                            logConsoleRunner = LogConsoleRunner(project, baseDir.path, pipedProcess!!)
                        } else {
                            logConsoleRunner = LogConsoleRunner(project, project.basePath!!, pipedProcess!!)
                        }
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
        if (logData == null) return
        try {
            val pipedProcess = checkProcess()
            val bytes = logData.toByteArray()
            if (bytes.size > 1024) {
                pipedProcess.getOutForInputStream()!!.write(bytes)
            } else {//split?
                pipedProcess.getOutForInputStream()!!.write(bytes)
            }
        } catch (ex: IOException) {
            log.warn("Error processLog:", ex)
        }
    }

    companion object {

        private val log = com.intellij.openapi.diagnostic.Logger.getInstance("ConsoleRunnerLogger")
    }
}
