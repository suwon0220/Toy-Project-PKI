package toy.pki.ca.domain.certificate;

import java.util.ArrayList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ManagedX509CertificateRepository {

    private final Map<Long, ManagedX509Certificate> store = new ConcurrentHashMap<>();
    private static long sequence = 0L;

    public Long save(@NonNull ManagedX509Certificate managedX509Certificate) {
        managedX509Certificate.setId(++sequence);
        store.put(sequence, managedX509Certificate);
        return sequence;
    }

    public ManagedX509Certificate findById(@NonNull Long id) {
        return store.get(id);
    }

    public List<ManagedX509Certificate> findAll() {
        return new ArrayList<>(store.values());
    }

    public void clear() {
        store.clear();
    }

}
