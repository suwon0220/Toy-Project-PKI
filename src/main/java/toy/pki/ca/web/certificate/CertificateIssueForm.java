package toy.pki.ca.web.certificate;

import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.SubjectKeySpec;

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
    private SubjectKeySpec subjectKeySpec;
    private String kmsKeyId;

    // 발급 요청의 실제 DN 값. 검증 규칙은 프로파일의 DnPolicy에 둡니다.
    private SubjectDnForm subjectDn = new SubjectDnForm();

    @Data
    public static class SubjectDnForm {
        private String domainComponent;
        private String commonName;
        private String organization;
        private String organizationUnit;
        private String country;
    }
}
