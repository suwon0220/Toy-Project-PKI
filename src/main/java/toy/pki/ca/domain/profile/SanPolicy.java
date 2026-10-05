package toy.pki.ca.domain.profile;

import java.util.Set;

public record SanPolicy(
    boolean required,
    Set<SanType> allowedTypes) {

}
