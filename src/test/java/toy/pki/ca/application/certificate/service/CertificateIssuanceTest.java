package toy.pki.ca.application.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;

import java.security.KeyPair;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateStatusService;
import toy.pki.ca.application.certificate.model.IssueCertificateCommand;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.certificate.SubjectAlternativeName;
import toy.pki.ca.domain.policy.SanPolicy;
import toy.pki.ca.domain.policy.SubjectKeyPolicy;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.ExtendedKeyUsageOid;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.domain.profile.SanType;
import toy.pki.ca.domain.profile.SubjectKeySpec;
import toy.pki.kms.domain.HashAlgorithm;
import toy.pki.kms.domain.KeyId;

class CertificateIssuanceTest extends CertificateServiceTestSupport {

    @ParameterizedTest
    @EnumSource(value = CertificateSignatureAlgorithm.class, names = "UNKNOWN", mode = EnumSource.Mode.EXCLUDE)
    void signsCrlAndOcspWithTheCaPublicKeyAlgorithm(CertificateSignatureAlgorithm algorithm) throws Exception {
        SubjectKeySpec spec = algorithm.name().startsWith("RSA_") ? SubjectKeySpec.RSA_2048
            : algorithm.name().startsWith("ECDSA_") ? SubjectKeySpec.EC_P384 : SubjectKeySpec.valueOf(algorithm.name());
        KeyId key = key(spec);
        CertificateProfile profile = profile(CertificateType.ROOT_CA, spec, algorithm, 30, 1);
        profile.setKeyUsages(Set.of(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN));
        CertificateId id = service.issue(request(profile, key, null, null, Set.of()));
        var status = new BouncyCastleCertificateStatusService(certificates, kms);
        var crl = status.crl(id);
        crl.verify(keys.get(key).getPublic());
        assertThat(crl.getRevokedCertificates()).isNull();
        assertThat(status.check(id).signatureValid()).isTrue();
        assertThat(status.check(id).status()).isEqualTo("GOOD");
    }

    @ParameterizedTest
    @EnumSource(value = CertificateSignatureAlgorithm.class, names = "UNKNOWN", mode = EnumSource.Mode.EXCLUDE)
    void issuesAndVerifiesRootForEverySupportedSignature(CertificateSignatureAlgorithm algorithm) throws Exception {
        SubjectKeySpec spec = algorithm.name().startsWith("RSA_") ? SubjectKeySpec.RSA_2048
            : algorithm.name().startsWith("ECDSA_") ? SubjectKeySpec.EC_P256
            : SubjectKeySpec.valueOf(algorithm.name());
        KeyId key = key(spec);
        CertificateProfile profile = profile(CertificateType.ROOT_CA, spec, algorithm, 30, 1);
        CertificateId id = service.issue(request(profile, key, null, null, Set.of()));
        X509Certificate certificate = certificate(id);

        certificate.verify(keys.get(key).getPublic());
        certificate.checkValidity();
        assertThat(certificate.getVersion()).isEqualTo(3);
        assertThat(certificate.getBasicConstraints()).isEqualTo(1);
        assertThat(certificate.getKeyUsage()[5]).isTrue();
        assertThat(certificate.getKeyUsage()[6]).isFalse(); // CRL_SIGN is optional for a CA.
        assertThat(certificate.getSerialNumber().signum()).isPositive();
        assertThat(certificate.getSerialNumber().toByteArray().length).isLessThanOrEqualTo(20);
        assertThat(certificate.getSubjectX500Principal()).isEqualTo(certificate.getIssuerX500Principal());
        assertThat(certificate.getSubjectX500Principal().getName()).contains("O=Fixed Organization", "CN=Test Certificate", "C=KR");
        assertThat(certificate.getNotBefore().toInstant()).isEqualTo(start);
        assertThat(certificate.getNotAfter().toInstant()).isEqualTo(start.plus(30, ChronoUnit.DAYS));
        assertThat(certificate.getPublicKey().getEncoded()).isEqualTo(keys.get(key).getPublic().getEncoded());
        assertThat(authorityKeyId(certificate)).isEqualTo(subjectKeyId(certificate));
        assertThat(service.findById(id.id()).getSubjectKeyId()).isEqualTo(key);
        assertThat(service.findById(id.id()).getIssuerCertificateId()).isNull();
    }

