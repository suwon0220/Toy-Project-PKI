package toy.pki.ca.adapter.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.MyCertificate;

import java.util.List;

public interface SpringDataJpaMyCertificateRepository extends JpaRepository<MyCertificate, CertificateId> {
    List<MyCertificate> findAllByAliasContainingIgnoreCaseOrProfileId_ValueContainingIgnoreCase(
        String alias, String profileId);
}
