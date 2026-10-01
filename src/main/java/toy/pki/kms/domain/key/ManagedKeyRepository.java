package toy.pki.kms.domain.key;

import lombok.NonNull;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ManagedKeyRepository {

    private static final Map<KeyID, ManagedKey> store = new ConcurrentHashMap<>();

    public ManagedKey save(@NonNull ManagedKey managedKey) {
        store.put(managedKey.getId(), managedKey);
        return managedKey;
    }

    public ManagedKey findByKeyID(@NonNull KeyID id) {
        return store.get(id);
    }

    public List<ManagedKey> findAll() {
        return new ArrayList<>(store.values());
    }

    public void clearStore() {
        store.clear();
    }
}
