package toy.pki.kms.infrastructure.jca;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.algorithm.KeyAlgorithm;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;

@Component
@RequiredArgsConstructor
public class Ed448KeyGenerator implements KeyGenerator {

    private final Provider provider;

    @Override
    public KeyAlgorithm supports() {
        return KeyAlgorithm.Ed448;
    }

    @Override
    public ManagedKey generate(
            KeyAlgorithmPreset keyAlgorithmPreset,
            KeyGenerationParameter keyGenerationParameter
    ) throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed448", provider);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return new ManagedKey(keyAlgorithmPreset, keyPair);
    }

}
