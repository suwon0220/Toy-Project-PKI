package toy.pki.kms.infrastructure.jca;

import java.security.InvalidAlgorithmParameterException;
import java.security.spec.ECGenParameterSpec;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.key.genparam.EcKeyGenerationParameters;
import toy.pki.kms.domain.key.genparam.KeyGenerationParameters;

import java.security.KeyPairGenerator;

@Component
public class EcJcaKeyGenerator extends AbstractJcaKeyGenerator {

    @Override
    public boolean supports(KeyGenerationParameters parameters) {
        return parameters instanceof EcKeyGenerationParameters;
    }

    @Override
    protected void initializeKeyPairGenerator(KeyPairGenerator keyPairGenerator, KeyGenerationParameters parameters)
        throws InvalidAlgorithmParameterException {
        EcKeyGenerationParameters keyGenerationParameter = (EcKeyGenerationParameters) parameters;
        keyPairGenerator.initialize(new ECGenParameterSpec(keyGenerationParameter.curve()));
    }


}
