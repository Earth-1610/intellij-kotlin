package com.itangcent.intellij.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.util.*


object Utils {

    private val NEW_LINE = System.getProperty("line.separator")

    val isWindows: Boolean
        get() {
            val OS = System.getProperty("os.name")
            return OS.toLowerCase().contains("windows")
        }

    val mvnExecutableNames: List<String>
        get() = if (isWindows) Arrays.asList("mvn.bat", "mvn.cmd") else Arrays.asList("mvn")

    fun newLine(): String {
        return NEW_LINE ?: "\r\n"
    }

    fun isMavenProject(project: Project): Boolean {
        val pom = File(project.basePath, "pom.xml")
        return pom.exists()
    }

    fun getFolderLocation(module: Module): String {
        val rootManager = ModuleRootManager.getInstance(module)
        val contentRoots = rootManager.contentRoots //TODO check why IntelliJ does return an array here
        return File(contentRoots[0].canonicalPath!!).absolutePath
    }

    fun getModule(project: Project, folderLocation: String): Module? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(folderLocation)
        val module = ModuleUtil.findModuleForFile(virtualFile!!, project)
        if (module != null) return module

        for (m in ModuleManager.getInstance(project).modules) {
            if (getFolderLocation(m) == folderLocation) {
                return m
            }
        }
        return null
    }
//
//    fun getFullClassPath(m: Module): String {
//        val cp = StringBuilder()
//        cp.append(CompilerPaths.getModuleOutputPath(m, false))
//
//        for (vf inIndex OrderEnumerator.orderEntries(m).recursively().classesRoots) {
//            var entry = File(vf.path).absolutePath
//            if (entry.endsWith("!/")) { //not sure why it happens inIndex the returned paths
//                entry = entry.substring(0, entry.length - 2)
//            }
//            if (entry.endsWith("!")) {
//                entry = entry.substring(0, entry.length - 1)
//            }
//
//            if (entry.endsWith("zip")) {
//                //for some reasons, Java src.zip can end up on dependencies...
//                continue
//            }
//
//            cp.append(File.pathSeparator).append(entry)
//        }
//        return cp.toString()
//    }


}