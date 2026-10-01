package toy.pki.ca.domain.certificate;

import java.security.cert.X509Certificate;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bouncycastle.asn1.ocsp.CertStatus;
import org.bouncycastle.cert.ocsp.CertificateStatus;

@Data
@NoArgsConstructor
public class ManagedX509Certificate {
    private Long id;
    private String displayName;
    private X509Certificate certificate;
}
