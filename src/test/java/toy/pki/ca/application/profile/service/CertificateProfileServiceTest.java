package toy.pki.ca.application.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import toy.pki.ca.adapter.persistence.memory.InMemoryCertificateProfileRepository;
import toy.pki.ca.application.profile.model.CreateProfileCommand;
import toy.pki.ca.application.profile.model.UpdateProfileCommand;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.policy.DnAttributePolicy;
import toy.pki.ca.domain.policy.DnPolicy;
import toy.pki.ca.domain.policy.SanPolicy;
import toy.pki.ca.domain.policy.SubjectKeyPolicy;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.domain.profile.SanType;
import toy.pki.ca.domain.profile.SubjectKeySpec;

class CertificateProfileServiceTest {

    private final CertificateProfileService service = new CertificateProfileService(new InMemoryCertificateProfileRepository());
    private final SubjectKeyPolicy keyPolicy = new SubjectKeyPolicy(Set.of(SubjectKeySpec.EC_P256));
    private final DnPolicy dnPolicy = new DnPolicy(new DnAttributePolicy("V2G"), new DnAttributePolicy(true),
        new DnAttributePolicy(false), new DnAttributePolicy("Example"), new DnAttributePolicy("KR"));

    @Test
    void createsIndependentDraftsAndPreservesTheirPolicies() {
        ProfileId first = create();
        ProfileId second = create();
        CertificateProfile profile = service.findById(first.value());
        assertThat(first).isNotEqualTo(second);
        assertThat(service.findAll()).hasSize(2);
        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.DRAFT);
        assertThat(profile.getAlias()).isEqualTo("Root CA");
        assertThat(profile.getDefaultValidityDays()).isEqualTo(30);
        assertThat(profile.getMaxValidityDays()).isEqualTo(365);
        assertThat(profile.getSubjectKeyPolicy()).isEqualTo(keyPolicy);
        assertThat(profile.getAllowedSignatures()).containsExactly(CertificateSignatureAlgorithm.ECDSA_WITH_SHA256);
        assertThat(profile.getDnPolicy()).isEqualTo(dnPolicy);
        assertThat(profile.getCertificateType()).isEqualTo(CertificateType.ROOT_CA);
        assertThat(profile.getKeyUsages()).containsExactly(KeyUsage.KEY_CERT_SIGN);
    }

    @Test
    void updatesDraftSettingsAndNestedDnPolicy() {
        ProfileId id = create();
        UpdateProfileCommand command = update(id);
        service.updateDraft(command);
        CertificateProfile profile = service.findById(id.value());
        assertThat(profile.getAlias()).isEqualTo("Updated CA");
        assertThat(profile.getDefaultValidityDays()).isEqualTo(60);
        assertThat(profile.getMaxValidityDays()).isEqualTo(180);
        assertThat(profile.getDnPolicy()).isEqualTo(command.dnPolicy());
        assertThat(profile.getSanPolicy()).isEqualTo(command.sanPolicy());
        assertThat(profile.getKeyUsages()).containsExactlyInAnyOrder(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN);
        assertThat(profile.getCertificateType()).isEqualTo(CertificateType.INTERMEDIATE_CA);
        assertThat(profile.getPathLenConstraint()).isZero();
        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.DRAFT);
    }

    @ParameterizedTest
    @EnumSource(value = ProfileStatus.class, names = {"ACTIVE", "INACTIVE"})
    void rejectsUpdatesOutsideDraftWithoutChangingTheProfile(ProfileStatus status) {
        ProfileId id = create();
        service.findById(id.value()).setStatus(status);
        assertThatThrownBy(() -> service.updateDraft(update(id))).isInstanceOf(IllegalStateException.class);
        assertThat(service.findById(id.value()).getAlias()).isEqualTo("Root CA");
        assertThat(service.findById(id.value()).getStatus()).isEqualTo(status);
    }

    @Test
    void duplicatesAnActiveProfileAsAnIndependentDraft() {
        ProfileId id = create();
        service.activate(id.value());
        service.duplicateDraft(id.value());
        CertificateProfile original = service.findById(id.value());
        CertificateProfile copy = service.findAll().stream().filter(profile -> !profile.getId().equals(id)).findFirst().orElseThrow();
        assertThat(copy.getStatus()).isEqualTo(ProfileStatus.DRAFT);
        assertThat(copy.getAlias()).isEqualTo("Root CA (복제)");
        assertThat(copy.getDnPolicy()).isEqualTo(original.getDnPolicy());
        assertThat(copy.getAllowedSignatures()).isEqualTo(original.getAllowedSignatures());
        copy.setAlias("Changed copy");
        copy.setKeyUsages(Set.of(KeyUsage.CRL_SIGN));
        assertThat(original.getAlias()).isEqualTo("Root CA");
        assertThat(original.getKeyUsages()).containsExactly(KeyUsage.KEY_CERT_SIGN);
        assertThat(original.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
    }

    @Test
    void changesStatusAndDeletesOnlyTheSelectedProfile() {
        ProfileId first = create();
        ProfileId second = create();
        service.activate(first.value());
        assertThat(service.findById(first.value()).getStatus()).isEqualTo(ProfileStatus.ACTIVE);
        service.deactivate(first.value());
        assertThat(service.findById(first.value()).getStatus()).isEqualTo(ProfileStatus.INACTIVE);
        service.delete(first.value());
        assertThat(service.findAll()).singleElement().satisfies(profile -> assertThat(profile.getId()).isEqualTo(second));
        assertThatThrownBy(() -> service.findById(first.value())).isInstanceOf(IllegalArgumentException.class);
    }

    private ProfileId create() {
        return service.createDraft(new CreateProfileCommand("Root CA", "Profile", 30, 365, keyPolicy,
            Set.of(CertificateSignatureAlgorithm.ECDSA_WITH_SHA256), new SanPolicy(false, Set.of()), dnPolicy,
            CertificateType.ROOT_CA, 1, Set.of(KeyUsage.KEY_CERT_SIGN), Set.of()));
    }

    private UpdateProfileCommand update(ProfileId id) {
        return new UpdateProfileCommand(id.value(), "Updated CA", "Updated profile", 60, 180, keyPolicy,
            new SanPolicy(true, Set.of(SanType.DNS_NAME)),
            new DnPolicy(null, new DnAttributePolicy("Fixed CA"), null, null, new DnAttributePolicy("DE")),
            CertificateType.INTERMEDIATE_CA, 0, Set.of(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN), Set.of());
    }
}
