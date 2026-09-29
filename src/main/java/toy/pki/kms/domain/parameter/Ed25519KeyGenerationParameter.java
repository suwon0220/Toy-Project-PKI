package toy.pki.kms.domain.parameter;

import toy.pki.kms.domain.algorithm.KeyAlgorithm;

public record Ed25519KeyGenerationParameter() implements KeyGenerationParameter {

    @Override
    public KeyAlgorithm getAlgorithm() {
        return KeyAlgorithm.Ed25519;
    }

}
