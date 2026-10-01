package toy.pki.kms.domain.key;

import java.security.GeneralSecurityException;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;
import toy.pki.kms.domain.request.KeyGenerationRequest;
import toy.pki.kms.infrastructure.jca.KeyGenerator;
import toy.pki.kms.infrastructure.jca.KeyGeneratorRegistry;

@Service
@RequiredArgsConstructor
public class KeyGenerationService {

    private final KeyGeneratorRegistry keyGeneratorRegistry;

    public ManagedKey generate(KeyGenerationRequest request) throws GeneralSecurityException {
        KeyGenerationParameter parameter = request.keyAlgorithmPreset().createParameter();

        KeyGenerator generator = keyGeneratorRegistry.get(parameter.getAlgorithm());

        ManagedKey managedKey = generator.generate(request.keyAlgorithmPreset(), parameter);
        managedKey.setDisplayName(request.displayName());
        return managedKey;
    }

}
