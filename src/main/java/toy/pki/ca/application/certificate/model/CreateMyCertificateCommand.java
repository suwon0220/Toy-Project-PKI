package toy.pki.ca.application.certificate.model;

import java.security.cert.Certificate;
import lombok.NonNull;
import toy.pki.ca.domain.profile.ProfileId;

public record CreateMyCertificateCommand(
    String alias,
    String description,
    @NonNull ProfileId profileId,
    @NonNull Certificate certificate) {
}
