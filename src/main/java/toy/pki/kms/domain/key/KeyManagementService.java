package toy.pki.kms.domain.key;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;
import toy.pki.kms.infrastructure.jca.KeyGenerator;
import toy.pki.kms.infrastructure.jca.KeyGeneratorRegistry;

import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
public class KeyManagementService {

    private final KeyGeneratorRegistry keyGeneratorRegistry;

    public ManagedKey generate(KeyAlgorithmPreset profile) throws GeneralSecurityException {
        KeyGenerationParameter parameter = profile.createParameter();

        KeyGenerator generator = keyGeneratorRegistry.get(parameter.getAlgorithm());

        return generator.generate(profile, parameter);
    }
}
