package toy.pki.ca.web.certificate.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bouncycastle.asn1.ocsp.CertStatus;
import toy.pki.ca.domain.certificate.CertType;


@Data
@AllArgsConstructor
public class CertificateSummary {
    String serial;
    String subjectCn;
//    CertType certType;
//    String issuerSerial;
//    String issuerCn;
//    String profileName;
//    Instant notAfter;
//    CertStatus status;
//    long daysLeft;
}
