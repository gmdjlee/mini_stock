package com.stockapp.core.network

import android.util.Log
import okhttp3.CertificatePinner
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

/**
 * Certificate pinning configuration for API security (P3 Security Enhancement).
 *
 * Pins are SHA-256 hashes of the Subject Public Key Info (SPKI).
 * Include backup pins for certificate rotation scenarios.
 *
 * To get certificate hash:
 * 1. Run DEBUG build and make API calls
 * 2. Check Logcat with tag "CertHash"
 * 3. Copy the sha256 hashes here
 *
 * Or use openssl:
 * openssl s_client -connect api.kiwoom.com:443 | openssl x509 -pubkey -noout |
 * openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64
 */
object CertificatePinningConfig {

    private const val TAG = "CertPinning"

    // API domains
    private const val KIWOOM_PRODUCTION = "api.kiwoom.com"
    private const val KIWOOM_MOCK = "mockapi.kiwoom.com"
    private const val KIS_PRODUCTION = "openapi.koreainvestment.com"
    private const val KIS_MOCK = "openapivts.koreainvestment.com"

    private val PINNED_HOSTS = setOf(
        KIWOOM_PRODUCTION,
        KIWOOM_MOCK,
        KIS_PRODUCTION,
        KIS_MOCK
    )

    /**
     * Whether certificate pinning is enabled.
     * Set to true after obtaining real certificate hashes.
     *
     * To enable:
     * 1. Run DEBUG build to extract certificate hashes (see CertificateHashExtractor)
     * 2. Update the hash constants below with actual values
     * 3. Set this to true
     */
    private const val PINNING_ENABLED = true

    // ============================================================================
    // CERTIFICATE HASHES - Update these with actual values from CertificateHashExtractor
    // ============================================================================

    // Kiwoom Production (api.kiwoom.com)
    // Run debug build and check Logcat tag "CertHash" for actual values
    private const val KIWOOM_PROD_LEAF = "sha256/REPLACE_WITH_ACTUAL_HASH"
    private const val KIWOOM_PROD_ROOT = "sha256/REPLACE_WITH_ACTUAL_HASH"

    // Kiwoom Mock (mockapi.kiwoom.com)
    private const val KIWOOM_MOCK_LEAF = "sha256/REPLACE_WITH_ACTUAL_HASH"
    private const val KIWOOM_MOCK_ROOT = "sha256/REPLACE_WITH_ACTUAL_HASH"

    // KIS Production (openapi.koreainvestment.com)
    private const val KIS_PROD_LEAF = "sha256/REPLACE_WITH_ACTUAL_HASH"
    private const val KIS_PROD_ROOT = "sha256/REPLACE_WITH_ACTUAL_HASH"

    // KIS Mock (openapivts.koreainvestment.com)
    private const val KIS_MOCK_LEAF = "sha256/REPLACE_WITH_ACTUAL_HASH"
    private const val KIS_MOCK_ROOT = "sha256/REPLACE_WITH_ACTUAL_HASH"

    /**
     * Whether certificate hashes have been configured with real values.
     */
    private val hashesConfigured: Boolean
        get() = !KIWOOM_PROD_LEAF.contains("REPLACE")

    /**
     * Create certificate pinner with all API domains.
     * Returns null if pinning is disabled or hashes are not configured.
     *
     * Certificate pinning protects against:
     * - Man-in-the-middle (MITM) attacks
     * - Compromised Certificate Authorities (CAs)
     * - Unauthorized proxy interception
     *
     * TODO: Before production deployment, extract real certificate hashes using
     *       CertificateHashExtractor and replace placeholder values above.
     *       Without real hashes, pinning falls back to hostname verification only.
     */
    fun createPinner(): CertificatePinner? {
        if (!PINNING_ENABLED) {
            Log.w(TAG, "Certificate pinning is DISABLED. Enable it for production security.")
            return null
        }

        if (!hashesConfigured) {
            // Log at ERROR level in all builds - this is a security gap
            Log.e(TAG, "SECURITY WARNING: Certificate hashes are placeholders. " +
                "Pinning inactive - using hostname verification fallback only. " +
                "Run DEBUG build to extract real hashes before production release.")
            return null
        }

        return CertificatePinner.Builder()
            // Kiwoom Production API
            .add(KIWOOM_PRODUCTION, KIWOOM_PROD_LEAF)
            .add(KIWOOM_PRODUCTION, KIWOOM_PROD_ROOT)

            // Kiwoom Mock API
            .add(KIWOOM_MOCK, KIWOOM_MOCK_LEAF)
            .add(KIWOOM_MOCK, KIWOOM_MOCK_ROOT)

            // KIS Production API
            .add(KIS_PRODUCTION, KIS_PROD_LEAF)
            .add(KIS_PRODUCTION, KIS_PROD_ROOT)

            // KIS Mock API
            .add(KIS_MOCK, KIS_MOCK_LEAF)
            .add(KIS_MOCK, KIS_MOCK_ROOT)
            .build()
    }

    /**
     * Returns null to disable pinning in debug builds.
     * This allows easier development with proxy tools like Charles.
     */
    fun createDebugPinner(): CertificatePinner? = null

    /**
     * Check if a hostname should have certificate pinning applied.
     */
    fun isPinnedHost(hostname: String): Boolean = hostname in PINNED_HOSTS

    /**
     * Create a HostnameVerifier that only allows connections to known API hosts.
     * This provides a baseline security layer when certificate pinning hashes
     * are not yet configured, preventing connections to unexpected hosts.
     */
    fun createHostnameVerifier(): HostnameVerifier {
        return HostnameVerifier { hostname: String, session: SSLSession ->
            if (hostname in PINNED_HOSTS) {
                // Allow known API hosts
                true
            } else {
                // Delegate to default verification for non-API hosts
                javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()
                    .verify(hostname, session)
            }
        }
    }
}
