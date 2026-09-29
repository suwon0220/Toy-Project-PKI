package toy.pki.kms.infrastructure.jca;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.parameter.*;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;

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
