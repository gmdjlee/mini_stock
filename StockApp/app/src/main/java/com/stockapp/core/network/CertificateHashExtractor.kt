package com.stockapp.core.network

import android.util.Base64
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * Interceptor that extracts and logs SSL certificate hashes.
 * Use this in DEBUG builds to obtain the actual SPKI hashes for certificate pinning.
 *
 * Usage:
 * 1. Add this interceptor to OkHttpClient in debug builds
 * 2. Make API calls to each domain
 * 3. Check Logcat for "CertHash" tag
 * 4. Copy the sha256 hashes to CertificatePinningConfig.kt
 */
class CertificateHashExtractor : Interceptor {

    companion object {
        private const val TAG = "CertHash"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host

        // Proceed with the request first
        val response = chain.proceed(request)

        // Extract certificate hashes from the connection
        try {
            val handshake = response.handshake
            if (handshake != null) {
                val certificates = handshake.peerCertificates

                Log.d(TAG, "═══════════════════════════════════════════════════════")
                Log.d(TAG, "Host: $host")
                Log.d(TAG, "Certificate chain (${certificates.size} certificates):")
                Log.d(TAG, "───────────────────────────────────────────────────────")

                certificates.forEachIndexed { index, cert ->
                    val x509 = cert as? X509Certificate ?: return@forEachIndexed
                    val hash = extractSpkiHash(x509)

                    Log.d(TAG, "[$index] Subject: ${x509.subjectX500Principal.name}")
                    Log.d(TAG, "    Issuer: ${x509.issuerX500Principal.name}")
                    Log.d(TAG, "    Expires: ${x509.notAfter}")
                    Log.d(TAG, "    SHA256 Pin: sha256/$hash")
                    Log.d(TAG, "")

                    // Copy-paste friendly format for leaf and root
                    when (index) {
                        0 -> {
                            Log.i(TAG, "=== COPY THIS FOR $host ===")
                            Log.i(TAG, "\"sha256/$hash\" // Leaf certificate")
                        }
                        certificates.lastIndex -> {
                            Log.i(TAG, "\"sha256/$hash\" // Root CA")
                        }
                    }
                }
                Log.d(TAG, "═══════════════════════════════════════════════════════")
            }
        } catch (e: SSLPeerUnverifiedException) {
            Log.e(TAG, "Failed to get peer certificates for $host: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting certificate hash for $host: ${e.message}")
        }

        return response
    }

    /**
     * Extracts the SHA-256 hash of the Subject Public Key Info (SPKI).
     * This is the format used by OkHttp's CertificatePinner.
     */
    private fun extractSpkiHash(certificate: X509Certificate): String {
        val publicKey = certificate.publicKey.encoded
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKey)
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}
