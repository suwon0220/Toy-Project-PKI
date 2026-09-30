package toy.pki.ca.web.certificate;

import lombok.Data;

/**
 * CertificateFilter
 */
@Data
public class CertificateFilter {
    private String caId;
    private String q;
    private String status;
    private Integer expiringWithinDays;
}