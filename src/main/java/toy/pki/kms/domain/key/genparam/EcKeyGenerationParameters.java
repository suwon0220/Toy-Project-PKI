package toy.pki.kms.domain.key.genparam;

import toy.pki.kms.domain.key.KeyAlgorithm;

public record EcKeyGenerationParameters(
    String curve
) implements KeyGenerationParameters {

    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.EC;
    }
}
