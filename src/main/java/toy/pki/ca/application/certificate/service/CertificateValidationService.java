package toy.pki.ca.application.certificate.service;

import java.security.GeneralSecurityException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import toy.pki.ca.application.certificate.model.CertificateValidationResult;
import toy.pki.ca.application.certificate.model.CertificateValidationResult.Reason;
import toy.pki.ca.application.certificate.port.MyCertificateRepository;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.certificate.MyCertificate;

/**
 * Validates a managed chain against its self-signed root in this application's repository.
 * This local CA-management policy does not establish trust in an OS/browser trust store.
 * Revocation is checked for every record (including the root) using local state, not remote OCSP/CDP.
 */
@Service
@RequiredArgsConstructor
public class CertificateValidationService {

    private final MyCertificateRepository repository;

    public CertificateValidationResult validate(CertificateId certificateId) {
        Instant checkedAt = Instant.now();
        Date date = Date.from(checkedAt);
        List<CertificateId> ids = new ArrayList<>();
        List<X509Certificate> certificates = new ArrayList<>();
        Set<CertificateId> visited = new HashSet<>();
        CertificateId currentId = certificateId;

        while (currentId != null) {
            if (!visited.add(currentId)) {
                return result(Reason.CHAIN_CYCLE, "발급자 연결에 순환이 있습니다.", currentId, ids, checkedAt);
            }
            MyCertificate current = repository.findById(currentId).orElse(null);
            if (current == null) {
                return result(ids.isEmpty() ? Reason.NOT_FOUND : Reason.MISSING_ISSUER,
                    "인증서 또는 상위 발급 CA가 저장소에 없습니다.", currentId, ids, checkedAt);
            }
            ids.add(currentId);
            if (!(current.getCertificate() instanceof X509Certificate x509)) {
                return result(Reason.NOT_X509, "X.509 인증서가 아닙니다.", currentId, ids, checkedAt);
            }
            certificates.add(x509);
            String title = current.getAlias() == null || current.getAlias().isBlank()
                           ? x509.getSubjectX500Principal().getName() : current.getAlias();
            if (current.getStatus() == CertificateStatus.SUSPENDED) {
                return result(Reason.SUSPENDED, "효력이 정지된 인증서입니다: " + title, currentId, ids, checkedAt);
            }
            if (current.getRevokedAt() != null || current.getStatus() == CertificateStatus.REVOKED) {
                return result(Reason.REVOKED, "폐지된 인증서입니다: " + title, currentId, ids, checkedAt);
            }
            if (current.getStatus() == CertificateStatus.EXPIRED) {
                return result(Reason.EXPIRED, "만료된 인증서입니다: " + title, currentId, ids, checkedAt);
            }
            if (current.getStatus() != CertificateStatus.ACTIVE) {
                return result(Reason.UNKNOWN_STATUS, "상태를 확인할 수 없는 인증서입니다: " + title,
                    currentId, ids, checkedAt);
            }
            try {
                x509.checkValidity(date);
            } catch (CertificateExpiredException e) {
                return result(Reason.EXPIRED, "만료된 인증서입니다: " + title, currentId, ids, checkedAt);
            } catch (CertificateNotYetValidException e) {
                return result(Reason.NOT_YET_VALID, "유효기간이 시작되지 않은 인증서입니다: " + title,
                    currentId, ids, checkedAt);
            }

            CertificateId parentId = current.getIssuerCertificateId();
            if (parentId == null && !selfIssued(x509)) {
                return result(Reason.MISSING_ISSUER, "상위 발급 CA 연결이 없습니다: " + title,
                    currentId, ids, checkedAt);
            }
            if (parentId == null || certificates.size() > 1) {
                if (!canIssue(x509)) {
                    return result(Reason.INVALID_CA, "발급 CA에 CA 제약 또는 KEY_CERT_SIGN 권한이 없습니다: " + title,
                        currentId, ids, checkedAt);
                }
                // PKIX excludes the trust anchor; apply pathLen to it as well, including a CA target.
                long caBelow = certificates.subList(0, certificates.size() - 1).stream()
                    .filter(child -> child.getBasicConstraints() >= 0 && !selfIssued(child)).count();
                if (caBelow > x509.getBasicConstraints()) {
                    return result(Reason.INVALID_CA, "발급 CA의 경로 길이 제한을 초과했습니다: " + title,
                        currentId, ids, checkedAt);
                }
            }
            if (certificates.size() > 1) {
                X509Certificate child = certificates.get(certificates.size() - 2);
                if (!child.getIssuerX500Principal().equals(x509.getSubjectX500Principal())) {
                    return result(Reason.INVALID_CHAIN, "인증서의 발급자 이름이 연결된 CA와 일치하지 않습니다.",
                        ids.get(ids.size() - 2), ids, checkedAt);
                }
                try {
                    child.verify(x509.getPublicKey());
                } catch (GeneralSecurityException e) {
                    return result(Reason.INVALID_SIGNATURE, "인증서 서명이 연결된 발급 CA와 일치하지 않습니다.",
                        ids.get(ids.size() - 2), ids, checkedAt);
                }
            }
            currentId = parentId;
        }

        if (certificates.isEmpty()) {
            return result(Reason.NOT_FOUND, "검증할 인증서가 없습니다.", certificateId, ids, checkedAt);
        }
        X509Certificate root = certificates.getLast();
        try {
            root.verify(root.getPublicKey());
        } catch (GeneralSecurityException e) {
            return result(Reason.INVALID_SIGNATURE, "저장소 루트 CA의 자체 서명이 올바르지 않습니다.",
                ids.getLast(), ids, checkedAt);
        }
        try {
            var path = CertificateFactory.getInstance("X.509")
                .generateCertPath(certificates.subList(0, certificates.size() - 1));
            var parameters = new PKIXParameters(Set.of(new TrustAnchor(root, null)));
            parameters.setDate(date);
            // Every managed record was checked above. Never silently fall back to remote revocation lookup.
            parameters.setRevocationEnabled(false);
            CertPathValidator.getInstance("PKIX").validate(path, parameters);
        } catch (CertPathValidatorException e) {
            CertificateId failed = e.getIndex() >= 0 && e.getIndex() < ids.size()
                                   ? ids.get(e.getIndex()) : ids.getFirst();
            return result(Reason.INVALID_CHAIN, "인증서 경로의 PKIX 제약 검증에 실패했습니다.",
                failed, ids, checkedAt);
        } catch (GeneralSecurityException e) {
            return result(Reason.INVALID_CHAIN, "인증서 경로를 검증하지 못했습니다.",
                ids.getFirst(), ids, checkedAt);
        }
        return result(Reason.VALID, "저장소 루트 CA까지의 서명·유효기간·폐지 상태와 경로 제약을 확인했습니다.",
            null, ids, checkedAt);
    }

    public void requireValidIssuer(CertificateId issuerId) {
        CertificateValidationResult result = validate(issuerId);
        if (!result.isValid()) {
            throw new IllegalArgumentException("An active issuer with a valid managed chain is required: "
                + result.message());
        }
    }

    private static boolean selfIssued(X509Certificate certificate) {
        return certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal());
    }

    private static boolean canIssue(X509Certificate certificate) {
        boolean[] usages = certificate.getKeyUsage();
        return certificate.getBasicConstraints() >= 0 && usages != null && usages.length > 5 && usages[5];
    }

    private static CertificateValidationResult result(Reason reason, String message, CertificateId failed,
        List<CertificateId> chain, Instant checkedAt) {
        return new CertificateValidationResult(reason, message, failed, chain, checkedAt);
    }
}
