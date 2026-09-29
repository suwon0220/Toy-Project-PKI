package toy.pki.kms.domain.parameter;

import toy.pki.kms.domain.algorithm.KeyAlgorithm;

public sealed interface KeyGenerationParameter permits EcKeyGenerationParameter, Ed25519KeyGenerationParameter, Ed448KeyGenerationParameter, RsaKeyGenerationParameter {

    KeyAlgorithm getAlgorithm();
}
