package toy.pki.kms.service;

import java.security.GeneralSecurityException;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import toy.pki.kms.domain.algorithm.KeyGenerationProfile;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;
import toy.pki.kms.infrastructure.jca.KeyGenerator;
import toy.pki.kms.infrastructure.jca.KeyGeneratorRegistry;

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
