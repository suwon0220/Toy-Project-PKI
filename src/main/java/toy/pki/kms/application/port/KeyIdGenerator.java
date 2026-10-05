package toy.pki.kms.application.port;

import java.security.PublicKey;

import jakarta.validation.constraints.NotNull;
import toy.pki.kms.domain.key.KeyId;

public interface KeyIdGenerator {
    KeyId generate(@NotNull PublicKey publicKey);
}