    @Test
    void issuesMixedAlgorithmChainWithProfileExtensions() throws Exception {
        KeyId rootKey = key(SubjectKeySpec.EC_P256);
        CertificateProfile root = profile(CertificateType.ROOT_CA, SubjectKeySpec.EC_P256,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA256, 365, 1);
        CertificateId rootId = service.issue(request(root, rootKey, null, null, Set.of()));
        KeyId subKey = key(SubjectKeySpec.RSA_2048);
        CertificateProfile sub = profile(CertificateType.INTERMEDIATE_CA, SubjectKeySpec.RSA_2048,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA384, 180, null);
        CertificateId subId = service.issue(request(sub, subKey, rootId, null, Set.of()));
        assertThat(certificate(subId).getBasicConstraints()).isZero();
        certificate(subId).verify(keys.get(rootKey).getPublic());

        KeyId leafKey = key(SubjectKeySpec.EC_P384);
        CertificateProfile leaf = profile(CertificateType.END_ENTITY, SubjectKeySpec.EC_P384,
            CertificateSignatureAlgorithm.RSA_WITH_SHA512, 30, null);
        leaf.setExtendedKeyUsages(Set.of(new ExtendedKeyUsageOid("1.3.6.1.5.5.7.3.1")));
        leaf.setSanPolicy(new SanPolicy(true, Set.of(SanType.values())));
        Set<SubjectAlternativeName> sans = Set.of(
            new SubjectAlternativeName(SanType.DNS_NAME, "service.example.com"),
            new SubjectAlternativeName(SanType.IP_ADDRESS, "127.0.0.1"),
            new SubjectAlternativeName(SanType.EMAIL_ADDRESS, "test@example.com"),
            new SubjectAlternativeName(SanType.URI, "https://example.com/service"));
        CertificateId leafId = service.issue(request(leaf, leafKey, subId, 7, sans));
        X509Certificate certificate = certificate(leafId);
        certificate.verify(keys.get(subKey).getPublic());
        assertThat(certificate.getSigAlgName()).isEqualToIgnoringCase("SHA512withRSA");
        assertThat(certificate.getBasicConstraints()).isEqualTo(-1);
        assertThat(certificate.getKeyUsage()[0]).isTrue();
        assertThat(certificate.getExtendedKeyUsage()).containsExactly("1.3.6.1.5.5.7.3.1");
        assertThat(certificate.getSubjectAlternativeNames()).hasSize(4);
        assertThat(certificate.getNotAfter().toInstant()).isEqualTo(start.plus(7, ChronoUnit.DAYS));
        assertThat(authorityKeyId(certificate)).isEqualTo(subjectKeyId(certificate(subId)));
        assertThat(service.findById(leafId.id()).getIssuerCertificateId()).isEqualTo(subId);
    }

