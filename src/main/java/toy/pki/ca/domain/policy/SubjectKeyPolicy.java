package toy.pki.ca.domain.policy;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import toy.pki.ca.domain.profile.SubjectKeySpec;

public record SubjectKeyPolicy(
    @NotEmpty Set<SubjectKeySpec> allowedKeys) {

    public SubjectKeyPolicy {
        allowedKeys = Set.copyOf(allowedKeys);
    }

    public boolean allows(SubjectKeySpec parameters) {
        return allowedKeys.contains(parameters);
    }
}