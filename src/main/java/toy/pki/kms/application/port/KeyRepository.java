package toy.pki.kms.application.port;

import java.util.List;
import java.util.Optional;

import toy.pki.kms.application.model.KeySearchCriteria;
import toy.pki.kms.domain.key.KeyId;
import toy.pki.kms.domain.key.ManagedKey;

public interface KeyRepository {
    List<ManagedKey> findByCriteria(KeySearchCriteria criteria);

    List<ManagedKey> findAll();

    void save(ManagedKey managedKey);

    Optional<ManagedKey> findById(KeyId keyId);

    void delete(KeyId keyId);
}
