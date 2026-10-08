package toy.pki.ca.application.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.EdECPrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateIssuer;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateStatusService;
import toy.pki.ca.adapter.persistence.memory.InMemoryMyCertificateRepository;
import toy.pki.ca.application.certificate.model.CertificateValidationResult.Reason;
import toy.pki.ca.application.certificate.model.IssueCertificateCommand;
import toy.pki.ca.application.profile.service.CertificateProfileService;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.certificate.MyCertificate;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.domain.KeyId;

class CertificateValidationServiceTest {

    private final InMemoryMyCertificateRepository repository = new InMemoryMyCertificateRepository();
    private final CertificateValidationService validation = new CertificateValidationService(repository);
    private final Map<CertificateId, KeyPair> keys = new HashMap<>();
    private final AtomicLong serials = new AtomicLong();
    private final Instant now = Instant.now();
    private List<MyCertificate> chain;

    @BeforeEach
    void createChain() throws Exception {
        MyCertificate root = create("Root", null, 2);
        MyCertificate upper = create("Intermediate 1", root, 1);
        MyCertificate lower = create("Intermediate 2", upper, 0);
        MyCertificate leaf = create("Leaf", lower, -1);
        chain = List.of(leaf, lower, upper, root);
    }

    @Test
    void validatesEveryLinkAndTheManagedRoot() {
        var result = validation.validate(chain.getFirst().getId());
        assertThat(result.isValid()).isTrue();
        assertThat(result.reason()).isEqualTo(Reason.VALID);
        assertThat(result.chain()).containsExactlyElementsOf(chain.stream().map(MyCertificate::getId).toList());
        assertThat(result.failedCertificateId()).isNull();
        assertThat(validation.validate(chain.getLast().getId()).isValid()).isTrue();
        assertThat(validation.validate(chain.get(1).getId()).isValid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"RSA", "secp256r1", "secp384r1", "secp521r1", "Ed25519", "Ed448"})
    void validatesMixedKeyChainsWithSupportedIssuerAlgorithms(String algorithm) throws Exception {
        MyCertificate root = create("Mixed root", null, 1, keyPair(algorithm));
        MyCertificate intermediate = create("Mixed intermediate", root, 0, keyPair("RSA"));
        MyCertificate leaf = create("Mixed leaf", intermediate, -1);
        assertThat(validation.validate(leaf.getId()).isValid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void revocationAtAnyDepthInvalidatesTheChainWithoutRevokingDescendants(int index) {
        MyCertificate revoked = chain.get(index);
        revoked.revoke();

        var result = validation.validate(chain.getFirst().getId());
        assertThat(result.reason()).isEqualTo(Reason.REVOKED);
        assertThat(result.failedCertificateId()).isEqualTo(revoked.getId());
        assertThat(result.message()).contains(revoked.getAlias());
        for (int i = 0; i < index; i++) {
            assertThat(chain.get(i).getStatus()).isEqualTo(CertificateStatus.ACTIVE);
            assertThat(chain.get(i).getRevokedAt()).isNull();
        }
    }

    @ParameterizedTest
    @EnumSource(value = CertificateStatus.class, names = {"SUSPENDED", "UNKNOWN", "EXPIRED"})
    void rejectsNonActiveAncestorStates(CertificateStatus status) {
        chain.get(2).setStatus(status);
        var result = validation.validate(chain.getFirst().getId());
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedCertificateId()).isEqualTo(chain.get(2).getId());
    }

    @Test
    void revocationTimestampCannotBeBypassedByChangingTheStatusToActive() {
        chain.get(2).revoke();
        chain.get(2).setStatus(CertificateStatus.ACTIVE);
        assertThat(validation.validate(chain.getFirst().getId()).reason()).isEqualTo(Reason.REVOKED);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void rejectsExpiredCertificatesIncludingTheRoot(int index) throws Exception {
        replace(chain.get(index), now.minusSeconds(300), now.minusSeconds(60), null, false, null);
        var result = validation.validate(chain.getFirst().getId());
        assertThat(result.reason()).isEqualTo(Reason.EXPIRED);
        assertThat(result.failedCertificateId()).isEqualTo(chain.get(index).getId());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 3})
    void rejectsFutureValidityAtAnyDepth(int index) throws Exception {
        replace(chain.get(index), now.plusSeconds(3600), now.plusSeconds(7200), null, false, null);
        assertThat(validation.validate(chain.getFirst().getId()).reason()).isEqualTo(Reason.NOT_YET_VALID);
    }

    @Test
    void rejectsMissingIssuerInsteadOfTreatingAnIntermediateAsTrusted() {
        repository.delete(chain.get(2).getId());
        var result = validation.validate(chain.getFirst().getId());
        assertThat(result.reason()).isEqualTo(Reason.MISSING_ISSUER);
        assertThat(result.failedCertificateId()).isEqualTo(chain.get(2).getId());
    }

    @Test
    void rejectsMissingParentLinkAndUnknownTarget() {
        chain.get(1).setIssuerCertificateId(null);
        assertThat(validation.validate(chain.getFirst().getId()).reason()).isEqualTo(Reason.MISSING_ISSUER);
        assertThat(validation.validate(new CertificateId("missing")).reason()).isEqualTo(Reason.NOT_FOUND);
    }

    @Test
    void rejectsCycles() {
        // A self-issued link passes name and signature checks but must not loop forever.
        chain.getLast().setIssuerCertificateId(chain.getLast().getId());
        assertThat(validation.validate(chain.getFirst().getId()).reason()).isEqualTo(Reason.CHAIN_CYCLE);
    }

    @Test
    void rejectsWrongSigningKeyAndInvalidRootSelfSignature() throws Exception {
        replace(chain.getFirst(), null, null, null, false, keyPair());
        assertThat(validation.validate(chain.getFirst().getId()).reason()).isEqualTo(Reason.INVALID_SIGNATURE);
        replace(chain.getLast(), null, null, null, false, keyPair());
        assertThat(validation.validate(chain.getLast().getId()).reason()).isEqualTo(Reason.INVALID_SIGNATURE);
    }

    @Test
    void rejectsIssuerDnMismatch() {
        chain.getFirst().setIssuerCertificateId(chain.getLast().getId());
        assertThat(validation.validate(chain.getFirst().getId()).reason()).isEqualTo(Reason.INVALID_CHAIN);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void enforcesPathLengthForIntermediateAndRootEvenWhenTargetIsACa(int index) throws Exception {
        replace(chain.get(index), null, null, 0, false, null);
        assertThat(validation.validate(chain.getFirst().getId()).reason()).isEqualTo(Reason.INVALID_CA);
        assertThat(validation.validate(chain.get(1).getId()).reason()).isEqualTo(Reason.INVALID_CA);
    }

    @Test
    void rejectsNonCaIssuer() throws Exception {
        replace(chain.get(2), null, null, -1, false, null);
        assertThat(validation.validate(chain.getFirst().getId()).reason()).isEqualTo(Reason.INVALID_CA);
    }

    @Test
    void pkixRejectsUnknownCriticalExtensionInAnOtherwiseValidChain() throws Exception {
        replace(chain.getFirst(), null, null, null, true, null);
        assertThat(validation.validate(chain.getFirst().getId()).reason()).isEqualTo(Reason.INVALID_CHAIN);
    }

    @Test
    void revokedAncestorBlocksIssuanceBeforeCallingTheIssuer() throws Exception {
        chain.get(2).revoke();
        var profiles = mock(CertificateProfileService.class);
        var issuer = mock(BouncyCastleCertificateIssuer.class);
        var profile = mock(CertificateProfile.class);
        when(profile.getCertificateType()).thenReturn(CertificateType.END_ENTITY);
        when(profiles.findById(any())).thenReturn(profile);
        var service = new MyCertificateService(repository, profiles, issuer, validation);
        var request = new IssueCertificateCommand("new leaf", null, new ProfileId("leaf"), new KeyId("key"),
            chain.get(1).getId(), null, Set.of(), null, null, null);

        assertThatThrownBy(() -> service.issue(request)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Intermediate 1");
        verifyNoInteractions(issuer);
        assertThat(repository.findAll()).hasSize(4);
    }

    @Test
    void revokedAncestorBlocksCrlAndOcspSigningBeforeCallingKms() throws Exception {
        chain.get(2).revoke();
        var kms = mock(KeyManagementService.class);
        var status = new BouncyCastleCertificateStatusService(repository, kms, validation);

        assertThatThrownBy(() -> status.crl(chain.get(1).getId())).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Intermediate 1");
        assertThatThrownBy(() -> status.respond(chain.get(1).getId(), status.request(chain.getFirst().getId())))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Intermediate 1");
        verifyNoInteractions(kms);
    }

    private MyCertificate create(String alias, MyCertificate parent, int pathLen) throws Exception {
        return create(alias, parent, pathLen, keyPair());
    }

    private MyCertificate create(String alias, MyCertificate parent, int pathLen, KeyPair pair) throws Exception {
        CertificateId id = new CertificateId(UUID.randomUUID().toString());
        keys.put(id, pair);
        X500Name subject = new X500Name("CN=" + alias);
        X500Name issuer = parent == null ? subject
                           : X500Name.getInstance(x509(parent).getSubjectX500Principal().getEncoded());
        var certificate = build(subject, issuer, pair, parent == null ? pair : keys.get(parent.getId()), pathLen,
            now.minusSeconds(60), now.plusSeconds(86400), false);
        var managed = new MyCertificate(id, new ProfileId("profile"), certificate);
        managed.setAlias(alias);
        managed.setSubjectKeyId(new KeyId(id.id()));
        managed.setIssuerCertificateId(parent == null ? null : parent.getId());
        repository.save(managed);
        return managed;
    }

    private void replace(MyCertificate target, Instant start, Instant end, Integer pathLen,
        boolean unknownCritical, KeyPair wrongSigner) throws Exception {
        X509Certificate old = x509(target);
        CertificateId signingId = target.getIssuerCertificateId() == null ? target.getId() : target.getIssuerCertificateId();
        var replacement = build(X500Name.getInstance(old.getSubjectX500Principal().getEncoded()),
            X500Name.getInstance(old.getIssuerX500Principal().getEncoded()), keys.get(target.getId()),
            wrongSigner == null ? keys.get(signingId) : wrongSigner,
            pathLen == null ? old.getBasicConstraints() : pathLen,
            start == null ? old.getNotBefore().toInstant() : start,
            end == null ? old.getNotAfter().toInstant() : end, unknownCritical);
        var updated = new MyCertificate(target.getId(), target.getProfileId(), replacement);
        updated.setAlias(target.getAlias());
        updated.setSubjectKeyId(target.getSubjectKeyId());
        updated.setIssuerCertificateId(target.getIssuerCertificateId());
        updated.setStatus(target.getStatus());
        updated.setRevokedAt(target.getRevokedAt());
        repository.save(updated);
    }

    private X509Certificate build(X500Name subject, X500Name issuer, KeyPair key, KeyPair signer,
        int pathLen, Instant start, Instant end, boolean unknownCritical) throws Exception {
        var builder = new JcaX509v3CertificateBuilder(issuer, BigInteger.valueOf(serials.incrementAndGet()),
            Date.from(start), Date.from(end), subject, key.getPublic());
        builder.addExtension(Extension.basicConstraints, true,
            pathLen < 0 ? new BasicConstraints(false) : new BasicConstraints(pathLen));
        builder.addExtension(Extension.keyUsage, true,
            new KeyUsage(pathLen < 0 ? KeyUsage.digitalSignature : KeyUsage.keyCertSign | KeyUsage.cRLSign));
        if (unknownCritical) {
            builder.addExtension(new ASN1ObjectIdentifier("1.2.3.4.99"), true, new DERUTF8String("unsupported"));
        }
        return new JcaX509CertificateConverter().getCertificate(builder.build(
            new JcaContentSignerBuilder(signatureAlgorithm(signer)).build(signer.getPrivate())));
    }

    private static X509Certificate x509(MyCertificate certificate) {
        return (X509Certificate) certificate.getCertificate();
    }

    private static KeyPair keyPair() throws Exception {
        return keyPair("secp256r1");
    }

    private static KeyPair keyPair(String algorithm) throws Exception {
        var generator = KeyPairGenerator.getInstance(algorithm.startsWith("secp") ? "EC" : algorithm);
        if (algorithm.startsWith("secp")) {
            generator.initialize(new ECGenParameterSpec(algorithm));
        } else if (algorithm.equals("RSA")) {
            generator.initialize(2048);
        }
        return generator.generateKeyPair();
    }

    private static String signatureAlgorithm(KeyPair signer) {
        if (signer.getPrivate() instanceof EdECPrivateKey ed) {
            return ed.getParams().getName();
        }
        return signer.getPrivate().getAlgorithm().equals("RSA") ? "SHA256withRSA" : "SHA256withECDSA";
    }
}
