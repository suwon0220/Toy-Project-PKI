package toy.pki.kms.domain.key;

import lombok.Getter;
import lombok.Setter;
import toy.pki.kms.domain.algorithm.KeyGenerationProfile;

import java.security.KeyPair;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class ManagedKey {

    private final KeyID id;
    private final Date createdAt;
    private final KeyGenerationProfile keyGenerationProfile;
    private final KeyPair keyPair;
    @Setter
    private String alias;

    public ManagedKey(KeyGenerationProfile keyGenerationProfile, KeyPair keyPair) {
        this.id = new KeyID(UUID.randomUUID());
        this.keyGenerationProfile = keyGenerationProfile;
        this.keyPair = keyPair;
        this.createdAt = new Date();
    }
}
