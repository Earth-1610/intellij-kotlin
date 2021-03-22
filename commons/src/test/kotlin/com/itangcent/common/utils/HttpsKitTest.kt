package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class HttpsKitTest {

    @Test
    fun testHttps() {
        try {
            val url = URL("https://www.apache.org/licenses/LICENSE-2.0")
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