    @Test
    void rejectsInvalidProfileInputsBeforeSigningOrSaving() throws Exception {
        KeyId key = key(SubjectKeySpec.EC_P256);
        CertificateProfile profile = profile(CertificateType.ROOT_CA, SubjectKeySpec.EC_P256,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA256, 30, null);
        profile.setStatus(ProfileStatus.DRAFT);
        assertRejected(request(profile, key, null, null, Set.of()), "active");
        profile.setStatus(ProfileStatus.ACTIVE);
        assertRejected(request(profile, key, null, 31, Set.of()), "Validity");
        assertRejected(request(profile, key, null, 0, Set.of()), "Validity");
        profile.setSanPolicy(new SanPolicy(true, Set.of(SanType.DNS_NAME)));
        assertRejected(request(profile, key, null, null, Set.of()), "required");
        assertRejected(request(profile, key, null, null,
            Set.of(new SubjectAlternativeName(SanType.IP_ADDRESS, "127.0.0.1"))), "not allowed");
        profile.setSanPolicy(new SanPolicy(false, Set.of()));
        profile.setSubjectKeyPolicy(new SubjectKeyPolicy(Set.of(SubjectKeySpec.RSA_2048)));
        assertRejected(request(profile, key, null, null, Set.of()), "subject key");
        profile.setSubjectKeyPolicy(new SubjectKeyPolicy(Set.of(SubjectKeySpec.EC_P256)));
        profile.setAllowedSignatures(Set.of(CertificateSignatureAlgorithm.RSA_WITH_SHA256));
        assertRejected(request(profile, key, null, null, Set.of()), "signature algorithm");
        profile.setAllowedSignatures(Set.of(CertificateSignatureAlgorithm.ECDSA_WITH_SHA256));
        IssueCertificateCommand missingDn = new IssueCertificateCommand(null, null, profile.getId(), key,
            null, null, Set.of(), null, start, null);
        assertRejected(missingDn, "Required subject attribute");
        assertThat(signatures).hasValue(0);
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void enforcesIssuerStatusValidityAndPathLength() throws Exception {
        KeyId key = key(SubjectKeySpec.EC_P256);
        CertificateProfile root = profile(CertificateType.ROOT_CA, SubjectKeySpec.EC_P256,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA256, 30, 0);
        CertificateId rootId = service.issue(request(root, key, null, null, Set.of()));
        CertificateProfile sub = profile(CertificateType.INTERMEDIATE_CA, SubjectKeySpec.EC_P256,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA256, 10, null);
        assertRejected(request(sub, key, rootId, null, Set.of()), "path length");
        CertificateProfile leaf = profile(CertificateType.END_ENTITY, SubjectKeySpec.EC_P256,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA256, 60, null);
        assertRejected(request(leaf, key, null, 10, Set.of()), "issuer certificate");
        assertRejected(request(leaf, key, rootId, 31, Set.of()), "issuer validity");
        service.revoke(rootId.id());
        assertRejected(request(leaf, key, rootId, 10, Set.of()), "active issuer");
        assertThat(signatures).hasValue(1);
        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void rejectsWrongKmsSigningKeyWithoutSavingCertificate() throws Exception {
        KeyId key = key(SubjectKeySpec.EC_P256);
        CertificateProfile profile = profile(CertificateType.ROOT_CA, SubjectKeySpec.EC_P256,
            CertificateSignatureAlgorithm.ECDSA_WITH_SHA256, 30, null);
        KeyPair wrong = keys.get(key(SubjectKeySpec.EC_P256));
        doAnswer(call -> {
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(wrong.getPrivate());
            signer.update((byte[]) call.getArgument(3));
            return signer.sign();
        }).when(kms).sign(any(), any(), nullable(HashAlgorithm.class), any());
        assertThatThrownBy(() -> service.issue(request(profile, key, null, null, Set.of())))
            .isInstanceOf(java.security.SignatureException.class);
        assertThat(service.findAll()).isEmpty();
    }

    private byte[] subjectKeyId(X509Certificate certificate) {
        return SubjectKeyIdentifier.getInstance(ASN1OctetString.getInstance(
            certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId())).getOctets()).getKeyIdentifier();
    }

    private byte[] authorityKeyId(X509Certificate certificate) {
        return AuthorityKeyIdentifier.getInstance(ASN1OctetString.getInstance(
            certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId())).getOctets()).getKeyIdentifier();
    }

    private void assertRejected(IssueCertificateCommand request, String reason) {
        assertThatThrownBy(() -> service.issue(request)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(reason);
    }
}
