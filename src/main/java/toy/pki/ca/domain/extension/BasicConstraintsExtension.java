package toy.pki.ca.domain.extension;

import java.util.Optional;

public record BasicConstraintsExtension (
        boolean critical,
        boolean ca,
        Optional<Integer> pathLenConstraint
){
}
