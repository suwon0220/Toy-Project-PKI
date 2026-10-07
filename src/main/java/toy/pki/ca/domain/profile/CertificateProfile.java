package toy.pki.ca.domain.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.policy.DnPolicy;
import toy.pki.ca.domain.policy.SanPolicy;
import toy.pki.ca.domain.policy.SubjectKeyPolicy;

@Data
@AllArgsConstructor
public class CertificateProfile {
    // Unique identifier for the certificate profile
    @NotNull
    @Valid
    private final ProfileId id;
    @Size(min = 1, message = "alias는 null이 아닐 경우 비어있을 수 없습니다")
    private String alias;
    @Size(min = 1, message = "description은 null이 아닐 경우 비어있을 수 없습니다")
    private String description;
    // Status of the certificate profile
    @NotNull private ProfileStatus status;
    // Validity period in days
    @Min(1) private int defaultValidityDays;
    @Min(1) private int maxValidityDays;
    // Subject Key Policy
    @NotNull
    @Valid
    private SubjectKeyPolicy subjectKeyPolicy;
    private Set<CertificateSignatureAlgorithm> allowedSignatures;
    // SAN Policy
    @NotNull private SanPolicy sanPolicy;
    // Subject Policy
    private DnPolicy dnPolicy;
    // Basic Constraints
    private CertificateType certificateType;
    private Integer pathLenConstraint;
    // Key Usage
    @NotEmpty private Set<@NotNull KeyUsage> keyUsages;
    @NotNull private Set<@NotNull @Valid ExtendedKeyUsageOid> extendedKeyUsages;

    public CertificateProfile(
        ProfileId id,
        String alias,
        String description,
        int defaultValidityDays,
        int maxValidityDays,
        SubjectKeyPolicy subjectKeyPolicy,
        Set<CertificateSignatureAlgorithm> allowedSignatures,
        SanPolicy sanPolicy,
        DnPolicy dnPolicy,
        CertificateType certificateType,
        Integer pathLenConstraint,
        Set<KeyUsage> keyUsages,
        Set<ExtendedKeyUsageOid> extendedKeyUsages) {
        this(id, alias, description, ProfileStatus.DRAFT, defaultValidityDays, maxValidityDays,
            subjectKeyPolicy, allowedSignatures, sanPolicy, dnPolicy, certificateType, pathLenConstraint,
            keyUsages, extendedKeyUsages);
    }

    @AssertTrue(message = "defaultValidityDays는 maxValidityDays보다 작거나 같아야 합니다")
    public boolean isValidValidityRange() {
        return defaultValidityDays <= maxValidityDays;
    }

    @AssertTrue(message = "CA가 아닌 프로파일에는 KEY_CERT_SIGN을 설정할 수 없습니다")
    public boolean isValidCaKeyUsage() {
        return keyUsages == null || certificateType != CertificateType.END_ENTITY || !keyUsages.contains(KeyUsage.KEY_CERT_SIGN);
    }

    @AssertTrue(message = "pathLenConstraint는 CA이며 KEY_CERT_SIGN이 설정된 경우에만 0 이상으로 지정할 수 있습니다")
    public boolean isValidPathLenConstraint() {
        return pathLenConstraint == null
            || (certificateType != CertificateType.END_ENTITY && pathLenConstraint >= 0
            && keyUsages != null && keyUsages.contains(KeyUsage.KEY_CERT_SIGN));
    }

    @AssertTrue(message = "ENCIPHER_ONLY 또는 DECIPHER_ONLY를 사용하려면 KEY_AGREEMENT가 필요합니다")
    public boolean isValidKeyAgreementUsage() {
        return keyUsages == null
            || (!keyUsages.contains(KeyUsage.ENCIPHER_ONLY)
            && !keyUsages.contains(KeyUsage.DECIPHER_ONLY))
            || keyUsages.contains(KeyUsage.KEY_AGREEMENT);
    }

    @AssertTrue(message = "Extended Key Usage와 Key Usage의 용도가 호환되어야 합니다")
    public boolean isValidExtendedKeyUsages() {
        if (extendedKeyUsages == null || keyUsages == null) {
            return true; // null은 필드의 제약 조건에서 검증합니다.
        }
        return extendedKeyUsages.stream()
                                .filter(Objects::nonNull)
                                .allMatch(eku -> eku.isCompatibleWith(keyUsages));
    }

    public void activate() {
        this.status = ProfileStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProfileStatus.INACTIVE;
    }

    public CertificateProfile duplicate() {
        return new CertificateProfile(
            new ProfileId(UUID.randomUUID().toString()), // 새로운 ID 생성
            this.alias + " (복제)", // 복제된 프로파일의 alias를 변경
            this.description,
            this.defaultValidityDays,
            this.maxValidityDays,
            this.subjectKeyPolicy,
            Set.copyOf(this.allowedSignatures),
            this.sanPolicy,
            this.dnPolicy,
            this.certificateType,
            this.pathLenConstraint,
            Set.copyOf(this.keyUsages),
            Set.copyOf(this.extendedKeyUsages));
    }
}
