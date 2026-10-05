package toy.pki.kms.domain.key.signature;

import toy.pki.kms.domain.key.HashAlgorithm;

public record EcdsaSignatureParameters(
    HashAlgorithm hashAlgorithm) implements SignatureParameters {

}
