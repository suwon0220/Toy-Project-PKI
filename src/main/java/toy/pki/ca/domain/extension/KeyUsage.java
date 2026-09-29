package toy.pki.ca.domain.extension;

public enum KeyUsage {
    DIGITAL_SIGNATURE,
    CONTENT_COMMITMENT,
    KEY_ENCIPHERMENT,
    DATA_ENCIPHERMENT,
    KEY_AGREEMENT,
    KEY_CERT_SIGN,
    CRL_SIGN,
    ENCIPHER_ONLY,
    DECIPHER_ONLY
}
