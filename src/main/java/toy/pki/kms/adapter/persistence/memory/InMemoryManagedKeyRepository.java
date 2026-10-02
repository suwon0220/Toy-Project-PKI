package toy.pki.kms.adapter.persistence.memory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import toy.pki.kms.application.port.KeyRepository;
import toy.pki.kms.domain.key.KeyId;
import toy.pki.kms.domain.key.ManagedKey;

@Slf4j
@Repository
public class InMemoryManagedKeyRepository implements KeyRepository {

    private final Map<KeyId, ManagedKey> store = new ConcurrentHashMap<>();


    @Override
    public void save(ManagedKey managedKey) {
        if(store.putIfAbsent(managedKey.keyId(), managedKey) != null) {
            throw new IllegalStateException("Key with id " + managedKey.keyId() + " already exists");
        }
    }

    @Override
    public Optional<ManagedKey> findById(KeyId keyId) {
        return Optional.ofNullable(store.get(keyId));
    }

    @Override
    public void delete(KeyId keyId) {
        store.remove(keyId);
    }
}
