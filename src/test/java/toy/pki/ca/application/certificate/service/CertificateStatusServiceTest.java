package toy.pki.ca.application.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.cert.ocsp.jcajce.JcaBasicOCSPRespBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateStatusService;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.SubjectKeySpec;

class CertificateStatusServiceTest extends CertificateServiceTestSupport {

    private final BouncyCastleCertificateStatusService status = new BouncyCastleCertificateStatusService(certificates, kms);

    @Test
    void crlIncludesOnlyDirectlyIssuedRevokedAndHeldCertificates() throws Exception {
        CertificateId ca = root();
        CertificateId revoked = leaf(ca);
        CertificateId held = leaf(ca);
        leaf(ca);
        CertificateId unrelated = leaf(root());
        service.revoke(revoked.id());
        service.findById(held.id()).suspend();
        service.revoke(unrelated.id());

        var crl = status.crl(ca);
        crl.verify(certificate(ca).getPublicKey());
        assertThat(crl.getRevokedCertificates()).extracting(entry -> entry.getSerialNumber())
            .containsExactlyInAnyOrder(certificate(revoked).getSerialNumber(), certificate(held).getSerialNumber());
        assertThat(crl.getRevokedCertificate(certificate(held).getSerialNumber()).getRevocationReason())
            .isEqualTo(java.security.cert.CRLReason.CERTIFICATE_HOLD);
        assertThat(crl.getRevokedCertificate(certificate(revoked).getSerialNumber()).getRevocationDate().toInstant())
            .isEqualTo(service.findById(revoked.id()).getRevokedAt().truncatedTo(ChronoUnit.SECONDS));
        assertThat(crl.getNextUpdate()).isAfter(crl.getThisUpdate()).isBeforeOrEqualTo(certificate(ca).getNotAfter());
        assertThat(crl.getExtensionValue(Extension.authorityKeyIdentifier.getId())).isNotNull();
        BigInteger number = CRLNumber.getInstance(ASN1OctetString.getInstance(
            crl.getExtensionValue(Extension.cRLNumber.getId())).getOctets()).getCRLNumber();
        BigInteger next = CRLNumber.getInstance(ASN1OctetString.getInstance(
            status.crl(ca).getExtensionValue(Extension.cRLNumber.getId())).getOctets()).getCRLNumber();
        assertThat(next).isGreaterThan(number);
    }

