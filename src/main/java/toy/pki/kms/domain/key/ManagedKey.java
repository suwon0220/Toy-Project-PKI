package toy.pki.kms.domain.key;

import java.security.KeyPair;
import java.util.Date;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import toy.pki.kms.domain.algorithm.KeyGenerationProfile;

@Getter
@Setter
public class ManagedKey {

    private final KeyID id;
    @Setter
    private String alias;
    private final Date createdAt;
    private final KeyGenerationProfile keyGenerationProfile;
    private final KeyPair keyPair;

    public ManagedKey(KeyGenerationProfile keyGenerationProfile, KeyPair keyPair) {
        this.id = new KeyID(UUID.randomUUID());
        this.keyGenerationProfile = keyGenerationProfile;
        this.keyPair = keyPair;
        this.createdAt = new Date();
    }
}
