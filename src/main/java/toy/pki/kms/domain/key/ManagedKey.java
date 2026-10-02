package toy.pki.kms.domain.key;

import java.time.Instant;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ManagedKey {
    private final KeyId keyId;
    private final KeyAlgorithm keyAlgorithm;
    private final KeyMaterialRef keyMaterialRef;
    private final KeyProviderId keyProviderId;
    private final Instant createdAt;
}
