package toy.pki.ca.domain.extension;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StandardAuthorityInformationAccessMethod {
    OCSP("1.3.6.1.5.5.7.48.1"),
    CA_ISSUERS("1.3.6.1.5.5.7.48.2");

    private final String oid;
}
