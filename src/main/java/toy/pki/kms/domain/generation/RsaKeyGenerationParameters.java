package toy.pki.kms.domain.generation;

import toy.pki.kms.domain.KeyAlgorithm;

public record RsaKeyGenerationParameters(
    int keySize
) implements KeyGenerationParameters {

    @Override
    public KeyAlgorithm algorithm() {
        return KeyAlgorithm.RSA;
    }
}
