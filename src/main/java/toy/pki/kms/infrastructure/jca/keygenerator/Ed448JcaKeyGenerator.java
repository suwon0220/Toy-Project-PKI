package toy.pki.kms.infrastructure.jca.keygenerator;

import java.security.KeyPairGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.generation.Ed448KeyGenerationParameters;
import toy.pki.kms.domain.generation.KeyGenerationParameters;

@Component
@RequiredArgsConstructor
public class Ed448JcaKeyGenerator extends AbstractJcaKeyGenerator {

    @Override
    public boolean supports(KeyGenerationParameters parameters) {
        return parameters instanceof Ed448KeyGenerationParameters;
    }

    @Override
    protected void initializeKeyPairGenerator(KeyPairGenerator keyPairGenerator, KeyGenerationParameters parameters) {
        // Ed448 does not require any specific initialization parameters
    }
}
