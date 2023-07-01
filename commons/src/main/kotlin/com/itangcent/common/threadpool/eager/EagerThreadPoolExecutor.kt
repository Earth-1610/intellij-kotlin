package com.itangcent.common.threadpool.eager

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * If greater than corePoolSize and fewer than maximumPoolSize threads are running
 * Try to start a new thread by add Worker instead of queued the task
 */
@Deprecated(message = "too complex")
class EagerThreadPoolExecutor : ThreadPoolExecutor {

    constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit,
        workQueueCapacity: Int,
        threadFactory: ThreadFactory,
        handler: RejectedExecutionHandler
    ) : this(corePoolSize, maximumPoolSize, keepAliveTime, unit, WorkQueue(workQueueCapacity), threadFactory, handler)

    constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit,
        threadFactory: ThreadFactory,
        handler: RejectedExecutionHandler
    ) : this(corePoolSize, maximumPoolSize, keepAliveTime, unit, WorkQueue(), threadFactory, handler)

    private constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit,
        workQueue: WorkQueue<Runnable>,
        threadFactory: ThreadFactory,
        handler: RejectedExecutionHandler
    ) : super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler) {
        workQueue.setExecutor(this)
        this.handler = handler
    }

    /**
     * task count
     */
    private val submittedTaskCount: AtomicInteger = AtomicInteger(0)

    @Volatile
    private var handler: RejectedExecutionHandler? = null

    /**
     * @return current tasks which are executed
     */
    fun getSubmittedTaskCount(): Int {
        return submittedTaskCount.get()
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        submittedTaskCount.decrementAndGet()
    }

    override fun execute(command: Runnable?) {
        if (command == null) {
            throw NullPointerException()
        }
        // do not increment in method beforeExecute!
        submittedTaskCount.incrementAndGet()
        try {
            super.execute(command)
        } catch (rx: RejectedExecutionException) {
            // retry to offer the task into queue.
            val queue = super.getQueue() as WorkQueue<Runnable>
            try {
                if (queue.retryOffer(command, 0, TimeUnit.MILLISECONDS)) {
                    return
                }
            } catch (x: InterruptedException) {
                Thread.interrupted()
            }
            submittedTaskCount.decrementAndGet()
            handler!!.rejectedExecution(command, this)
        } catch (t: Throwable) {
            // decrease any way
            submittedTaskCount.decrementAndGet()
            throw t
        }

    }

    /**
     * It offer a task if the executor's submittedTaskCount less than currentPoolThreadSize
     * or the currentPoolThreadSize more than executor's maximumPoolSize.
     * That can make the executor create new worker
     * when the task num is greater than corePoolSize but fewer than maximumPoolSize.
     */
    class WorkQueue<R> : LinkedBlockingQueue<R> {

        constructor() : super()
        constructor(capacity: Int) : super(capacity)

        private var executor: EagerThreadPoolExecutor? = null

        fun setExecutor(exec: EagerThreadPoolExecutor) {
            executor = exec
        }

        override fun offer(runnable: R): Boolean {
            if (executor == null) {
                throw RejectedExecutionException("The task queue does not have executor!")
            }

            val currentPoolThreadSize = executor!!.poolSize
            // have free worker. put task into queue to let the worker deal with task.
            if (executor!!.getSubmittedTaskCount() < currentPoolThreadSize) {
                return super.offer(runnable)
            }

            // return false to let executor create new worker.
            if (currentPoolThreadSize < executor!!.maximumPoolSize) {
                return false
            }

            // currentPoolThreadSize >= maximumPoolSize
            return super.offer(runnable)
        }

        /**
         * retry offer task
         *
         * @param o task
         * @return offer success or not
         * @throws RejectedExecutionException if executor is terminated.
         */
        @Throws(InterruptedException::class)
        fun retryOffer(o: R, timeout: Long, unit: TimeUnit): Boolean {
            if (executor!!.isShutdown) {
                throw RejectedExecutionException("Executor is shutdown!")
            }
            return super.offer(o, timeout, unit)
        }

    }
}
