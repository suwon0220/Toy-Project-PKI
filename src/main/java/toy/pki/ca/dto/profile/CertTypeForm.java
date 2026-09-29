package toy.pki.ca.dto.profile;

import jakarta.validation.constraints.NotNull;
import toy.pki.ca.domain.CertType;

public record CertTypeForm(
    @NotNull CertType certType) {
}
