package toy.pki.ca.application.certificate.model;

import java.time.Instant;
import java.util.List;
import toy.pki.ca.domain.certificate.CertificateId;

public record CertificateValidationResult(
    Reason reason,
    String message,
    CertificateId failedCertificateId,
    List<CertificateId> chain,
    Instant checkedAt
) {
    public CertificateValidationResult {
        chain = List.copyOf(chain);
    }

    public boolean isValid() {
        return reason == Reason.VALID;
    }

    public enum Reason {
        VALID,
        NOT_FOUND,
        NOT_X509,
        REVOKED,
        SUSPENDED,
        EXPIRED,
        NOT_YET_VALID,
        UNKNOWN_STATUS,
        MISSING_ISSUER,
        CHAIN_CYCLE,
        INVALID_CA,
        INVALID_SIGNATURE,
        INVALID_CHAIN
    }
}
