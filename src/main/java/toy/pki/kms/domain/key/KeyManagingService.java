package toy.pki.kms.domain.key;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;
import toy.pki.kms.domain.request.KeyGenerationRequest;
import toy.pki.kms.infrastructure.jca.KeyGenerator;
import toy.pki.kms.infrastructure.jca.KeyGeneratorRegistry;

@Service
@RequiredArgsConstructor
public class KeyManagingService {

    private final ManagedKeyRepository managedKeyRepository;

    public KeyID saveKey(ManagedKey managedKey) {
        ManagedKey savedManagedKey = managedKeyRepository.save(managedKey);
        return savedManagedKey.getId();
    }

    public PublicKey getPublicKey(KeyID keyID) {
        ManagedKey managedKey = managedKeyRepository.findByKeyID(keyID);
        if (managedKey == null) {
            throw new IllegalArgumentException("Key not found for keyID: " + keyID);
        }
        return managedKey.getKeyPair().getPublic();
    }

    public boolean hasKey(KeyID keyID) {
        return managedKeyRepository.findByKeyID(keyID) != null;
    }

    public String getDisplayName(KeyID keyID) {
        ManagedKey managedKey = managedKeyRepository.findByKeyID(keyID);
        if (managedKey == null) {
            throw new IllegalArgumentException("Key not found for keyID: " + keyID);
        }
        return managedKey.getDisplayName();
    }

    public List<KeyID> getKeys() {
        return managedKeyRepository.findAll().stream()
                .map(ManagedKey::getId)
                .toList();
    }

}
