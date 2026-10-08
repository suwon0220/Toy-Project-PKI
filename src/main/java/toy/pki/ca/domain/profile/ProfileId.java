package toy.pki.ca.domain.profile;

import jakarta.validation.constraints.NotBlank;

public record ProfileId(
    @NotBlank String value) {

}
