package toy.pki.ca.adapter.persistence.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import toy.pki.ca.application.certificate.model.CertificateSearchCriteria;
import toy.pki.ca.application.certificate.port.MyCertificateRepository;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.MyCertificate;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMyCertificateProfileRepository implements MyCertificateRepository {

    private final SpringDataJpaMyCertificateRepository springDataJpaMyCertificateRepository;

    @Override
    public void save(MyCertificate certificate) {
        springDataJpaMyCertificateRepository.save(certificate);
    }

    @Override
    public Optional<MyCertificate> findById(CertificateId certificateId) {
        return springDataJpaMyCertificateRepository.findById(certificateId);
    }

    @Override
    public List<MyCertificate> findAll(CertificateSearchCriteria criteria) {
        String keyword = criteria.keyword();
        if (keyword == null) {
            return springDataJpaMyCertificateRepository.findAll();
        }
        return springDataJpaMyCertificateRepository
                .findAllByAliasContainingIgnoreCaseOrProfileId_ValueContainingIgnoreCase(keyword, keyword);
    }

    @Override
    public void delete(CertificateId certificateId) {
        springDataJpaMyCertificateRepository.deleteById(certificateId);
    }
}
