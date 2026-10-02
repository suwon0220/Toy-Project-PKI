package toy.pki.kms.infrastructure.jca;

import org.springframework.stereotype.Component;
import toy.pki.kms.domain.key.genparam.KeyGenerationParameters;
import toy.pki.kms.domain.key.genparam.RsaKeyGenerationParameters;

import java.security.KeyPairGenerator;

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
