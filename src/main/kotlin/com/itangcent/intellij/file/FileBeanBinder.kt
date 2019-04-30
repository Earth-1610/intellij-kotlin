package com.itangcent.intellij.file

import com.itangcent.common.utils.FileUtils
import com.itangcent.common.utils.GsonUtils
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * bind a file which content present a bean
 */
open class FileBeanBinder<T : Any> : BeanBinder<T> {

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

    override fun read(): T {
        val fileContent = FileUtils.read(file)
        if (fileContent.isBlank()) return init!!.invoke()
        return GsonUtils.fromJson(fileContent, beanType)
    }

    override fun save(t: T) {
        FileUtils.write(file, GsonUtils.toJson(t))
    }
}