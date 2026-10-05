package toy.pki.kms.domain.key.generation;

import toy.pki.kms.domain.key.KeyAlgorithm;

public record Ed25519KeyGenerationParameters() implements KeyGenerationParameters {
    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.Ed25519;
    }
}
