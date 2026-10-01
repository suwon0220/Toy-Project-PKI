package toy.pki.ca.web.certificate.dto;

import lombok.Data;

/**
 * CertificateFilter
 */
@Data
public class CertificateFilter {
    private String q;
    private String status;
    private String profileName;
}