package com.itangcent.intellij.file

import com.itangcent.common.utils.SystemUtils

class DefaultLocalFileRepository : AbstractLocalFileRepository() {
    override fun basePath(): String {

        var home = SystemUtils.userHome
        if (home.endsWith("/")) {
            home = home.substring(0, home.length - 1)
        }
        return home
    }
}