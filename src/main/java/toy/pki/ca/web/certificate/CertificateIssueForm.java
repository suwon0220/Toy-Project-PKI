package toy.pki.ca.web.certificate;

import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.kms.web.KeyAlgorithmPreset;

@Data
@NoArgsConstructor
public class CertificateIssueForm {
    private String alias;
    private String description;

    private ProfileId profileId;
    private CertificateId issuerCertificateId;

    @Min(1)
    private Integer validityDays;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime notBefore;

    private KeyMode keyMode;
    private String keyAlias;
    private KeyAlgorithmPreset keyAlgorithmPreset;
    private String kmsKeyId;

    private SubjectDnForm subjectDn = new SubjectDnForm();

    @Data
    public static class SubjectDnForm {
        private String commonName;
        private String organization;
        private String organizationalUnit;
        private String locality;
        private String stateOrProvince;
        private String country;
    }
}
