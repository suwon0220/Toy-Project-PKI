package toy.pki.ca.adapter.persistence.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import toy.pki.ca.application.model.ProfileSearchCriteria;
import toy.pki.ca.application.port.CertificateProfileRepository;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ProfileId;

@Repository
@RequiredArgsConstructor
public class InMemoryCertificateProfileRepository implements CertificateProfileRepository {

    private final Map<ProfileId, CertificateProfile> store = new ConcurrentHashMap<>();

    @Override
    public void save(CertificateProfile profile) {
        store.put(profile.getId(), profile);
    }

    @Override
    public Optional<CertificateProfile> findById(ProfileId profileId) {
        return Optional.ofNullable(store.get(profileId));
    }

    @Override
    public List<CertificateProfile> findByCriteria(ProfileSearchCriteria criteria) {
        return store.values()
            .stream()
            .filter(criteria.toPredicate())
            .toList();
    }

    @Override
    public List<CertificateProfile> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(ProfileId profileId) {
        store.remove(profileId);
    }
}
