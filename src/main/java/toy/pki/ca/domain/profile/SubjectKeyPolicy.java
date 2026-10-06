package toy.pki.ca.domain.profile;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record SubjectKeyPolicy(
    @NotEmpty Set<SubjectKeySpec> allowedKeys) {

    public SubjectKeyPolicy {
        allowedKeys = Set.copyOf(allowedKeys);
    }

    public boolean allows(SubjectKeySpec parameters) {
        return allowedKeys.contains(parameters);
    }
}