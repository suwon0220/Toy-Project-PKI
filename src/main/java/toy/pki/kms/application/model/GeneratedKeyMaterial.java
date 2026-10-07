package toy.pki.kms.application.model;

import java.security.PublicKey;

import toy.pki.kms.domain.KeyMaterialRef;

public record GeneratedKeyMaterial(
    KeyMaterialRef reference,
    PublicKey publicKey
) {
}
