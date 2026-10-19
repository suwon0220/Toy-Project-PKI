package toy.pki.ca.adapter.persistence.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import toy.pki.ca.application.profile.model.ProfileSearchCriteria;
import toy.pki.ca.application.profile.port.CertificateProfileRepository;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ProfileId;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaCertificateProfileRepository implements CertificateProfileRepository {

    private final SpringDataJpaCertificateProfileRepository springDataJpaCertificateProfileRepository;

    @Override
    public void save(CertificateProfile profile) {
        springDataJpaCertificateProfileRepository.save(profile);
    }

    @Override
    public Optional<CertificateProfile> findById(ProfileId profileId) {
        return springDataJpaCertificateProfileRepository.findById(profileId);
    }

    @Override
    public List<CertificateProfile> findAll(ProfileSearchCriteria criteria) {
        return springDataJpaCertificateProfileRepository.findAllByAliasContainingIgnoreCase((criteria.keyword() == null) ? "" : criteria.keyword());
    }

    @Override
    public void delete(ProfileId profileId) {
        springDataJpaCertificateProfileRepository.deleteById(profileId);
    }
}
