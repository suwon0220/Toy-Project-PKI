package toy.pki.ca.domain.profile;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import toy.pki.kms.domain.key.generation.KeyGenerationParameters;

public record SubjectKeyPolicy(
    @NotEmpty Set<KeyGenerationParameters> allowedKeys) {

    public SubjectKeyPolicy {
        allowedKeys = Set.copyOf(allowedKeys);
    }

    public boolean allows(KeyGenerationParameters parameters) {
        return allowedKeys.contains(parameters);
    }
}