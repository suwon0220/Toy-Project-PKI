package toy.pki.kms.application.model;

import java.security.PublicKey;

import toy.pki.kms.domain.key.KeyMaterialRef;

public record GeneratedKeyMaterial(
    KeyMaterialRef reference,
    PublicKey publicKey
) {
}
