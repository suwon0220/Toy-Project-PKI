package toy.pki.kms.domain.parameter;

import toy.pki.kms.domain.algorithm.KeyAlgorithm;

public record RsaKeyGenerationParameter(int keySize) implements KeyGenerationParameter {

    @Override
    public KeyAlgorithm getAlgorithm() {
        return KeyAlgorithm.RSA;
    }

}
