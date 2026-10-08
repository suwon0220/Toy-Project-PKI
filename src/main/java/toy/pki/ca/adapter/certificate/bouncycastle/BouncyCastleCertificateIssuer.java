package toy.pki.ca.adapter.certificate.bouncycastle;

import jakarta.validation.Validator;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.IPAddress;
import org.springframework.stereotype.Component;
import toy.pki.ca.application.certificate.model.IssueCertificateCommand;
import toy.pki.ca.domain.certificate.CertificateSerialNumber;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.certificate.CertificateSubject;
import toy.pki.ca.domain.certificate.MyCertificate;
import toy.pki.ca.domain.certificate.SubjectAlternativeName;
import toy.pki.ca.domain.policy.DnAttributePolicy;
import toy.pki.ca.domain.policy.DnPolicy;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.domain.profile.SubjectKeySpec;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.domain.KeyId;

@Component
@RequiredArgsConstructor
public class BouncyCastleCertificateIssuer {

    // Used locally for encoding/verification; private-key operations stay in KMS.
    private static final Provider PROVIDER = new BouncyCastleProvider();
    private final KeyManagementService kms;
    private final Validator validator;

    public X509Certificate issue(
        CertificateProfile profile, IssueCertificateCommand request, MyCertificate issuer,
        CertificateSerialNumber serial)
        throws GeneralSecurityException, IOException {
        validateProfile(profile, request);
        boolean root = profile.getCertificateType() == CertificateType.ROOT_CA;
        boolean ca = profile.getCertificateType() != CertificateType.END_ENTITY;
        X500Name subject = subject(profile.getDnPolicy(), request.subject());
        if (subject.getRDNs().length == 0 && (ca || request.subjectAlternativeNames().isEmpty())) {
            throw new IllegalArgumentException("A subject DN is required unless an end-entity certificate has SANs");
        }
        GeneralName[] sans = request.subjectAlternativeNames().stream()
                                    .map(this::generalName).toArray(GeneralName[]::new);
        int days = request.validityDays() == null ? profile.getDefaultValidityDays() : request.validityDays();
        if (days < 1 || days > profile.getMaxValidityDays()) {
            throw new IllegalArgumentException("Validity must be between 1 and " + profile.getMaxValidityDays() + " days");
        }
        Instant notBefore = (request.notBefore() == null
                             ? Instant.now()
                             : request.notBefore()).truncatedTo(ChronoUnit.SECONDS);
        Instant notAfter = notBefore.plus(days, ChronoUnit.DAYS);
        if (!notAfter.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Certificate validity has already ended");
        }
        X509Certificate issuerCertificate = root ? null : issuerCertificate(issuer, notBefore, notAfter);
        Integer pathLength = pathLength(profile, issuerCertificate);
        SubjectPublicKeyInfo subjectKey = SubjectPublicKeyInfo.getInstance(kms.getPublicKeyOf(request.subjectKeyId()));
        if (!profile.getSubjectKeyPolicy().allows(keySpec(subjectKey))) {
            throw new IllegalArgumentException("The subject key is not allowed by the profile");
        }
        SubjectPublicKeyInfo signingKey = root ? subjectKey
                                               : SubjectPublicKeyInfo.getInstance(issuerCertificate.getPublicKey()
                                                                                                   .getEncoded());
        KeyId signingKeyId = root ? request.subjectKeyId() : issuer.getSubjectKeyId();
        CertificateSignatureAlgorithm algorithm = signatureAlgorithm(profile, request.signatureAlgorithm(), keySpec(signingKey));
        X500Name issuerName = root
                              ? subject
                              : X500Name.getInstance(issuerCertificate.getSubjectX500Principal().getEncoded());
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
            issuerName, serial.value(), Date.from(notBefore), Date.from(notAfter), subject, subjectKey);
        builder.addExtension(Extension.basicConstraints, true,
            pathLength == null ? new BasicConstraints(ca) : new BasicConstraints(pathLength));
        int usages = profile.getKeyUsages().stream().mapToInt(this::keyUsage).reduce(0, (a, b) -> a | b);
        builder.addExtension(Extension.keyUsage, true, new org.bouncycastle.asn1.x509.KeyUsage(usages));
        if (!profile.getExtendedKeyUsages().isEmpty()) {
            KeyPurposeId[] purposes = profile.getExtendedKeyUsages().stream()
                                             .map(eku -> KeyPurposeId.getInstance(new ASN1ObjectIdentifier(eku.value())))
                                             .toArray(KeyPurposeId[]::new);
            builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(purposes));
        }
        if (sans.length > 0) {
            builder.addExtension(Extension.subjectAlternativeName, subject.getRDNs().length == 0, new GeneralNames(sans));
        }
        JcaX509ExtensionUtils extensions = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.subjectKeyIdentifier, false, extensions.createSubjectKeyIdentifier(subjectKey));
        builder.addExtension(Extension.authorityKeyIdentifier, false, root
                                                                      ? extensions.createAuthorityKeyIdentifier(subjectKey)
                                                                      : extensions.createAuthorityKeyIdentifier(issuerCertificate));
        X509Certificate certificate = new JcaX509CertificateConverter().setProvider(PROVIDER)
                                                                       .getCertificate(builder.build(new KmsContentSigner(kms, signingKeyId, algorithm)));
        // Detect a wrong KMS key/signature before saving the certificate.
        certificate.verify(new JcaPEMKeyConverter().setProvider(PROVIDER).getPublicKey(signingKey), PROVIDER);
        return certificate;
    }

    private void validateProfile(CertificateProfile profile, IssueCertificateCommand request) {
        if (!profile.getId().equals(request.profileId()) || profile.getStatus() != ProfileStatus.ACTIVE) {
            throw new IllegalArgumentException("An active matching profile is required");
        }
        var violations = validator.validate(profile);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid certificate profile: " + violations.iterator()
                                                                                           .next()
                                                                                           .getMessage());
        }
        if (profile.getCertificateType() == null || profile.getAllowedSignatures() == null
            || profile.getAllowedSignatures().isEmpty() || profile.getAllowedSignatures()
                                                                  .stream()
                                                                  .anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Certificate type and allowed signatures are required");
        }
        if (profile.getCertificateType() != CertificateType.END_ENTITY && !profile.getKeyUsages()
                                                                                  .contains(KeyUsage.KEY_CERT_SIGN)) {
            throw new IllegalArgumentException("CA profiles must include KEY_CERT_SIGN");
        }
        if (profile.getSanPolicy().required() && request.subjectAlternativeNames().isEmpty()) {
            throw new IllegalArgumentException("Subject alternative names are required by the profile");
        }
        if (request.subjectAlternativeNames().stream().anyMatch(san -> san.type() == null
            || profile.getSanPolicy().allowedTypes() == null || !profile.getSanPolicy()
                                                                        .allowedTypes()
                                                                        .contains(san.type()))) {
            throw new IllegalArgumentException("A subject alternative name type is not allowed by the profile");
        }
    }

    private X509Certificate issuerCertificate(MyCertificate issuer, Instant notBefore, Instant notAfter)
        throws GeneralSecurityException {
        if (issuer == null || issuer.getStatus() != CertificateStatus.ACTIVE || issuer.getSubjectKeyId() == null
            || !(issuer.getCertificate() instanceof X509Certificate certificate)) {
            throw new IllegalArgumentException("An active issuer certificate with a KMS key is required");
        }
        certificate.checkValidity();
        boolean[] usages = certificate.getKeyUsage();
        if (certificate.getBasicConstraints() < 0 || usages == null || usages.length <= 5 || !usages[5]) {
            throw new IllegalArgumentException("Issuer must be a CA with KEY_CERT_SIGN");
        }
        if (notBefore.isBefore(certificate.getNotBefore().toInstant()) || notAfter.isAfter(certificate.getNotAfter()
                                                                                                      .toInstant())) {
            throw new IllegalArgumentException("Certificate validity must be within the issuer validity period");
        }
        return certificate;
    }

    private Integer pathLength(CertificateProfile profile, X509Certificate issuer) {
        if (profile.getCertificateType() == CertificateType.END_ENTITY) {
            return null;
        }
        Integer requested = profile.getPathLenConstraint();
        if (issuer == null || issuer.getBasicConstraints() == Integer.MAX_VALUE) {
            return requested;
        }
        int maximum = issuer.getBasicConstraints() - 1;
        if (maximum < 0 || (requested != null && requested > maximum)) {
            throw new IllegalArgumentException("Issuer path length does not allow this CA certificate");
        }
        return requested == null ? maximum : requested;
    }

    private X500Name subject(DnPolicy policy, CertificateSubject values) {
        CertificateSubject input = values == null ? new CertificateSubject(null, null, null, null, null) : values;
        X500NameBuilder name = new X500NameBuilder(BCStyle.INSTANCE);
        add(name, BCStyle.C, policy == null ? null : policy.country(), input.country());
        add(name, BCStyle.O, policy == null ? null : policy.organization(), input.organization());
        add(name, BCStyle.OU, policy == null ? null : policy.organizationUnit(), input.organizationUnit());
        add(name, BCStyle.CN, policy == null ? null : policy.commonName(), input.commonName());
        add(name, BCStyle.DC, policy == null ? null : policy.domainComponent(), input.domainComponent());
        return name.build();
    }

    private void add(X500NameBuilder name, ASN1ObjectIdentifier oid, DnAttributePolicy policy, String input) {
        String value = policy != null && policy.fixedValue() != null && !policy.fixedValue().isBlank()
                       ? policy.fixedValue() : input;
        if (value == null || value.isBlank()) {
            if (policy != null && policy.required()) {
                throw new IllegalArgumentException("Required subject attribute: " + BCStyle.INSTANCE.oidToDisplayName(oid));
            }
            return;
        }
        if (oid.equals(BCStyle.C) && !value.strip().matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("Country must be a two-letter code");
        }
        name.addRDN(oid, value.strip());
    }

    private SubjectKeySpec keySpec(SubjectPublicKeyInfo key) throws IOException {
        String algorithm = key.getAlgorithm().getAlgorithm().getId();
        String preset = switch (algorithm) {
            case "1.2.840.113549.1.1.1" ->
                "RSA_" + RSAPublicKey.getInstance(key.parsePublicKey()).getModulus().bitLength();
            case "1.2.840.10045.2.1" ->
                switch (ASN1ObjectIdentifier.getInstance(key.getAlgorithm().getParameters()).getId()) {
                    case "1.2.840.10045.3.1.7" -> "EC_P256";
                    case "1.3.132.0.34" -> "EC_P384";
                    case "1.3.132.0.35" -> "EC_P521";
                    default -> throw new IllegalArgumentException("Unsupported EC curve");
                };
            case "1.3.101.112" -> "ED25519";
            case "1.3.101.113" -> "ED448";
            default -> throw new IllegalArgumentException("Unsupported public key algorithm: " + algorithm);
        };
        return SubjectKeySpec.valueOf(preset);
    }

    private CertificateSignatureAlgorithm signatureAlgorithm(
        CertificateProfile profile, CertificateSignatureAlgorithm requested, SubjectKeySpec key) {
        if (requested != null) {
            if (!profile.getAllowedSignatures().contains(requested) || !compatible(requested, key)) {
                throw new IllegalArgumentException("Signature algorithm must match the signing key and profile");
            }
            return requested;
        }
        return profile.getAllowedSignatures().stream().filter(Objects::nonNull).sorted()
                      .filter(algorithm -> compatible(algorithm, key)).findFirst()
                      .orElseThrow(() -> new IllegalArgumentException("No allowed signature algorithm matches the signing key"));
    }

    private boolean compatible(CertificateSignatureAlgorithm algorithm, SubjectKeySpec key) {
        return switch (algorithm) {
            case RSA_WITH_SHA256, RSA_WITH_SHA384, RSA_WITH_SHA512 -> key.name().startsWith("RSA_");
            case ECDSA_WITH_SHA256, ECDSA_WITH_SHA384, ECDSA_WITH_SHA512 -> key.name().startsWith("EC_");
            case ED25519 -> key == SubjectKeySpec.ED25519;
            case ED448 -> key == SubjectKeySpec.ED448;
            case UNKNOWN -> false;
        };
    }

    private int keyUsage(KeyUsage usage) {
        return switch (usage) {
            case DIGITAL_SIGNATURE -> org.bouncycastle.asn1.x509.KeyUsage.digitalSignature;
            case CONTENT_COMMITMENT -> org.bouncycastle.asn1.x509.KeyUsage.nonRepudiation;
            case KEY_ENCIPHERMENT -> org.bouncycastle.asn1.x509.KeyUsage.keyEncipherment;
            case DATA_ENCIPHERMENT -> org.bouncycastle.asn1.x509.KeyUsage.dataEncipherment;
            case KEY_AGREEMENT -> org.bouncycastle.asn1.x509.KeyUsage.keyAgreement;
            case KEY_CERT_SIGN -> org.bouncycastle.asn1.x509.KeyUsage.keyCertSign;
            case CRL_SIGN -> org.bouncycastle.asn1.x509.KeyUsage.cRLSign;
            case ENCIPHER_ONLY -> org.bouncycastle.asn1.x509.KeyUsage.encipherOnly;
            case DECIPHER_ONLY -> org.bouncycastle.asn1.x509.KeyUsage.decipherOnly;
        };
    }

    private GeneralName generalName(SubjectAlternativeName san) {
        if (san.value().chars().anyMatch(character -> character > 127)) {
            throw new IllegalArgumentException("SAN values must use ASCII (punycode for internationalized names)");
        }
        int tag = switch (san.type()) {
            case DNS_NAME -> GeneralName.dNSName;
            case IP_ADDRESS -> {
                if (!IPAddress.isValid(san.value())) {
                    throw new IllegalArgumentException("Invalid SAN IP address");
                }
                yield GeneralName.iPAddress;
            }
            case EMAIL_ADDRESS -> GeneralName.rfc822Name;
            case URI -> GeneralName.uniformResourceIdentifier;
        };
        return new GeneralName(tag, san.value());
    }
}
