package toy.pki.kms.application.service;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import toy.pki.kms.application.model.GeneratedKeyMaterial;
import toy.pki.kms.application.model.KeySearchCriteria;
import toy.pki.kms.application.port.KeyIdGenerator;
import toy.pki.kms.application.port.KeyMaterialProvider;
import toy.pki.kms.application.port.KeyRepository;
import toy.pki.kms.application.registry.KeyMaterialProviderRegistry;
import toy.pki.kms.domain.key.KeyId;
import toy.pki.kms.domain.key.KeyProviderId;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.key.generation.KeyGenerationParameters;
import toy.pki.kms.domain.key.signature.SignatureParameters;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class KeyManagementService {

    private final KeyRepository keyRepository;
    private final KeyIdGenerator keyIdGenerator;
    private final KeyMaterialProviderRegistry keyMaterialProviderRegistry;

    public List<ManagedKey> search(@NotNull KeySearchCriteria criteria) {
        return keyRepository.findByCriteria(criteria);
    }

    public ManagedKey findById(@NotNull KeyId keyId) {
        return keyRepository.findById(keyId)
            .orElseThrow(() -> new NoSuchElementException(
                "Key not found: " + keyId.value()));
    }

    public List<ManagedKey> findAll() {
        return keyRepository.findAll();
    }

    public ManagedKey generate(@NotEmpty String providerId, @NotNull KeyGenerationParameters parameters)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return generate(providerId, parameters, null);
    }

    public ManagedKey generate(
        @NotEmpty String providerId,
        @NotNull KeyGenerationParameters parameters,
        @Size(max = ManagedKey.MAX_ALIAS_LENGTH) String alias)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        log.debug("Generating key with providerId: {}, parameters: {}", providerId, parameters);

        // KeyMaterialProvider 조회
        KeyProviderId keyProviderId = new KeyProviderId(providerId);
        KeyMaterialProvider keyMaterialProvider = keyMaterialProviderRegistry.get(keyProviderId);

        // Provider에게 키 생성 요청
        GeneratedKeyMaterial generatedKeyMaterial = keyMaterialProvider.generate(parameters);
        if (generatedKeyMaterial == null || generatedKeyMaterial.reference() == null) {
            throw new IllegalStateException("Generated key material is null or has no reference");
        }

        // Provider에서 키를 생성하고 reference를 검증한 다음 실행
        try {
            ManagedKey managedKey = new ManagedKey(
                keyIdGenerator.generate(generatedKeyMaterial.publicKey()),
                parameters,
                generatedKeyMaterial.reference(),
                keyProviderId,
                java.time.Instant.now());
            managedKey.setAlias(alias);

            keyRepository.save(managedKey);
            log.debug("Managed key created and saved: {}", managedKey);
            return managedKey;
        } catch (RuntimeException failure) {
            try {
                keyMaterialProvider.delete(generatedKeyMaterial.reference());
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
                log.error(
                    "Failed to clean up key material: providerId={}, reference={}",
                    keyProviderId,
                    generatedKeyMaterial.reference(),
                    cleanupFailure);
            }

            throw failure;
        }
    }

    public byte[] sign(
        KeyId keyId,
        SignatureParameters parameters,
        byte[] data) {
        ManagedKey managedKey = keyRepository.findById(keyId)
            .orElseThrow(() -> new NoSuchElementException(
                "Managed key not found: " + keyId.value()));

        KeyMaterialProvider provider = keyMaterialProviderRegistry.get(
            managedKey.getKeyProviderId());

        return provider.sign(
            managedKey.getKeyMaterialRef(),
            parameters,
            data);
    }

    public boolean verify(
        KeyId keyId,
        SignatureParameters parameters,
        byte[] data,
        byte[] signature) {
        ManagedKey managedKey = keyRepository.findById(keyId)
            .orElseThrow(() -> new NoSuchElementException(
                "Managed key not found: " + keyId.value()));

        KeyMaterialProvider provider = keyMaterialProviderRegistry.get(
            managedKey.getKeyProviderId());

        return provider.verify(
            managedKey.getKeyMaterialRef(),
            parameters,
            data,
            signature);
    }
}
