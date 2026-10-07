package toy.pki.ca.application.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.security.cert.Certificate;
import org.junit.jupiter.api.Test;
import toy.pki.ca.adapter.persistence.memory.InMemoryMyCertificateRepository;
import toy.pki.ca.adapter.certificate.bouncycastle.BouncyCastleCertificateIssuer;
import toy.pki.ca.application.profile.service.CertificateProfileService;
import toy.pki.ca.application.certificate.model.CertificateSearchCriteria;
import toy.pki.ca.application.certificate.model.CreateMyCertificateCommand;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.profile.ProfileId;

class MyCertificateServiceTest {

    private final MyCertificateService service = new MyCertificateService(new InMemoryMyCertificateRepository(),
        mock(CertificateProfileService.class), mock(BouncyCastleCertificateIssuer.class));
    private final ProfileId profileId = new ProfileId("root-ca-profile");
    private final Certificate certificate = mock(Certificate.class);

    @Test
    void createsIndependentRecordsAndPreservesCertificateMetadata() {
        CertificateId first = service.create(new CreateMyCertificateCommand("Root CA", "Original", profileId, certificate));
        CertificateId second = service.create(new CreateMyCertificateCommand("Second", null, profileId, certificate));

        assertThat(first).isNotEqualTo(second);
        assertThat(service.findAll()).hasSize(2);
        var saved = service.findById(first.id());
        assertThat(saved.getCertificate()).isSameAs(certificate);
        assertThat(saved.getProfileId()).isEqualTo(profileId);
        assertThat(saved.getAlias()).isEqualTo("Root CA");
        assertThat(saved.getDescription()).isEqualTo("Original");
        assertThat(saved.getStatus()).isEqualTo(CertificateStatus.ACTIVE);
    }

    @Test
    void searchesByIdAliasOrDescriptionAndAllowsEmptySearch() {
        CertificateId first = service.create(new CreateMyCertificateCommand("Root CA", "Production", profileId, certificate));
        service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));

        for (String keyword : new String[] {first.id(), "  ROOT ca  ", "PRODUCTION"}) {
            assertThat(service.search(new CertificateSearchCriteria(keyword))).singleElement()
                .satisfies(found -> assertThat(found.getId()).isEqualTo(first));
        }
        assertThat(service.search(new CertificateSearchCriteria("  "))).hasSize(2);
        assertThat(service.search(new CertificateSearchCriteria(null))).hasSize(2);
        assertThat(service.search(new CertificateSearchCriteria("missing"))).isEmpty();
    }

    @Test
    void deletesOnlyTheRequestedRecordAndReportsMissingIds() {
        CertificateId first = service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));
        CertificateId second = service.create(new CreateMyCertificateCommand(null, null, profileId, certificate));

        service.delete(first.id());

        assertThat(service.findAll()).singleElement()
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
}
