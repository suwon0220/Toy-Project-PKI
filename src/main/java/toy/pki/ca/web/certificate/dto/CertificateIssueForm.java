package toy.pki.ca.web.certificate.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import toy.pki.ca.domain.certificate.DistinguishedName;
import toy.pki.kms.domain.algorithm.KeyAlgorithm;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

@Data
public class CertificateIssueForm {
    private Long issuerCertificateId;

    @NotNull 
    private Long profileId;

    private Integer validityDays;

    @NotNull 
    private LocalDateTime notBefore;

    @NotNull
    private KeyMode keyMode;

    // If keyMode is NEW {
    private String keyAlias;
    private KeyAlgorithmPreset keyAlgorithmPreset;
    // }

    // If keyMode is EXISTING {
    private Long kmsKeyId;
    // }

    @NotNull
    private DistinguishedName subjectDn;
}
