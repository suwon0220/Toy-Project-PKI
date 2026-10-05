package toy.pki.kms.infrastructure.jca.keygenerator;

import java.security.KeyPairGenerator;

import org.springframework.stereotype.Component;

import toy.pki.kms.domain.key.generation.KeyGenerationParameters;
import toy.pki.kms.domain.key.generation.RsaKeyGenerationParameters;

@Component
public class RsaJcaKeyGenerator extends AbstractJcaKeyGenerator {

    @Override
    public boolean supports(KeyGenerationParameters parameters) {
        return parameters instanceof RsaKeyGenerationParameters;
    }

    @Override
    protected void initializeKeyPairGenerator(KeyPairGenerator keyPairGenerator, KeyGenerationParameters parameters) {
        RsaKeyGenerationParameters keyGenerationParameter = (RsaKeyGenerationParameters) parameters;
        keyPairGenerator.initialize(keyGenerationParameter.keySize());
    }


}
