package toy.pki.ca.web.certificate;

import lombok.Data;
import toy.pki.ca.domain.certificate.CertificateStatus;

@Data
public class CertificateFilter {
    private String q;
    private CertificateStatus status;
    private String profileName;
}
