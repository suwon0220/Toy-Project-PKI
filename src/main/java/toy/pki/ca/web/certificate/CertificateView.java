package toy.pki.ca.web.certificate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import lombok.Getter;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.certificate.CertificateSubject;
import toy.pki.ca.domain.certificate.MyCertificate;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.kms.domain.KeyId;

public final class CertificateView {

    private final MyCertificate source;
    private final X509Certificate certificate;
    @Getter private final String profileName;
    private final ZoneId zone;

    public CertificateView(MyCertificate source, String profileName, ZoneId zone) {
        if (!(source.getCertificate() instanceof X509Certificate x509)) {
            throw new IllegalArgumentException("An X.509 certificate is required");
        }
        this.source = source;
        this.certificate = x509;
        this.profileName = profileName;
        this.zone = zone;
    }

    public static String pem(X509Certificate certificate) throws CertificateEncodingException {
        return "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder(64, new byte[]{'\n'})
                                                       .encodeToString(certificate.getEncoded()) + "\n-----END CERTIFICATE-----\n";
    }

    private static String part(X500Name name, ASN1ObjectIdentifier oid) {
        var rdns = name.getRDNs(oid);
        return rdns.length == 0 ? null : IETFUtils.valueToString(rdns[0].getFirst().getValue());
    }

    private static String hex(byte[] bytes) {
        return bytes == null ? null : HexFormat.ofDelimiter(":").withUpperCase().formatHex(bytes);
    }

    public CertificateId getId() {
        return source.getId();
    }

    public String getAlias() {
        return source.getAlias();
    }

    public String getTitle() {
        return getAlias() == null || getAlias().isBlank() ? getSerial() : getAlias();
    }

    public String getDescription() {
        return source.getDescription();
    }

    public KeyId getSubjectKeyId() {
        return source.getSubjectKeyId();
    }

    public CertificateId getIssuerCertificateId() {
        return source.getIssuerCertificateId();
    }

    public String getSerial() {
        return certificate.getSerialNumber().toString(16).toUpperCase();
    }

    public String getSubjectDn() {
        return certificate.getSubjectX500Principal().getName();
    }

    public String getIssuerDn() {
        return certificate.getIssuerX500Principal().getName();
    }

    public String getSignatureAlgorithm() {
        return certificate.getSigAlgName();
    }

    public ZonedDateTime getNotBefore() {
        return certificate.getNotBefore().toInstant().atZone(zone);
    }

    public ZonedDateTime getNotAfter() {
        return certificate.getNotAfter().toInstant().atZone(zone);
    }

    public ZonedDateTime getRevokedAt() {
        return source.getRevokedAt() == null ? null : source.getRevokedAt().atZone(zone);
    }

    public boolean isRevoked() {
        return source.getStatus() == CertificateStatus.REVOKED;
    }

    public long getDaysLeft() {
        return ChronoUnit.DAYS.between(Instant.now(), certificate.getNotAfter().toInstant());
    }

    public int getPathLength() {
        return certificate.getBasicConstraints();
    }

    public CertificateSubject getSubject() {
        X500Name name = X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded());
        return new CertificateSubject(part(name, BCStyle.DC), part(name, BCStyle.CN), part(name, BCStyle.OU),
            part(name, BCStyle.O), part(name, BCStyle.C));
    }

    public String getSubjectCn() {
        String cn = getSubject().commonName();
        return cn != null ? cn : (source.getAlias() != null ? source.getAlias() : getSerial());
    }

    public String getIssuerCn() {
        String cn = part(X500Name.getInstance(certificate.getIssuerX500Principal().getEncoded()), BCStyle.CN);
        return cn == null ? getIssuerDn() : cn;
    }

    public CertificateStatus getStatus() {
        CertificateStatus status = source.getStatus();
        return (status == CertificateStatus.ACTIVE || status == CertificateStatus.SUSPENDED)
                   && certificate.getNotAfter().toInstant().isBefore(Instant.now())
               ? CertificateStatus.EXPIRED
               : status;
    }

    public CertificateType getCertType() {
        if (certificate.getBasicConstraints() < 0) {
            return CertificateType.END_ENTITY;
        }
        return certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())
                   && source.getIssuerCertificateId() == null
               ? CertificateType.ROOT_CA
               : CertificateType.INTERMEDIATE_CA;
    }

    public boolean isIssuerEligible() {
        boolean[] usage = certificate.getKeyUsage();
        return getStatus() == CertificateStatus.ACTIVE && getPathLength() >= 0 && source.getSubjectKeyId() != null
            && !certificate.getNotBefore().toInstant().isAfter(Instant.now())
            && usage != null && usage.length > 5 && usage[5];
    }

    public boolean isCrlEligible() {
        boolean[] usage = certificate.getKeyUsage();
        return getStatus() == CertificateStatus.ACTIVE && source.getRevokedAt() == null
            && getPathLength() >= 0 && source.getSubjectKeyId() != null
            && !certificate.getNotBefore().toInstant().isAfter(Instant.now())
            && usage != null && usage.length > 6 && usage[6];
    }

    public String getKeyAlgorithm() {
        var key = certificate.getPublicKey();
        if (key instanceof RSAPublicKey rsa) {
            return "RSA_" + rsa.getModulus().bitLength();
        }
        if (key instanceof ECPublicKey ec) {
            return "EC_P" + ec.getParams().getCurve().getField().getFieldSize();
        }
        if (key instanceof EdECPublicKey ed) {
            return ed.getParams().getName();
        }
        return key.getAlgorithm();
    }

    public List<KeyUsage> getKeyUsages() {
        boolean[] bits = certificate.getKeyUsage();
        List<KeyUsage> result = new ArrayList<>();
        if (bits != null) {
            for (KeyUsage usage : KeyUsage.values()) {
                if (usage.ordinal() < bits.length && bits[usage.ordinal()]) {
                    result.add(usage);
                }
            }
        }
        return result;
    }

    public List<String> getExtendedKeyUsages() throws CertificateParsingException {
        List<String> usages = certificate.getExtendedKeyUsage();
        return usages == null ? List.of() : usages;
    }

    public List<String> getSubjectAlternativeNames() throws CertificateParsingException {
        var names = certificate.getSubjectAlternativeNames();
        if (names == null) {
            return List.of();
        }
        return names.stream().map(name -> {
            String type = switch ((Integer) name.get(0)) {
                case 1 -> "Email";
                case 2 -> "DNS";
                case 6 -> "URI";
                case 7 -> "IP";
                default -> name.get(0).toString();
            };
            return type + ": " + name.get(1);
        }).toList();
    }

    public String getSkid() {
        byte[] value = certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId());
        return value == null
               ? null
               : hex(SubjectKeyIdentifier.getInstance(ASN1OctetString.getInstance(value).getOctets())
                                         .getKeyIdentifier());
    }

    public String getAkid() {
        byte[] value = certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId());
        return value == null
               ? null
               : hex(AuthorityKeyIdentifier.getInstance(ASN1OctetString.getInstance(value).getOctets())
                                           .getKeyIdentifier());
    }

    public String getSha256Fingerprint() throws CertificateEncodingException, NoSuchAlgorithmException {
        return hex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
    }

    public String getPem() throws CertificateEncodingException {
        return pem(certificate);
    }
}
