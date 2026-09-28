package toy.pki.kms.domain.parameter;

import toy.pki.kms.domain.algorithm.KeyAlgorithm;

public sealed interface KeyGenerationParameter permits RsaKeyGenerationParameter, EcKeyGenerationParameter {

    KeyAlgorithm getAlgorithm();
}
