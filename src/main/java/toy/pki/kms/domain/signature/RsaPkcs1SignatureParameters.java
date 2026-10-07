package toy.pki.kms.domain.signature;

import toy.pki.kms.domain.HashAlgorithm;

public record RsaPkcs1SignatureParameters(
    HashAlgorithm hashAlgorithm
) implements SignatureParameters {
}
