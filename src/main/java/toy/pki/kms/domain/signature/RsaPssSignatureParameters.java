package toy.pki.kms.domain.signature;

import toy.pki.kms.domain.HashAlgorithm;

public record RsaPssSignatureParameters(
    HashAlgorithm hashAlgorithm,
    HashAlgorithm mgf1HashAlgorithm,
    int saltLength
) implements SignatureParameters {
}
