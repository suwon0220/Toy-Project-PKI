package toy.pki.kms.web;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import toy.pki.kms.domain.KeyAlgorithm;

@Getter
@RequiredArgsConstructor
public enum KeyAlgorithmPreset {
    RSA_2048(KeyAlgorithm.RSA),
    RSA_3072(KeyAlgorithm.RSA),
    RSA_4096(KeyAlgorithm.RSA),
    EC_P256(KeyAlgorithm.EC),
    EC_P384(KeyAlgorithm.EC),
    EC_P521(KeyAlgorithm.EC),
    ED25519(KeyAlgorithm.Ed25519),
    ED448(KeyAlgorithm.Ed448);

    private final KeyAlgorithm algorithm;
}
