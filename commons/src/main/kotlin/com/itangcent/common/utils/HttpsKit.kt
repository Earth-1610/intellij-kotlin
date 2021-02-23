package com.itangcent.common.utils

import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Overrides Java default certificate authentication
 */
private val TRUST_ALL_CERTS = arrayOf<TrustManager>(object : X509TrustManager {

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }
})

/**
 * Never authenticate the host
 */
private val DO_NOT_VERIFY: HostnameVerifier = HostnameVerifier { hostname, session -> true }

fun HttpsURLConnection.trustAllHosts(): HttpsURLConnection {
    try {
        val sc: SSLContext = SSLContext.getInstance("TLS")
        sc.init(null, TRUST_ALL_CERTS, SecureRandom())
        val newFactory: SSLSocketFactory = sc.socketFactory
        this.sslSocketFactory = newFactory
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return this
}


fun HttpsURLConnection.notVerify(): HttpsURLConnection {
    this.hostnameVerifier = DO_NOT_VERIFY
    return this
}

/**
 * Make some unsafe settings for the HttpsURLConnection.
 * This method is a shorthand for:
 * <blockquote><pre>
 *     trustAllHosts().notVerify()
 * </pre></blockquote>
 *
 * @return the HttpsURLConnection
 * @see trustAllHosts
 * @see notVerify
 */
fun HttpsURLConnection.unsafe(): HttpsURLConnection {
    this.trustAllHosts()
    this.notVerify()
    return this
}
