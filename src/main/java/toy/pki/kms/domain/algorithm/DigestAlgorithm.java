package toy.pki.kms.domain.algorithm;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DigestAlgorithm in JCA (Java Cryptography Architecture) format.
 */
@Getter
@AllArgsConstructor
public enum DigestAlgorithm {

    SHA224("SHA224"),
    SHA256("SHA256"),
    SHA384("SHA384"),
    SHA512("SHA512"),

    SHA512_224("SHA512/224"),
    SHA512_256("SHA512/256"),

    SHA3_224("SHA3-224"),
    SHA3_256("SHA3-256"),
    SHA3_384("SHA3-384"),
    SHA3_512("SHA3-512");

    private final String jcaName;
}