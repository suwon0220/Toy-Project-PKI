package toy.pki.ca.web.certificate.dto;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.Data;
import toy.pki.ca.domain.certificate.CertType;
import toy.pki.ca.domain.certificate.DistinguishedName;
import toy.pki.kms.domain.algorithm.KeyAlgorithm;

@Data
public class CertificatePreview {
    private CertType certType;
    private String subjectDn;
    private String issuerDn;
    private Date notBefore;
    private Date notAfter;

    // KMS Key
    private KeyAlgorithm keyAlgorithm;
    private String keyLabel;
    private String signatureAlgorithm;
    private String signerName;
    private List<String> keyUsages;
    private List<String> extKeyUsages;
    private String basicConstraints;
    private String aiaOcspUrl;
}
