package toy.pki.ca.domain.extension;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KeyUsageBit {
    DIGITAL_SIGNATURE("Digital Signature", 1 << 7),
    CONTENT_COMMITMENT("Content Commitment", 1 << 6),
    KEY_ENCIPHERMENT("Key Encipherment", 1 << 5),
    DATA_ENCIPHERMENT("Data Encipherment", 1 << 4),
    KEY_AGREEMENT("Key Agreement", 1 << 3),
    KEY_CERT_SIGN("Key Certificate Sign", 1 << 2),
    CRL_SIGN("CRL Sign", 1 << 1),
    ENCIPHER_ONLY("Encipher Only", 1),
    DECIPHER_ONLY("Decipher Only", 1 << 15);

    private final String displayName;
    private final int value;
}

