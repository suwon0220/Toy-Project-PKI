package toy.pki.kms.domain.key.signature;

import toy.pki.kms.domain.key.HashAlgorithm;

public record RsaPkcs1SignatureParameters(
    HashAlgorithm hashAlgorithm
) implements SignatureParameters {
}
