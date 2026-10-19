package toy.pki.ca.application.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.security.cert.Certificate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateIssuer;
import toy.pki.ca.adapter.persistence.memory.InMemoryMyCertificateRepository;
import toy.pki.ca.application.certificate.model.CertificateSearchCriteria;
import toy.pki.ca.application.certificate.model.CreateMyCertificateCommand;
import toy.pki.ca.application.profile.service.CertificateProfileService;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.certificate.MyCertificate;
import toy.pki.ca.domain.profile.ProfileId;

class MyCertificateServiceTest {

    private final MyCertificateService service = new MyCertificateService(new InMemoryMyCertificateRepository(),
        mock(CertificateProfileService.class), mock(BouncyCastleCertificateIssuer.class),
        mock(CertificateValidationService.class));
    private final ProfileId profileId = new ProfileId("root-ca-profile");
    private final Certificate certificate = mock(Certificate.class);

    @Test
    void createsIndependentRecordsAndPreservesCertificateMetadata() {
        CertificateId first = service.create(new CreateMyCertificateCommand("Root CA", "Original", profileId, certificate));
        CertificateId second = service.create(new CreateMyCertificateCommand("Second", null, profileId, certificate));

        assertThat(first).isNotEqualTo(second);
        assertThat(service.findAll(new CertificateSearchCriteria(null))).hasSize(2);
        var saved = service.findById(first.id());
        assertThat(saved.getCertificate()).isSameAs(certificate);
        assertThat(saved.getProfileId()).isEqualTo(profileId);
        assertThat(saved.getAlias()).isEqualTo("Root CA");
        assertThat(saved.getDescription()).isEqualTo("Original");
        assertThat(saved.getStatus()).isEqualTo(CertificateStatus.ACTIVE);
    }

    @Test
    void searchesByCertificateId() {
        CertificateId first = service.create(new CreateMyCertificateCommand("Root CA", "Production", profileId, certificate));
        service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));

        assertThat(service.findAll(new CertificateSearchCriteria(first.id()))).singleElement()
            .satisfies(found -> assertThat(found.getId()).isEqualTo(first));
    }

    @ParameterizedTest
    @ValueSource(strings = {"  ROOT ca  ", "oOt", "PRODUCTION", "duct"})
    void searchesByPartialAliasOrDescriptionIgnoringCaseAndWhitespace(String keyword) {
        CertificateId id = service.create(new CreateMyCertificateCommand("Root CA", "Production", profileId, certificate));
        service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));

        assertThat(service.findAll(new CertificateSearchCriteria(keyword))).singleElement()
            .satisfies(found -> assertThat(found.getId()).isEqualTo(id));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t\n"})
    void returnsAllCertificatesForAbsentOrBlankKeywords(String keyword) {
        CertificateId first = service.create(new CreateMyCertificateCommand("Root CA", "Production", profileId, certificate));
        CertificateId second = service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));

        assertThat(service.findAll(new CertificateSearchCriteria(keyword)))
            .extracting(MyCertificate::getId).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void returnsNoCertificatesWhenKeywordDoesNotMatch() {
        service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));

        assertThat(service.findAll(new CertificateSearchCriteria("missing"))).isEmpty();
    }

    @Test
    void deletesOnlyTheRequestedRecordAndReportsMissingIds() {
        CertificateId first = service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));
        CertificateId second = service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));

        service.delete(first.id());

        assertThat(service.findAll(new CertificateSearchCriteria(null))).singleElement()
            .satisfies(remaining -> assertThat(remaining.getId()).isEqualTo(second));
        assertThatThrownBy(() -> service.findById(first.id()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Certificate not found: " + first.id());
    }

    @Test
    void revokesAnExistingCertificateAndReportsMissingIds() {
        CertificateId id = service.create(new CreateMyCertificateCommand("To revoke", null, profileId, certificate));
        service.revoke(id.id());
        assertThat(service.findById(id.id()).getStatus()).isEqualTo(CertificateStatus.REVOKED);
        assertThat(service.findById(id.id()).getRevokedAt()).isNotNull();
        assertThatThrownBy(() -> service.revoke("missing")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void repeatedRevocationPreservesTheOriginalTimestampAndOtherCertificates() {
        CertificateId revoked = service.create(new CreateMyCertificateCommand("Revoked", null, profileId, certificate));
        CertificateId active = service.create(new CreateMyCertificateCommand("Active", null, profileId, certificate));
        service.revoke(revoked.id());
        var revokedAt = service.findById(revoked.id()).getRevokedAt();

        service.revoke(revoked.id());

        assertThat(service.findById(revoked.id()).getStatus()).isEqualTo(CertificateStatus.REVOKED);
        assertThat(service.findById(revoked.id()).getRevokedAt()).isEqualTo(revokedAt).isNotNull();
        assertThat(service.findById(active.id()).getStatus()).isEqualTo(CertificateStatus.ACTIVE);
        assertThat(service.findById(active.id()).getRevokedAt()).isNull();
    }

    @Test
    void expiresOnlyTheRequestedCertificate() {
        CertificateId expired = service.create(new CreateMyCertificateCommand("Expired", null, profileId, certificate));
        CertificateId active = service.create(new CreateMyCertificateCommand("Active", null, profileId, certificate));

        service.expire(expired.id());

        assertThat(service.findById(expired.id()).getStatus()).isEqualTo(CertificateStatus.EXPIRED);
        assertThat(service.findById(expired.id()).getRevokedAt()).isNull();
        assertThat(service.findById(active.id()).getStatus()).isEqualTo(CertificateStatus.ACTIVE);
    }

    @Test
    void expirationDoesNotOverrideRevocation() {
        CertificateId id = service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));
        service.revoke(id.id());
        var revokedAt = service.findById(id.id()).getRevokedAt();

        service.expire(id.id());

        assertThat(service.findById(id.id()).getStatus()).isEqualTo(CertificateStatus.REVOKED);
        assertThat(service.findById(id.id()).getRevokedAt()).isEqualTo(revokedAt);
    }

    @Test
    void expiringOrDeletingMissingCertificatesLeavesExistingRecordsUnchanged() {
        CertificateId id = service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));

        service.expire("missing");
        service.delete("missing");

        assertThat(service.findAll(new CertificateSearchCriteria(null))).singleElement()
            .satisfies(saved -> {
                assertThat(saved.getId()).isEqualTo(id);
                assertThat(saved.getStatus()).isEqualTo(CertificateStatus.ACTIVE);
            });
    }
}
