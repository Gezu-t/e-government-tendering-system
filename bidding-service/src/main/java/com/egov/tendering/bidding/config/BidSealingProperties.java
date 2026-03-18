package com.egov.tendering.bidding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for bid sealing cryptographic parameters.
 * All values are bound from the {@code bidding.seal.*} namespace in application.yaml
 * (or config-server), making algorithm choices and schedule tunable without redeployment.
 */
@Configuration
@ConfigurationProperties(prefix = "bidding.seal")
@Data
public class BidSealingProperties {

    /** AES mode used to encrypt bid content. */
    private String encryptionAlgorithm = "AES/GCM/NoPadding";

    /** Hash algorithm used to compute the content integrity digest. */
    private String hashAlgorithm = "SHA-256";

    /** GCM authentication tag length in bits. */
    private int gcmTagLength = 128;

    /** GCM initialisation-vector length in bytes. */
    private int gcmIvLength = 12;

    /** Cron expression controlling the scheduled auto-unseal sweep. */
    private String unsealCheckCron = "0 */15 * * * *";

    /**
     * Base64-encoded 256-bit (32-byte) master key used to wrap content encryption keys.
     * <p>
     * Must be supplied via the {@code SEALING_MASTER_KEY} environment variable.
     * Generate with: {@code openssl rand -base64 32}
     * <p>
     * Never commit a real value — this property has no default on purpose.
     */
    private String masterKeyBase64;
}
