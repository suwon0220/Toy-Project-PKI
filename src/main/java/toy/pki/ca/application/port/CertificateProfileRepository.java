package toy.pki.ca.application.port;

import java.util.List;
import java.util.Optional;

import toy.pki.ca.application.model.ProfileSearchCriteria;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ProfileId;

public interface CertificateProfileRepository {
    void save(CertificateProfile profile);

    Optional<CertificateProfile> findById(ProfileId profileId);
    List<CertificateProfile> findByCriteria(ProfileSearchCriteria criteria);

    List<CertificateProfile> findAll();

    void delete(ProfileId profileId);
}