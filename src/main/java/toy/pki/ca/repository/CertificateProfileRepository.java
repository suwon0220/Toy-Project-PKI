package toy.pki.ca.repository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import toy.pki.ca.domain.profile.Profile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class CertificateProfileRepository {

    private final Map<UUID, Profile> store = new ConcurrentHashMap<>();

    public void save(@NonNull Profile profile) {
        store.put(profile.getProfileId().id(), profile);
    }

    public Profile findById(@NonNull UUID uuid) {
        return store.get(uuid);
    }

    public List<Profile> findAll() {
        return store.values().stream()
                .filter(Objects::nonNull)
                .map(obj -> obj)
                .toList();
    }

    public void clear() {
        store.clear();
    }

}
