package toy.pki.kms.infrastructure.jca.keygenerator;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

import org.springframework.stereotype.Component;

import toy.pki.kms.domain.key.generation.EcKeyGenerationParameters;
import toy.pki.kms.domain.key.generation.KeyGenerationParameters;

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
