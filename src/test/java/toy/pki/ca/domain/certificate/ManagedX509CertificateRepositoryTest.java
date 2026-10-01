package toy.pki.ca.domain.certificate;

import static org.assertj.core.api.Assertions.*;

import java.math.BigInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ManagedX509CertificateRepositoryTest {

    private final ManagedX509CertificateRepository managedX509CertificateRepository = new ManagedX509CertificateRepository();

    @AfterEach
    void tearDown() {
        managedX509CertificateRepository.clear();
    }

    @Test
    void save() {
        ManagedX509Certificate certificate = new ManagedX509Certificate();
        Long serialNumber = managedX509CertificateRepository.save(certificate);
        assertThat(serialNumber).isEqualTo(1L);
    }
}