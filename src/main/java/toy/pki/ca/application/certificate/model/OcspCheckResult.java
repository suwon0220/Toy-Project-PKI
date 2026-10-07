package toy.pki.ca.application.certificate.model;

import java.time.Instant;

public record OcspCheckResult(String status, Instant revokedAt, Instant thisUpdate,
    Instant nextUpdate, Instant producedAt, boolean signatureValid) {}
