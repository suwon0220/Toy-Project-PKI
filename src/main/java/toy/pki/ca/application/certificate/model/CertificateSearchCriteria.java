package toy.pki.ca.application.certificate.model;

import java.util.Locale;

public record CertificateSearchCriteria(
    String keyword) {

    public CertificateSearchCriteria {
        keyword = keyword == null || keyword.isBlank()
            ? null
            : keyword.strip().toLowerCase(Locale.ROOT);
    }
}
