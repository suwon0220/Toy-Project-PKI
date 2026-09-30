package toy.pki.kms.domain.key;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

import java.security.KeyPair;
import java.util.Date;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class ManagedKey {
    private final KeyID id = new KeyID(UUID.randomUUID());
    private final Date createdAt = new Date();
    private final KeyAlgorithmPreset keyAlgorithmPreset;
    private final KeyPair keyPair;
    @Setter private String alias;
}
