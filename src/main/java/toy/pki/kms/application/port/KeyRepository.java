package toy.pki.kms.application.port;

import java.util.Optional;
import toy.pki.kms.domain.key.KeyId;
import toy.pki.kms.domain.key.ManagedKey;

public interface KeyRepository {
    void save(ManagedKey managedKey);

    Optional<ManagedKey> findById(KeyId keyId);

    void delete(KeyId keyId);
}
