package toy.pki.ca.adapter.certificate.bouncycastle;

import java.io.ByteArrayOutputStream;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.domain.HashAlgorithm;
import toy.pki.kms.domain.KeyId;
import toy.pki.kms.domain.SignatureAlgorithm;

@RequiredArgsConstructor
final class KmsContentSigner implements ContentSigner {

    private final KeyManagementService kms;
    private final KeyId keyId;
    private final CertificateSignatureAlgorithm algorithm;
    @Getter private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    static KmsContentSigner forKey(KeyManagementService kms, KeyId keyId, PublicKey key) {
        String oid = SubjectPublicKeyInfo.getInstance(key.getEncoded()).getAlgorithm().getAlgorithm().getId();
        CertificateSignatureAlgorithm algorithm = switch (oid) {
            case "1.2.840.113549.1.1.1" -> CertificateSignatureAlgorithm.RSA_WITH_SHA256;
            case "1.2.840.10045.2.1" -> {
                int bits = ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize();
                yield bits <= 256 ? CertificateSignatureAlgorithm.ECDSA_WITH_SHA256
                    : bits <= 384 ? CertificateSignatureAlgorithm.ECDSA_WITH_SHA384
                        : CertificateSignatureAlgorithm.ECDSA_WITH_SHA512;
            }
            case "1.3.101.112" -> CertificateSignatureAlgorithm.ED25519;
            case "1.3.101.113" -> CertificateSignatureAlgorithm.ED448;
            default -> throw new IllegalArgumentException("지원하지 않는 CA 서명 키입니다.");
        };
        return new KmsContentSigner(kms, keyId, algorithm);
    }

    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return new DefaultSignatureAlgorithmIdentifierFinder().find(algorithm.getJcaName());
    }

    @Override
    public byte[] getSignature() {
        SignatureAlgorithm signature = switch (algorithm) {
            case RSA_WITH_SHA256, RSA_WITH_SHA384, RSA_WITH_SHA512 -> SignatureAlgorithm.RSA_PKCS1_V1_5;
            case ECDSA_WITH_SHA256, ECDSA_WITH_SHA384, ECDSA_WITH_SHA512 -> SignatureAlgorithm.ECDSA;
            case ED25519 -> SignatureAlgorithm.Ed25519;
            case ED448 -> SignatureAlgorithm.Ed448;
            case UNKNOWN -> throw new IllegalArgumentException("Unsupported signature algorithm");
        };
        HashAlgorithm digest = switch (algorithm) {
            case RSA_WITH_SHA256, ECDSA_WITH_SHA256 -> HashAlgorithm.SHA256;
            case RSA_WITH_SHA384, ECDSA_WITH_SHA384 -> HashAlgorithm.SHA384;
            case RSA_WITH_SHA512, ECDSA_WITH_SHA512 -> HashAlgorithm.SHA512;
            default -> null;
        };
        return kms.sign(keyId, signature, digest, outputStream.toByteArray());
    }
}
