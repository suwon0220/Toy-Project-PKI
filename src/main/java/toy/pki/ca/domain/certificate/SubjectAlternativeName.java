package toy.pki.ca.domain.certificate;

import jakarta.validation.constraints.NotNull;
import toy.pki.ca.domain.profile.SanType;

public record SubjectAlternativeName(
    @NotNull SanType type,
    String value
) {
    public SubjectAlternativeName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SubjectAlternativeName value must not be blank");
        }

        value = value.strip();

    }
}
