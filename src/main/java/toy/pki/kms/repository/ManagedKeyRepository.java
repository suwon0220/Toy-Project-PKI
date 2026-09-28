package toy.pki.kms.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import toy.pki.kms.domain.key.KeyID;
import toy.pki.kms.domain.key.ManagedKey;

@Repository
public class ManagedKeyRepository {

    private static final Map<KeyID, ManagedKey> store = new ConcurrentHashMap<>();

    public ManagedKey save(ManagedKey managedKey) {
        store.put(managedKey.getId(), managedKey);
        return managedKey;
    }

    public ManagedKey findByKeyID(KeyID id) {
        return store.get(id);
    }

    public List<ManagedKey> findAll() {
        return new ArrayList<>(store.values());
    }

    public void clearStore() {
        store.clear();
    }
}
