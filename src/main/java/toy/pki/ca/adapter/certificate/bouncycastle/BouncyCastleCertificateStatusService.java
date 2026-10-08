package toy.pki.ca.adapter.certificate.bouncycastle;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;
import org.bouncycastle.cert.ocsp.RespID;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.cert.ocsp.jcajce.JcaBasicOCSPRespBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import toy.pki.ca.application.certificate.model.OcspCheckResult;
import toy.pki.ca.application.certificate.port.MyCertificateRepository;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.certificate.MyCertificate;
import toy.pki.kms.application.service.KeyManagementService;

@Slf4j
@Service
@RequiredArgsConstructor
public class BouncyCastleCertificateStatusService {

    private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AtomicLong crlNumber = new AtomicLong(System.currentTimeMillis());
    private final MyCertificateRepository repository;
    private final KeyManagementService kms;

    private static X509Certificate x509(MyCertificate certificate) {
        if (!(certificate.getCertificate() instanceof X509Certificate x509)) {
            throw new IllegalArgumentException("X.509 인증서가 필요합니다.");
        }
        return x509;
    }

    private static boolean revoked(MyCertificate certificate) {
        return certificate.getRevokedAt() != null || certificate.getStatus() == CertificateStatus.REVOKED
            || certificate.getStatus() == CertificateStatus.SUSPENDED;
    }

    private static Instant revokedAt(MyCertificate certificate) {
        if (certificate.getRevokedAt() == null) {
            throw new IllegalArgumentException("폐기 시각이 기록되지 않은 인증서입니다.");
        }
        return certificate.getRevokedAt();
    }

    private static int reason(MyCertificate certificate) {
        return certificate.getStatus() == CertificateStatus.SUSPENDED
               ? CRLReason.certificateHold
               : CRLReason.unspecified;
    }

    private static Instant nextUpdate(Instant now, X509Certificate ca, int minutes) {
        Instant expiry = ca.getNotAfter().toInstant();
        return now.plusSeconds(minutes * 60L).isBefore(expiry) ? now.plusSeconds(minutes * 60L) : expiry;
    }

    private static byte[] error(int status) throws IOException {
        try {
            return new OCSPRespBuilder().build(status, null).getEncoded();
        } catch (OCSPException e) {
            throw new IOException(e);
        }
    }

    public X509CRL crl(CertificateId issuerId) throws GeneralSecurityException, IOException {
        MyCertificate issuer = find(issuerId);
        X509Certificate ca = signingCertificate(issuer);
        boolean[] usage = ca.getKeyUsage();
        if (usage == null || usage.length <= 6 || !usage[6]) {
            throw new IllegalArgumentException("이 CA 인증서에는 CRL_SIGN 용도가 없습니다.");
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var builder = new JcaX509v2CRLBuilder(ca.getSubjectX500Principal(), Date.from(now));
        builder.setNextUpdate(Date.from(nextUpdate(now, ca, 24 * 60)));
        for (MyCertificate certificate : issuedBy(issuer)) {
            if (revoked(certificate)) {
                builder.addCRLEntry(x509(certificate).getSerialNumber(), Date.from(revokedAt(certificate)), reason(certificate));
            }
        }
        builder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(ca));
        long number = crlNumber.incrementAndGet();
        builder.addExtension(Extension.cRLNumber, false, new CRLNumber(BigInteger.valueOf(number)));
        X509CRL crl = new JcaX509CRLConverter().setProvider(PROVIDER)
                                               .getCRL(builder.build(KmsContentSigner.forKey(kms, issuer.getSubjectKeyId(), ca.getPublicKey())));
        crl.verify(ca.getPublicKey(), PROVIDER);
        log.info("CRL issued: issuerCertificateId={}, number={}", issuerId.id(), number);
        return crl;
    }

    public CertificateId issuerId(CertificateId certificateId) {
        MyCertificate certificate = find(certificateId);
        if (certificate.getIssuerCertificateId() != null) {
            return certificate.getIssuerCertificateId();
        }
        X509Certificate x509 = x509(certificate);
        if (x509.getBasicConstraints() >= 0 && x509.getIssuerX500Principal().equals(x509.getSubjectX500Principal())) {
            return certificate.getId();
        }
        throw new IllegalArgumentException("발급 CA 인증서가 없어 OCSP를 요청할 수 없습니다.");
    }

