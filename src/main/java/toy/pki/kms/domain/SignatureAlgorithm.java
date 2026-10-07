package toy.pki.kms.domain;

public enum SignatureAlgorithm {
    DSA,
    RSA_PKCS1_V1_5,
    RSA_PSS,
    ECDSA,
    Ed448,
    Ed25519;

    public static String getSignatureAlgorithmString(SignatureAlgorithm signatureAlgorithm) {
        return switch (signatureAlgorithm) {
            case DSA -> "DSA";
            case RSA_PKCS1_V1_5 -> "SHA256withRSA";
            case RSA_PSS -> "RSA-PSS";
            case ECDSA -> "ECDSA";
            case Ed448 -> "Ed448";
            case Ed25519 -> "Ed25519";
        };
    }
}
