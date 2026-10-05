package toy.pki.ca.domain.profile;

import jakarta.validation.constraints.NotBlank;

/**
 * ProfileId
 */
public record ProfileId(
    @NotBlank String value) {

}
