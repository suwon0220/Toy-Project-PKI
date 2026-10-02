package toy.pki.kms.application.service;

import jakarta.validation.constraints.NotEmpty;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import toy.pki.kms.application.port.GeneratedKeyMaterial;
import toy.pki.kms.application.port.KeyMaterialProvider;
import toy.pki.kms.application.port.KeyRepository;
import toy.pki.kms.application.registry.KeyMaterialProviderRegistry;
import toy.pki.kms.domain.key.KeyId;
import toy.pki.kms.domain.key.KeyProviderId;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.key.genparam.KeyGenerationParameters;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeyManagementService {

    private final KeyRepository keyRepository;
    private final KeyMaterialProviderRegistry keyMaterialProviderRegistry;

    public ManagedKey generate(@NotEmpty String providerId, KeyGenerationParameters parameters)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        log.debug("Generating key with providerId: {}, parameters: {}", providerId, parameters);

        // KeyMaterialProvider 조회
        KeyProviderId keyProviderId = new KeyProviderId(providerId);
        KeyMaterialProvider keyMaterialProvider = keyMaterialProviderRegistry.get(keyProviderId);

        // Provider에게 키 생성 요청
        GeneratedKeyMaterial generatedKeyMaterial = keyMaterialProvider.generate(parameters);
        if(generatedKeyMaterial == null || generatedKeyMaterial.reference() == null) {
            throw new IllegalStateException("Generated key material is null or has no reference");
        }

        // ManagedKey 객체 생성
        ManagedKey managedKey = new ManagedKey(
            new KeyId(generatedKeyMaterial.reference().value()),
            parameters.algorithm(),
            generatedKeyMaterial.reference(),
            keyProviderId,
            java.time.Instant.now()
        );

        // 저장소에 키 저장
        keyRepository.save(managedKey);
        log.debug("Managed key created and saved: {}", managedKey);


        // 생성된 ManagedKey 반환
        return managedKey;
    }
}
