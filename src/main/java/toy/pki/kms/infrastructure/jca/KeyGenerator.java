package toy.pki.kms.infrastructure.jca;

import toy.pki.kms.domain.algorithm.KeyAlgorithm;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;

import java.security.GeneralSecurityException;

public interface KeyGenerator {

    KeyAlgorithm supports();

    ManagedKey generate(
            KeyAlgorithmPreset keyAlgorithmPreset,
            KeyGenerationParameter keyGenerationParameter
    ) throws GeneralSecurityException;

}
