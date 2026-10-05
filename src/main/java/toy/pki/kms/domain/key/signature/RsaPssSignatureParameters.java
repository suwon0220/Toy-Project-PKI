package toy.pki.kms.domain.key.signature;

import toy.pki.kms.domain.key.HashAlgorithm;

public record RsaPssSignatureParameters(
    HashAlgorithm hashAlgorithm,
    HashAlgorithm mgf1HashAlgorithm,
    int saltLength
) implements SignatureParameters {
}
