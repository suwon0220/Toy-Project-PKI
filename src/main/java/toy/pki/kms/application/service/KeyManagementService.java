package toy.pki.kms.application.service;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import toy.pki.kms.adapter.http.RestKmsClient;
import toy.pki.kms.application.model.KeySearchCriteria;
import toy.pki.kms.domain.HashAlgorithm;
import toy.pki.kms.domain.KeyId;
import toy.pki.kms.domain.ManagedKey;
import toy.pki.kms.domain.SignatureAlgorithm;
import toy.pki.kms.web.KeyAlgorithmPreset;

@Validated
@Service
@RequiredArgsConstructor
public class KeyManagementService {

    private final RestKmsClient kmsClient;

    public List<ManagedKey> search(@NotNull KeySearchCriteria criteria) {
        return findAll().stream().filter(criteria.toPredicate()).toList();
    }

    public List<ManagedKey> findAll() {
        return kmsClient.findAll();
    }

    public ManagedKey generate(
        @NotEmpty String keyAlgorithm,
        @Size(max = ManagedKey.MAX_ALIAS_LENGTH) String alias) {
        KeyAlgorithmPreset preset = KeyAlgorithmPreset.valueOf(keyAlgorithm);
        String normalizedAlias = alias == null || alias.isBlank() ? null : alias.strip();
        return kmsClient.generate(preset, normalizedAlias);
    }

    public byte[] sign(
        @NotNull KeyId keyId,
        @NotNull SignatureAlgorithm algorithm,
        HashAlgorithm digest,
        @NotNull byte[] data) {
        return kmsClient.sign(keyId, algorithm, digest, data);
    }

    public boolean verify(
        @NotNull KeyId keyId,
        @NotNull SignatureAlgorithm algorithm,
        HashAlgorithm digest,
        @NotNull byte[] data,
        @NotNull byte[] signature) {
        return kmsClient.verify(keyId, algorithm, digest, data, signature);
    }

    public byte[] getPublicKeyOf(@NotNull KeyId keyId) {
        return kmsClient.getPublicKeyOf(keyId);
    }

    public KeyId delete(@NotNull KeyId keyId) {
        return kmsClient.delete(keyId);
    }
}
