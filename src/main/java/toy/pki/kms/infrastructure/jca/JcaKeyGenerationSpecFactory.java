package toy.pki.kms.infrastructure.jca;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;

import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import toy.pki.kms.domain.parameter.EcKeyGenerationParameter;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;
import toy.pki.kms.domain.parameter.RsaKeyGenerationParameter;

@Component
public class JcaKeyGenerationSpecFactory {

    public AlgorithmParameterSpec create(
            @Nonnull KeyGenerationParameter parameter
    ) {
        if (parameter instanceof RsaKeyGenerationParameter) {
            return createRsa((RsaKeyGenerationParameter) parameter);
        }
        if (parameter instanceof EcKeyGenerationParameter) {
            return createEc((EcKeyGenerationParameter) parameter);
        }
        throw new IllegalArgumentException("Unsupported key generation parameter: " + parameter.getClass().getName());
    }

    private AlgorithmParameterSpec createRsa(RsaKeyGenerationParameter parameter) {
        return new RSAKeyGenParameterSpec(parameter.keySize(), RSAKeyGenParameterSpec.F4);
    }

    private AlgorithmParameterSpec createEc(EcKeyGenerationParameter parameter) {
        return new ECGenParameterSpec(parameter.curve().name());
    }
}
