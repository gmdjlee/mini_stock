package com.stockapp.core.network

import android.util.Log
import okhttp3.CertificatePinner

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

    // Kiwoom API domains
    private const val KIWOOM_PRODUCTION = "api.kiwoom.com"
    private const val KIWOOM_MOCK = "mockapi.kiwoom.com"

    // KIS API domains
    private const val KIS_PRODUCTION = "openapi.koreainvestment.com"
    private const val KIS_MOCK = "openapivts.koreainvestment.com"

    /**
     * Whether certificate pinning is enabled.
     * Set to true after obtaining real certificate hashes.
     *
     * To enable:
     * 1. Run DEBUG build to extract certificate hashes (see CertificateHashExtractor)
     * 2. Update the hash constants below with actual values
     * 3. Set this to true
     */
    private const val PINNING_ENABLED = false

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
     * Create certificate pinner with all API domains.
     * Returns null if pinning is disabled or hashes are not configured.
     *
     * Certificate pinning protects against:
     * - Man-in-the-middle (MITM) attacks
     * - Compromised Certificate Authorities (CAs)
     * - Unauthorized proxy interception
     */
    fun createPinner(): CertificatePinner? {
        if (!PINNING_ENABLED) {
            Log.w(TAG, "Certificate pinning is DISABLED. Set PINNING_ENABLED=true after configuring hashes.")
            return null
        }

        if (KIWOOM_PROD_LEAF.contains("REPLACE")) {
            Log.e(TAG, "Certificate hashes not configured! Run DEBUG build to extract hashes.")
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
    fun isPinnedHost(hostname: String): Boolean {
        return hostname == KIWOOM_PRODUCTION ||
            hostname == KIWOOM_MOCK ||
            hostname == KIS_PRODUCTION ||
            hostname == KIS_MOCK
    }
}
