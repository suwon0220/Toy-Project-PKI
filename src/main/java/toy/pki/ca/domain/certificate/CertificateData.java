package toy.pki.ca.domain.certificate;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigInteger;
import java.util.Date;

@Data
@RequiredArgsConstructor
public class CertificateData {
    private BigInteger serialNumber;

    private Date notBefore;
    private Date notAfter;
}
