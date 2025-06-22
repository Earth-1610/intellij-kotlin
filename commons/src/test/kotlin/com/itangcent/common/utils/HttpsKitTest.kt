package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import javax.net.ssl.HttpsURLConnection

class HttpsKitTest {

    @Test
    fun testHttps() {
        try {
            val url = "https://www.apache.org/licenses/LICENSE-2.0".asUrl()
            var connection = url.openConnection()
            if (connection is HttpsURLConnection) {//support https
                connection = connection.unsafe()
            }
            (connection.connect())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}