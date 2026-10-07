package toy.pki.ca.adapter.persistence.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import toy.pki.ca.application.certificate.model.CertificateSearchCriteria;
import toy.pki.ca.application.certificate.port.MyCertificateRepository;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.MyCertificate;

@Repository
public class InMemoryMyCertificateRepository implements MyCertificateRepository {

    private final Map<CertificateId, MyCertificate> store = new ConcurrentHashMap<>();

    @Override
    public void save(MyCertificate certificate) {
        store.put(certificate.getId(), certificate);
    }

    @Override
    public Optional<MyCertificate> findById(CertificateId certificateId) {
        return Optional.ofNullable(store.get(certificateId));
    }

    @Override
    public List<MyCertificate> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(CertificateId certificateId) {
        store.remove(certificateId);
    }

    @Override
    public List<MyCertificate> findByCriteria(CertificateSearchCriteria criteria) {
        String keyword = criteria.keyword();
        return store.values().stream()
                    .filter(certificate -> keyword == null
                        || contains(certificate.getId().id(), keyword)
                        || contains(certificate.getAlias(), keyword)
                        || contains(certificate.getDescription(), keyword))
                    .toList();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
