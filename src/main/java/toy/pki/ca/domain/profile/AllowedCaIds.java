package toy.pki.ca.domain.profile;

import java.util.Set;

public record AllowedCaIds(
        Set<String> allowedCaIds
) {
}
