package toy.pki.kms.domain.generation;

import toy.pki.kms.domain.KeyAlgorithm;

public record Ed448KeyGenerationParameters() implements KeyGenerationParameters {
    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.Ed448;
    }
}
