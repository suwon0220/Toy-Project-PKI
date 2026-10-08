package toy.pki.ca.application.profile.model;

import java.util.Locale;
import java.util.function.Predicate;

import toy.pki.ca.domain.profile.CertificateProfile;

public record ProfileSearchCriteria(
    String keyword) {

    public ProfileSearchCriteria {
        keyword = keyword == null || keyword.isBlank()
                  ? null
                  : keyword.strip().toLowerCase(Locale.ROOT);
    }

    public Predicate<CertificateProfile> toPredicate() {
        throw new UnsupportedOperationException("Unimplemented method 'toPredicate'");
    }

}
