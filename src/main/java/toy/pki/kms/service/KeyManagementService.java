package toy.pki.kms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import toy.pki.kms.domain.algorithm.KeyGenerationProfile;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;
import toy.pki.kms.infrastructure.jca.KeyGenerator;
import toy.pki.kms.infrastructure.jca.KeyGeneratorRegistry;

import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
public class KeyManagementService {

    private final KeyGeneratorRegistry keyGeneratorRegistry;

    public ManagedKey generate(KeyGenerationProfile profile) throws GeneralSecurityException {
        KeyGenerationParameter parameter = profile.createParameter();

        KeyGenerator generator = keyGeneratorRegistry.get(parameter.getAlgorithm());

        return generator.generate(profile, parameter);
    }
}
