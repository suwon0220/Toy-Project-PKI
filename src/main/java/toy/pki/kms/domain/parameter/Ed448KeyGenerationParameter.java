package toy.pki.kms.domain.parameter;

import toy.pki.kms.domain.algorithm.KeyAlgorithm;

public record Ed448KeyGenerationParameter() implements KeyGenerationParameter {

    @Override
    public KeyAlgorithm getAlgorithm() {
        return KeyAlgorithm.Ed448;
    }

}
