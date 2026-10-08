package toy.pki.kms.domain;

import java.time.Instant;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import toy.pki.kms.web.KeyAlgorithmPreset;

@Data
@RequiredArgsConstructor
public class ManagedKey {
    public static final int MAX_ALIAS_LENGTH = 64;

    private final KeyId keyId; // Unique identifier for the key
    private final KeyAlgorithmPreset keyAlgorithmPreset;
    private final Instant createdAt;
    private String alias; // Optional alias for the key

    public KeyAlgorithm getKeyAlgorithm() {
        return keyAlgorithmPreset.getAlgorithm();
    }

    public void setAlias(String alias) {
        String normalized = alias == null ? null : alias.strip();
        if (normalized != null && normalized.length() > MAX_ALIAS_LENGTH) {
            throw new IllegalArgumentException("Key alias must be at most " + MAX_ALIAS_LENGTH + " characters");
        }
        this.alias = normalized == null || normalized.isEmpty() ? null : normalized;
    }
}
