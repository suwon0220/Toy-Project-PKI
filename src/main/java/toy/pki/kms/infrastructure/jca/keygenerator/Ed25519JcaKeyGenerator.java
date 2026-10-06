package toy.pki.kms.infrastructure.jca.keygenerator;

import java.security.KeyPairGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.key.generation.Ed25519KeyGenerationParameters;
import toy.pki.kms.domain.key.generation.KeyGenerationParameters;

@Component
@RequiredArgsConstructor
public class Ed25519JcaKeyGenerator extends AbstractJcaKeyGenerator {

    @Override
    public boolean supports(KeyGenerationParameters parameters) {
        return parameters instanceof Ed25519KeyGenerationParameters;
    }

    @Override
    protected void initializeKeyPairGenerator(KeyPairGenerator keyPairGenerator, KeyGenerationParameters parameters) {
        // Ed25519 does not require any specific initialization parameters
    }
}
