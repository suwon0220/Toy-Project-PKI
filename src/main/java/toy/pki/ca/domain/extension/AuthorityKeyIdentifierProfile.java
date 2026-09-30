package toy.pki.ca.domain.extension;

import org.bouncycastle.asn1.x509.GeneralNames;

import java.util.Optional;

// TODO: BC 의존성 제거
public record AuthorityKeyIdentifierProfile(
        boolean includeIssuerAndSerialNumber
) {
}
