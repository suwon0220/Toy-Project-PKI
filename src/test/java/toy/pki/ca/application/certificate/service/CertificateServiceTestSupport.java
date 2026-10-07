package toy.pki.ca.application.certificate.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateIssuer;
import toy.pki.ca.adapter.persistence.memory.InMemoryMyCertificateRepository;
import toy.pki.ca.application.certificate.model.IssueCertificateCommand;
import toy.pki.ca.application.profile.service.CertificateProfileService;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.certificate.CertificateSubject;
import toy.pki.ca.domain.certificate.SubjectAlternativeName;
import toy.pki.ca.domain.policy.DnAttributePolicy;
import toy.pki.ca.domain.policy.DnPolicy;
import toy.pki.ca.domain.policy.SanPolicy;
import toy.pki.ca.domain.policy.SubjectKeyPolicy;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.SubjectKeySpec;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.domain.HashAlgorithm;
import toy.pki.kms.domain.KeyId;
import toy.pki.kms.domain.SignatureAlgorithm;

abstract class CertificateServiceTestSupport {

    private static ValidatorFactory validators;
    final Instant start = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.SECONDS);
    final Map<KeyId, KeyPair> keys = new HashMap<>();
    final Map<String, CertificateProfile> profiles = new HashMap<>();
    final KeyManagementService kms = mock(KeyManagementService.class);
    final CertificateProfileService profileService = mock(CertificateProfileService.class);
    final AtomicInteger signatures = new AtomicInteger();
    final InMemoryMyCertificateRepository certificates = new InMemoryMyCertificateRepository();
    MyCertificateService service;

    @BeforeAll
    static void openValidator() {
        validators = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validators.close();
    }

    @BeforeEach
    void setUp() {
        when(profileService.findById(any())).thenAnswer(call -> profiles.get(call.getArgument(0)));
        when(kms.getPublicKeyOf(any())).thenAnswer(call -> keys.get(call.getArgument(0)).getPublic().getEncoded());
        // Only this test double owns private keys. Production sends the same TBS bytes to remote KMS.
        doAnswer(call -> {
            KeyId id = call.getArgument(0);
            SignatureAlgorithm algorithm = call.getArgument(1);
            HashAlgorithm digest = call.getArgument(2);
            String jca = switch (algorithm) {
                case RSA_PKCS1_V1_5 -> digest.name() + "withRSA";
                case ECDSA -> digest.name() + "withECDSA";
                case Ed25519, Ed448 -> algorithm.name();
                default -> throw new AssertionError("Unsupported algorithm");
            };
            Signature signature = Signature.getInstance(jca);
            signature.initSign(keys.get(id).getPrivate());
            signature.update((byte[]) call.getArgument(3));
            signatures.incrementAndGet();
            return signature.sign();
        }).when(kms).sign(any(), any(), nullable(HashAlgorithm.class), any());
        service = new MyCertificateService(certificates, profileService,
            new BouncyCastleCertificateIssuer(kms, validators.getValidator()));
    }

    CertificateProfile profile(CertificateType type, SubjectKeySpec key, CertificateSignatureAlgorithm algorithm,
        int days, Integer pathLength) {
        CertificateProfile profile = new CertificateProfile(new ProfileId(UUID.randomUUID().toString()),
            "Test profile", null, days, days, new SubjectKeyPolicy(Set.of(key)), Set.of(algorithm),
            new SanPolicy(false, Set.of()),
            new DnPolicy(null, new DnAttributePolicy(true), null,
                new DnAttributePolicy("Fixed Organization"), new DnAttributePolicy("KR")),
            type, pathLength, Set.of(type == CertificateType.END_ENTITY ? KeyUsage.DIGITAL_SIGNATURE : KeyUsage.KEY_CERT_SIGN), Set.of());
        profile.activate();
        profiles.put(profile.getId().value(), profile);
        return profile;
    }

    KeyId key(SubjectKeySpec spec) throws Exception {
        KeyPairGenerator generator;
        if (spec.name().startsWith("RSA_")) {
            generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(Integer.parseInt(spec.name().substring(4)));
        } else if (spec.name().startsWith("EC_")) {
            generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec(switch (spec) {
                case EC_P256 -> "secp256r1";
                case EC_P384 -> "secp384r1";
                case EC_P521 -> "secp521r1";
                default -> throw new AssertionError(spec);
            }));
        } else {
            generator = KeyPairGenerator.getInstance(spec == SubjectKeySpec.ED25519 ? "Ed25519" : "Ed448");
        }
        KeyId id = new KeyId(UUID.randomUUID().toString());
        keys.put(id, generator.generateKeyPair());
        return id;
    }

    IssueCertificateCommand request(CertificateProfile profile, KeyId key, CertificateId issuer,
        Integer days, Set<SubjectAlternativeName> sans) {
        return new IssueCertificateCommand("Test certificate", null, profile.getId(), key, issuer,
            new CertificateSubject(null, "Test Certificate", null, "Ignored input", null), sans, days, start, null);
    }

    X509Certificate certificate(CertificateId id) throws Exception {
        byte[] encoded = service.findById(id.id()).getCertificate().getEncoded();
        return (X509Certificate) CertificateFactory.getInstance("X.509")
            .generateCertificate(new ByteArrayInputStream(encoded));
    }

}
