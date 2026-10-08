package toy.pki.ca.domain.policy;

import java.util.Set;
import toy.pki.ca.domain.profile.SanType;

public record SanPolicy(
    boolean required,
    Set<SanType> allowedTypes) {

}
