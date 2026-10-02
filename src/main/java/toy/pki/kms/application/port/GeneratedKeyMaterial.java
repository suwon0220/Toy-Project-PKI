package toy.pki.kms.application.port;

import java.security.PublicKey;
import toy.pki.kms.domain.key.KeyId;
import toy.pki.kms.domain.key.KeyMaterialRef;

public record GeneratedKeyMaterial(
    KeyMaterialRef reference,
    PublicKey publicKey
) {
}
