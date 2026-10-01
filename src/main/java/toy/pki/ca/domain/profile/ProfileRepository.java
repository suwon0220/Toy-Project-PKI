package toy.pki.ca.domain.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProfileRepository {

    private final Map<Long, Profile> store = new ConcurrentHashMap<>();
    private long sequence = 0L;

    public Long save(@NonNull Profile profile) {
        profile.setId(++sequence);
        store.put(profile.getId(), profile);
        return profile.getId();
    }

    public Long replace(Long id, @NonNull Profile profile) {
        profile.setId(id);
        store.replace(profile.getId(), profile);
        return profile.getId();
    }

    public Profile findById(Long id) {
        return store.get(id);
    }

    public List<Profile> findByStatus(ProfileStatus status) {
        return store.values().stream().filter(profile -> status.equals(profile.getStatus())).toList();
    }

    public List<Profile> findAll() {
        return new ArrayList<>(store.values());
    }

    public void remove(Profile profile) {
        store.remove(profile.getId());
    }


    public void clear() {
        store.clear();
    }
}
