package toy.pki.ca.domain.policy;

import java.util.Set;

import jakarta.persistence.Embeddable;
import toy.pki.ca.domain.profile.SanType;

@Embeddable
public record SanPolicy(
    boolean required,
    Set<SanType> allowedTypes) {

}
