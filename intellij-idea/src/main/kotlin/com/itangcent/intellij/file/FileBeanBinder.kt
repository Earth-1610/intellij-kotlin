package com.itangcent.intellij.file

import com.google.inject.Inject
import com.itangcent.common.utils.FileUtils
import com.itangcent.common.utils.GsonUtils
import com.itangcent.intellij.logger.Logger
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * bind a file which content present a bean
 */
open class FileBeanBinder<T : Any> : BeanBinder<T> {

    @Inject
    protected val logger: Logger? = null

    private val file: File

    private val beanType: KClass<T>

    private var init: (() -> T)? = null

    constructor(file: File, beanType: KClass<T>) {
        this.file = file
        this.beanType = beanType
        this.init = { beanType.createInstance() }
    }

    constructor(file: File, beanType: KClass<T>, init: () -> T) {
        this.file = file
        this.beanType = beanType
        this.init = init
    }

    override fun tryRead(): T? {
        val fileContent = FileUtils.read(file)
        if (fileContent.isNullOrBlank()) return null
        return GsonUtils.fromJson(fileContent, beanType)
    }

    override fun read(): T {
        return tryRead() ?: init!!.invoke()
    }

    override fun save(t: T?) {
        if (t == null) {
            FileUtils.remove(file)
            return
        }
        if (!file.exists()) {
            if (!file.createNewFile()) {
                logger!!.error("error to create new file:${file.path}")
                return
            }
        }
        FileUtils.write(file, GsonUtils.toJson(t))
    }
}