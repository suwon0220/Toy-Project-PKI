package toy.pki.kms.domain.generation;

import toy.pki.kms.domain.KeyAlgorithm;

public record EcKeyGenerationParameters(
    String curve
) implements KeyGenerationParameters {

    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.EC;
    }
}
