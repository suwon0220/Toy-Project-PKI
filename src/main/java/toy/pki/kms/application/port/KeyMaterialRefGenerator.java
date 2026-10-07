package toy.pki.kms.application.port;

import java.security.PublicKey;
import toy.pki.kms.domain.KeyMaterialRef;

public interface KeyMaterialRefGenerator {
    KeyMaterialRef generate(PublicKey publicKey);
}
