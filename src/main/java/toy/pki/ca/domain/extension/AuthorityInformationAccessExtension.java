package toy.pki.ca.domain.extension;

// TODO: BC 의존성 삭제
import org.bouncycastle.asn1.x509.GeneralName;

public record AuthorityInformationAccessExtension(
        AuthorityInformationAccessMethod method,
        GeneralName location
) {
}
