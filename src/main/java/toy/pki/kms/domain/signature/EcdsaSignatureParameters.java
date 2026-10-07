package toy.pki.kms.domain.signature;

import toy.pki.kms.domain.HashAlgorithm;

public record EcdsaSignatureParameters(
    HashAlgorithm hashAlgorithm) implements SignatureParameters {

}
