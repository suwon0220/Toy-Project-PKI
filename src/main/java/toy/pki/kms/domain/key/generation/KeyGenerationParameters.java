package toy.pki.kms.domain.key.generation;

import toy.pki.kms.domain.key.KeyAlgorithm;

public interface KeyGenerationParameters {
    KeyAlgorithm algorithm();
}
