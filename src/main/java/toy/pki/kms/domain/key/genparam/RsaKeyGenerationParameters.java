package toy.pki.kms.domain.key.genparam;

import toy.pki.kms.domain.key.KeyAlgorithm;

public record RsaKeyGenerationParameters (
    int keySize
) implements KeyGenerationParameters {

    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.RSA;
    }
}
