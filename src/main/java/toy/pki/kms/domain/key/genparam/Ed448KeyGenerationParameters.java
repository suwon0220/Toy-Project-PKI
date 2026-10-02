package toy.pki.kms.domain.key.genparam;

import toy.pki.kms.domain.key.KeyAlgorithm;

public record Ed448KeyGenerationParameters() implements KeyGenerationParameters {
    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.Ed448;
    }
}
