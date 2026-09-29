package toy.pki.ca.domain.profile;

import java.util.Optional;
import java.util.UUID;

public record ProfileId(
        UUID id,
        String alias,
        Optional<String> description
) {
}
