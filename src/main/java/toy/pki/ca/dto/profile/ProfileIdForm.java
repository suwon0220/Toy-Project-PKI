package toy.pki.ca.dto.profile;

import java.util.Optional;
import java.util.UUID;

public record ProfileIdForm(
        String alias,
        Optional<String> description
) {
}
