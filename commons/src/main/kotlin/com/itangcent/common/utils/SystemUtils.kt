package com.itangcent.common.utils

/**
 * @author tangcent
 */
object SystemUtils {
    val userName: String
        get() {
            var userName: String
            try {
                userName = System.getenv("USERNAME")
                if (!org.apache.commons.lang3.StringUtils.isBlank(userName)) {
                    return userName
                }
            } catch (ignored: Exception) {
            }

            try {
                userName = System.getProperty("user.name")
                if (!org.apache.commons.lang3.StringUtils.isBlank(userName)) {
                    return userName
                }
            } catch (ignored: Exception) {
            }

            return "Admin"
        }

    val userHome: String
        get() {

            var userName: String
            try {
                userName = System.getenv("USERHOME")
                if (!org.apache.commons.lang3.StringUtils.isBlank(userName)) {
                    return userName
                }
            } catch (ignored: Exception) {
            }

            try {
                userName = System.getProperty("user.home")
                if (!org.apache.commons.lang3.StringUtils.isBlank(userName)) {
                    return userName
                }
            } catch (ignored: Exception) {
            }

            return "~"
        }

    private val NEW_LINE = System.getProperty("line.separator")

    val isWindows: Boolean
        get() {
            val OS = System.getProperty("os.name")
            return OS.toLowerCase().contains("windows")
        }

    fun newLine(): String {
        return NEW_LINE ?: "\r\n"
    }
}
