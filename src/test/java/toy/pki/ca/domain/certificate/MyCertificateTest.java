package toy.pki.ca.domain.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.security.cert.Certificate;
import org.junit.jupiter.api.Test;
import toy.pki.ca.domain.profile.ProfileId;

class MyCertificateTest {

    private final MyCertificate certificate = new MyCertificate(new CertificateId("certificate"),
        new ProfileId("profile"), mock(Certificate.class));

    @Test
    void normalizesAliasAndTreatsBlankAsAbsent() {
        certificate.setAlias("  Root CA  ");
        assertThat(certificate.getAlias()).isEqualTo("Root CA");
        certificate.setAlias(" \t\n");
        assertThat(certificate.getAlias()).isNull();
        certificate.setAlias(null);
        assertThat(certificate.getAlias()).isNull();
    }

    @Test
    void revocationPreservesItsTimestampAndCannotBeUndoneByExpirationOrSuspension() {
        certificate.revoke();
        var revokedAt = certificate.getRevokedAt();
        certificate.revoke();
        certificate.expire();
        assertThat(certificate.getStatus()).isEqualTo(CertificateStatus.REVOKED);
        assertThat(certificate.getRevokedAt()).isEqualTo(revokedAt).isNotNull();
        assertThatThrownBy(certificate::suspend).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void revocationKeepsTheOriginalSuspensionTime() {
        certificate.suspend();
        var heldAt = certificate.getRevokedAt();
        assertThat(certificate.getStatus()).isEqualTo(CertificateStatus.SUSPENDED);
        certificate.revoke();
        assertThat(certificate.getStatus()).isEqualTo(CertificateStatus.REVOKED);
        assertThat(certificate.getRevokedAt()).isEqualTo(heldAt).isNotNull();
    }
}
