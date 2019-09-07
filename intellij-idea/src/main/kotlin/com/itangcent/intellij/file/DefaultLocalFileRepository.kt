package com.itangcent.intellij.file

import com.itangcent.common.utils.SystemUtils
import java.io.File

class DefaultLocalFileRepository : AbstractLocalFileRepository() {
    override fun basePath(): String {

        var home = SystemUtils.userHome
        if (home.endsWith(File.separator)) {
            home = home.substring(0, home.length - 1)
        }
        return home
    }
}