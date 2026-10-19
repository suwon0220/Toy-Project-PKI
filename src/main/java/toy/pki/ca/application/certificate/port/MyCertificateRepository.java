package toy.pki.ca.application.certificate.port;

import java.util.List;
import java.util.Optional;
import toy.pki.ca.application.certificate.model.CertificateSearchCriteria;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.MyCertificate;

public interface MyCertificateRepository {
    void save(MyCertificate certificate);

    Optional<MyCertificate> findById(CertificateId certificateId);

    List<MyCertificate> findAll(CertificateSearchCriteria criteria);

    void delete(CertificateId certificateId);
}
