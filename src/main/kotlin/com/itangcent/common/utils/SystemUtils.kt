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
}