    @Test
    void rejectsCrlWithoutCrlSignUsageAndRejectsRevokedIssuers() throws Exception {
        var profile = profile(CertificateType.ROOT_CA, SubjectKeySpec.EC_P256,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA256, 365, null);
        CertificateId noCrl = service.issue(request(profile, key(SubjectKeySpec.EC_P256), null, null, Set.of()));
        assertThatThrownBy(() -> status.crl(noCrl)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CRL_SIGN");
        assertThat(status.check(noCrl).status()).isEqualTo("GOOD");

        CertificateId ca = root();
        CertificateId target = leaf(ca);
        service.revoke(ca.id());
        assertThatThrownBy(() -> status.crl(ca)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> status.check(target)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> status.request(new CertificateId("missing"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ocspReportsRevocationHoldUnknownAndExpirySeparately() throws Exception {
        CertificateId ca = root();
        CertificateId target = leaf(ca);
        var checked = status.check(target);
        assertThat(checked.status()).isEqualTo("GOOD");
        assertThat(checked.signatureValid()).isTrue();
        assertThat(checked.nextUpdate()).isAfter(checked.thisUpdate());
        service.expire(target.id());
        assertThat(status.check(target).status()).isEqualTo("GOOD");
        service.findById(target.id()).setStatus(CertificateStatus.UNKNOWN);
        assertThat(status.check(target).status()).isEqualTo("UNKNOWN");
        service.findById(target.id()).suspend();
        assertThat(status.check(target).status()).isEqualTo("REVOKED");
        service.revoke(target.id());
        checked = status.check(target);
        assertThat(checked.status()).isEqualTo("REVOKED");
        assertThat(checked.revokedAt()).isEqualTo(service.findById(target.id()).getRevokedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void rejectsMalformedRequestsAndWrongIssuersAndReportsUnknownSerials() throws Exception {
        CertificateId ca = root();
        CertificateId other = root();
        for (byte[] malformed : List.of(new byte[] {1, 2, 3}, new byte[8193])) {
            assertThat(new OCSPResp(status.respond(ca, malformed)).getStatus()).isEqualTo(OCSPRespBuilder.MALFORMED_REQUEST);
        }
        assertThat(new OCSPResp(status.respond(other, status.request(ca))).getStatus()).isEqualTo(OCSPRespBuilder.UNAUTHORIZED);
        var digest = new JcaDigestCalculatorProviderBuilder().build();
        var id = new CertificateID(digest.get(CertificateID.HASH_SHA1), new JcaX509CertificateHolder(certificate(ca)), BigInteger.ONE);
        byte[] request = new OCSPReqBuilder().addRequest(id).build().getEncoded();
        var response = (BasicOCSPResp) new OCSPResp(status.respond(ca, request)).getResponseObject();
        assertThat(response.getResponses()[0].getCertStatus()).isInstanceOf(UnknownStatus.class);
    }

    @Test
    void rejectsResponsesWithDifferentNonceTargetOrSigner() throws Exception {
        CertificateId ca = root();
        CertificateId target = leaf(ca);
        CertificateId other = root();
        byte[] request = status.request(target);
        byte[] response = status.respond(ca, request);
        assertThat(status.verify(request, response, certificate(ca)).status()).isEqualTo("GOOD");
        assertThatThrownBy(() -> status.verify(status.request(target), response, certificate(ca)))
            .isInstanceOf(GeneralSecurityException.class).hasMessageContaining("nonce");
        assertThatThrownBy(() -> status.verify(status.request(ca), response, certificate(ca)))
            .isInstanceOf(GeneralSecurityException.class);
        assertThatThrownBy(() -> status.verify(request, response, certificate(other)))
            .isInstanceOf(GeneralSecurityException.class);
    }

    @Test
    void rejectsExpiredResponsesEvenWithValidSignatureAndNonce() throws Exception {
        CertificateId ca = root();
        byte[] request = status.request(ca);
        var parsed = new OCSPReq(request);
        var digest = new JcaDigestCalculatorProviderBuilder().build();
        var builder = new JcaBasicOCSPRespBuilder(certificate(ca).getPublicKey(), digest.get(CertificateID.HASH_SHA1));
        Instant old = Instant.now().minusSeconds(3600);
        builder.addResponse(parsed.getRequestList()[0].getCertID(), null, Date.from(old), Date.from(old.plusSeconds(300)), null);
        builder.setResponseExtensions(new Extensions(parsed.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce)));
        var signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keys.get(service.findById(ca.id()).getSubjectKeyId()).getPrivate());
        byte[] response = new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL,
            builder.build(signer, null, Date.from(old))).getEncoded();
        assertThatThrownBy(() -> status.verify(request, response, certificate(ca)))
            .isInstanceOf(GeneralSecurityException.class).hasMessageContaining("유효기간");
    }

    @Test
    void failsWhenKmsUsesWrongKeyOrIsUnavailable() throws Exception {
        CertificateId ca = root();
        var wrongKey = keys.get(key(SubjectKeySpec.EC_P256));
        doAnswer(call -> {
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(wrongKey.getPrivate());
            signer.update((byte[]) call.getArgument(3));
            return signer.sign();
        }).when(kms).sign(any(), any(), any(), any());
        assertThatThrownBy(() -> status.crl(ca)).isInstanceOf(GeneralSecurityException.class);
        assertThatThrownBy(() -> status.check(ca)).isInstanceOf(GeneralSecurityException.class);

        var failure = new ResourceAccessException("KMS offline");
        doThrow(failure).when(kms).sign(any(), any(), any(), any());
        assertThatThrownBy(() -> status.crl(ca)).isSameAs(failure);
        assertThatThrownBy(() -> status.check(ca)).isSameAs(failure);
    }

    private CertificateId root() throws Exception {
        var profile = profile(CertificateType.ROOT_CA, SubjectKeySpec.EC_P256,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA256, 365, null);
        profile.setKeyUsages(Set.of(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN));
        return service.issue(request(profile, key(SubjectKeySpec.EC_P256), null, null, Set.of()));
    }

    private CertificateId leaf(CertificateId issuer) throws Exception {
        var profile = profile(CertificateType.END_ENTITY, SubjectKeySpec.EC_P256,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA256, 30, null);
        return service.issue(request(profile, key(SubjectKeySpec.EC_P256), issuer, null, Set.of()));
    }
}
