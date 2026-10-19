package toy.pki.ca.domain.profile;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;

@Embeddable
public record ProfileId(
    @NotEmpty String value) {

}
