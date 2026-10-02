package toy.pki.kms.infrastructure.jca;

import java.security.KeyPairGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.key.genparam.Ed448KeyGenerationParameters;
import toy.pki.kms.domain.key.genparam.KeyGenerationParameters;

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
