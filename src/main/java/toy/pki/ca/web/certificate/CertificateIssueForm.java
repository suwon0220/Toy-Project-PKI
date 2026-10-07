package toy.pki.ca.web.certificate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.application.certificate.model.IssueCertificateCommand;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.certificate.CertificateSubject;
import toy.pki.ca.domain.certificate.SubjectAlternativeName;
import toy.pki.ca.domain.profile.SanType;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.SubjectKeySpec;
import toy.pki.kms.domain.KeyId;

@Data
@NoArgsConstructor
public class CertificateIssueForm {
    @Size(max = 128) private String alias;
    @Size(max = 1000) private String description;

    @Valid
    @NotNull(message = "{certificate.profile.required}")
    private ProfileId profileId;
    private CertificateId issuerCertificateId;

    @Min(1)
    private Integer validityDays;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime notBefore;

    @NotNull(message = "{certificate.keyMode.required}")
    private KeyMode keyMode = KeyMode.NEW;
    private SubjectKeySpec subjectKeySpec;
    private String kmsKeyId;
    private CertificateSignatureAlgorithm signatureAlgorithm;
    @Size(max = 8192) private String sanDnsNames;
    @Size(max = 8192) private String sanIpAddresses;
    @Size(max = 8192) private String sanEmailAddresses;
    @Size(max = 8192) private String sanUris;

    // 발급 요청의 실제 DN 값. 검증 규칙은 프로파일의 DnPolicy에 둡니다.
    @Valid
    @NotNull
    private SubjectDnForm subjectDn = new SubjectDnForm();

    public IssueCertificateCommand toCommand(KeyId keyId, ZoneId zone) {
        Set<SubjectAlternativeName> sans = new LinkedHashSet<>();
        addSans(sans, SanType.DNS_NAME, sanDnsNames);
        addSans(sans, SanType.IP_ADDRESS, sanIpAddresses);
        addSans(sans, SanType.EMAIL_ADDRESS, sanEmailAddresses);
        addSans(sans, SanType.URI, sanUris);
        return new IssueCertificateCommand(alias, description, profileId, keyId, issuerCertificateId,
            new CertificateSubject(subjectDn.domainComponent, subjectDn.commonName, subjectDn.organizationUnit,
                subjectDn.organization, subjectDn.country), sans, validityDays,
            notBefore == null ? null : notBefore.atZone(zone).toInstant(), signatureAlgorithm);
    }

    private void addSans(Set<SubjectAlternativeName> names, SanType type, String values) {
        if (values != null) values.lines().filter(value -> !value.isBlank())
            .map(value -> new SubjectAlternativeName(type, value)).forEach(names::add);
    }

    @Data
    public static class SubjectDnForm {
        @Size(max = 255) private String domainComponent;
        @Size(max = 64) private String commonName;
        @Size(max = 64) private String organization;
        @Size(max = 128) private String organizationUnit;
        @Size(max = 2) private String country;
    }
}
