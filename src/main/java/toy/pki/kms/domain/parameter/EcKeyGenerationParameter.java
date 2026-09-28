package toy.pki.kms.domain.parameter;

import toy.pki.kms.domain.algorithm.EcCurve;
import toy.pki.kms.domain.algorithm.KeyAlgorithm;

public record EcKeyGenerationParameter(EcCurve curve) implements KeyGenerationParameter {

    @Override
    public KeyAlgorithm getAlgorithm() {
        return KeyAlgorithm.EC;
    }

}
