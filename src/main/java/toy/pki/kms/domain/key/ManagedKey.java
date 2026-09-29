package toy.pki.kms.domain.key;

import lombok.Getter;
import lombok.Setter;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

import java.security.KeyPair;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class ManagedKey {

    private final KeyID id;
    private final Date createdAt;
    private final KeyAlgorithmPreset keyAlgorithmPreset;
    private final KeyPair keyPair;
    @Setter
    private String alias;

    public ManagedKey(KeyAlgorithmPreset keyAlgorithmPreset, KeyPair keyPair) {
        this.id = new KeyID(UUID.randomUUID());
        this.keyAlgorithmPreset = keyAlgorithmPreset;
        this.keyPair = keyPair;
        this.createdAt = new Date();
    }
}