    public byte[] request(CertificateId certificateId) throws GeneralSecurityException, IOException {
        try {
            X509Certificate certificate = x509(find(certificateId));
            X509Certificate ca = x509(find(issuerId(certificateId)));
            byte[] nonce = new byte[32];
            RANDOM.nextBytes(nonce);
            var digest = new JcaDigestCalculatorProviderBuilder().setProvider(PROVIDER).build();
            var id = new CertificateID(digest.get(CertificateID.HASH_SHA1), new JcaX509CertificateHolder(ca), certificate.getSerialNumber());
            return new OCSPReqBuilder().addRequest(id).setRequestExtensions(new Extensions(
                                           new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false, new DEROctetString(nonce).getEncoded())))
                                       .build().getEncoded();
        } catch (OCSPException | OperatorCreationException e) {
            throw new GeneralSecurityException("OCSP 요청을 생성하지 못했습니다.", e);
        }
    }

    // The web check and the binary HTTP endpoint use the same OCSP request/response path.
    public OcspCheckResult check(CertificateId certificateId) throws GeneralSecurityException, IOException {
        CertificateId issuerId = issuerId(certificateId);
        byte[] request = request(certificateId);
        byte[] response = respond(issuerId, request);
        OcspCheckResult result = verify(request, response, x509(find(issuerId)));
        log.info("OCSP checked: certificateId={}, issuerCertificateId={}, status={}, signatureValid={}",
            certificateId.id(), issuerId.id(), result.status(), result.signatureValid());
        return result;
    }

    public byte[] respond(CertificateId issuerId, byte[] encoded) throws GeneralSecurityException, IOException {
        OCSPReq request;
        try {
            if (encoded.length > 8192) {
                return error(OCSPRespBuilder.MALFORMED_REQUEST);
            }
            request = new OCSPReq(encoded);
            if (request.getVersionNumber() != 1 || request.getRequestList().length == 0 || request.getRequestList().length > 20 || request.isSigned()
                || !request.getCriticalExtensionOIDs().isEmpty()) {
                return error(OCSPRespBuilder.MALFORMED_REQUEST);
            }
            Extension nonce = request.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
            if (nonce != null) {
                int size = ASN1OctetString.getInstance(nonce.getParsedValue()).getOctets().length;
                if (size < 1 || size > 32) {
                    return error(OCSPRespBuilder.MALFORMED_REQUEST);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            return error(OCSPRespBuilder.MALFORMED_REQUEST);
        }
        MyCertificate issuer = find(issuerId);
        X509Certificate ca = signingCertificate(issuer);
        try {
            var digest = new JcaDigestCalculatorProviderBuilder().setProvider(PROVIDER).build();
            var caHolder = new JcaX509CertificateHolder(ca);
            var builder = new JcaBasicOCSPRespBuilder(ca.getPublicKey(), digest.get(CertificateID.HASH_SHA1));
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            List<MyCertificate> certificates = issuedBy(issuer);
            for (var entry : request.getRequestList()) {
                CertificateID id = entry.getCertID();
                try {
                    if (!id.matchesIssuer(caHolder, digest)) {
                        return error(OCSPRespBuilder.UNAUTHORIZED);
                    }
                } catch (OCSPException e) {
                    return error(OCSPRespBuilder.MALFORMED_REQUEST);
                }
                if (entry.getSingleRequestExtensions() != null
                    && entry.getSingleRequestExtensions().getCriticalExtensionOIDs().length > 0) {
                    return error(OCSPRespBuilder.MALFORMED_REQUEST);
                }
                MyCertificate certificate = certificates.stream()
                                                        .filter(item -> x509(item).getSerialNumber()
                                                                                  .equals(id.getSerialNumber()))
                                                        .findFirst()
                                                        .orElse(null);
                // Include a self-signed CA's own serial when checking the trust anchor.
                if (certificate == null && issuer.getIssuerCertificateId() == null && ca.getSerialNumber()
                                                                                        .equals(id.getSerialNumber())) {
                    certificate = issuer;
                }
                org.bouncycastle.cert.ocsp.CertificateStatus status = null;
                if (certificate == null || certificate.getStatus() == CertificateStatus.UNKNOWN) {
                    status = new UnknownStatus();
                } else if (revoked(certificate)) {
                    status = new RevokedStatus(Date.from(revokedAt(certificate)), reason(certificate));
                }
                builder.addResponse(id, status, Date.from(now), Date.from(nextUpdate(now, ca, 5)), null);
            }
            Extension nonce = request.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
            if (nonce != null) {
                builder.setResponseExtensions(new Extensions(nonce));
            }
            var basic = builder.build(KmsContentSigner.forKey(kms, issuer.getSubjectKeyId(), ca.getPublicKey()),
                new X509CertificateHolder[]{caHolder}, Date.from(now));
            if (!basic.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(PROVIDER)
                                                                               .build(ca.getPublicKey()))) {
                throw new GeneralSecurityException("OCSP 응답 서명이 CA 공개키와 일치하지 않습니다.");
            }
            log.info("OCSP response issued: issuerCertificateId={}, requestCount={}", issuerId.id(), request.getRequestList().length);
            return new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basic).getEncoded();
        } catch (OCSPException | OperatorCreationException e) {
            // Bouncy Castle wraps failures from ContentSigner; preserve the shared KMS advice.
            if (e.getCause() instanceof RestClientException failure) {
                throw failure;
            }
            throw new GeneralSecurityException("OCSP 응답을 생성하지 못했습니다.", e);
        }
    }

    public OcspCheckResult verify(byte[] requestBytes, byte[] responseBytes, X509Certificate issuer)
        throws GeneralSecurityException, IOException {
        try {
            OCSPReq request = new OCSPReq(requestBytes);
            OCSPResp response = new OCSPResp(responseBytes);
            if (response.getStatus() != OCSPRespBuilder.SUCCESSFUL || !(response.getResponseObject() instanceof BasicOCSPResp basic)) {
                throw new GeneralSecurityException("OCSP 응답이 성공 상태가 아닙니다.");
            }
            var digest = new JcaDigestCalculatorProviderBuilder().setProvider(PROVIDER).build();
            var issuerHolder = new JcaX509CertificateHolder(issuer);
            RespID keyResponder = new RespID(SubjectPublicKeyInfo.getInstance(issuer.getPublicKey()
                                                                                    .getEncoded()), digest.get(CertificateID.HASH_SHA1));
            if ((!basic.getResponderId().equals(keyResponder) && !basic.getResponderId()
                                                                       .equals(new RespID(issuerHolder.getSubject())))
                || request.getRequestList().length != 1 || basic.getResponses().length != 1
                || !request.getRequestList()[0].getCertID().equals(basic.getResponses()[0].getCertID())
                || !request.getRequestList()[0].getCertID().matchesIssuer(issuerHolder, digest)
                || !basic.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(PROVIDER)
                                                                                  .build(issuer.getPublicKey()))) {
                throw new GeneralSecurityException("OCSP 대상 인증서 또는 응답 서명이 일치하지 않습니다.");
            }
            Extension expected = request.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
            Extension actual = basic.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
            if (expected == null || actual == null || !Arrays.equals(expected.getExtnValue()
                                                                             .getOctets(), actual.getExtnValue()
                                                                                                 .getOctets())) {
                throw new GeneralSecurityException("OCSP nonce가 일치하지 않습니다.");
            }
            var single = basic.getResponses()[0];
            if (!basic.getCriticalExtensionOIDs().isEmpty() || !single.getCriticalExtensionOIDs().isEmpty()) {
                throw new GeneralSecurityException("지원하지 않는 OCSP 필수 확장이 있습니다.");
            }
            Instant now = Instant.now();
            if (single.getThisUpdate().toInstant().isAfter(now.plusSeconds(300)) || single.getNextUpdate() == null
                || single.getNextUpdate().before(single.getThisUpdate()) || single.getNextUpdate()
                                                                                  .toInstant()
                                                                                  .isBefore(now)
                || basic.getProducedAt().toInstant().isAfter(now.plusSeconds(300))) {
                throw new GeneralSecurityException("OCSP 응답의 유효기간을 확인할 수 없습니다.");
            }
            var status = single.getCertStatus();
            String value = status == null ? "GOOD" : status instanceof RevokedStatus ? "REVOKED" : "UNKNOWN";
            Instant revokedAt = status instanceof RevokedStatus revoked
                                ? revoked.getRevocationTime().toInstant()
                                : null;
            return new OcspCheckResult(value, revokedAt, single.getThisUpdate().toInstant(), single.getNextUpdate()
                                                                                                   .toInstant(),
                basic.getProducedAt().toInstant(), true);
        } catch (OCSPException | OperatorCreationException e) {
            throw new GeneralSecurityException("OCSP 응답을 검증하지 못했습니다.", e);
        }
    }

    private MyCertificate find(CertificateId id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("인증서를 찾을 수 없습니다."));
    }

    private X509Certificate signingCertificate(MyCertificate issuer) {
        X509Certificate ca = x509(issuer);
        if (issuer.getStatus() != CertificateStatus.ACTIVE || issuer.getRevokedAt() != null
            || ca.getBasicConstraints() < 0 || issuer.getSubjectKeyId() == null
            || ca.getNotBefore().toInstant().isAfter(Instant.now()) || !ca.getNotAfter()
                                                                          .toInstant()
                                                                          .isAfter(Instant.now())) {
            throw new IllegalArgumentException("현재 유효한 CA 인증서와 KMS 서명 키가 필요합니다.");
        }
        return ca;
    }

    private List<MyCertificate> issuedBy(MyCertificate issuer) {
        return repository.findAll()
                         .stream()
                         .filter(item -> issuer.getId().equals(item.getIssuerCertificateId()))
                         .toList();
    }
}
