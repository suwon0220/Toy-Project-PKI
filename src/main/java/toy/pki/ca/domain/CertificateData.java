package toy.pki.ca.domain;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x509.Certificate;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Date;

@Data
@RequiredArgsConstructor
public class CertificateData {
    private BigInteger serialNumber;

    private Date notBefore;
    private Date notAfter;
}
