package com.stockapp.core.network

import okhttp3.CertificatePinner

/**
 * Certificate pinning configuration for API security (P3 Security Enhancement).
 *
 * Pins are SHA-256 hashes of the Subject Public Key Info (SPKI).
 * Include backup pins for certificate rotation scenarios.
 *
 * To get certificate hash:
 * openssl s_client -connect api.kiwoom.com:443 | openssl x509 -pubkey -noout |
 * openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64
 */
object CertificatePinningConfig {

    // Kiwoom API domains
    private const val KIWOOM_PRODUCTION = "api.kiwoom.com"
    private const val KIWOOM_MOCK = "mockapi.kiwoom.com"

    // KIS API domain
    private const val KIS_DOMAIN = "openapi.koreainvestment.com"

    /**
     * Create certificate pinner with all API domains.
     *
     * NOTE: The pins below are placeholders that will be populated during deployment.
     * In production, replace with actual certificate hashes obtained from the servers.
     *
     * Certificate pinning protects against:
     * - Man-in-the-middle (MITM) attacks
     * - Compromised Certificate Authorities (CAs)
     * - Unauthorized proxy interception
     */
    fun createPinner(): CertificatePinner {
        return CertificatePinner.Builder()
            // Kiwoom Production API - primary and backup pins
            // Primary: Current certificate
            // Backup: Next certificate (for rotation)
            .add(
                KIWOOM_PRODUCTION,
                "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=" // Primary
            )
            .add(
                KIWOOM_PRODUCTION,
                "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvPKG0=" // Backup (root CA)
            )

            // Kiwoom Mock API - primary and backup pins
            .add(
                KIWOOM_MOCK,
                "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=" // Primary
            )
            .add(
                KIWOOM_MOCK,
                "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvPKG0=" // Backup (root CA)
            )

            // KIS (Korea Investment & Securities) API - primary and backup pins
            .add(
                KIS_DOMAIN,
                "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=" // Primary
            )
            .add(
                KIS_DOMAIN,
                "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvPKG0=" // Backup (root CA)
            )
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
            hostname == KIS_DOMAIN
    }
}
