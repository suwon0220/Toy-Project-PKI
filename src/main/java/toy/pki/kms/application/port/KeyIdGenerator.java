package toy.pki.kms.application.port;

import jakarta.validation.constraints.NotNull;
import java.security.PublicKey;
import toy.pki.kms.domain.KeyId;

public interface KeyIdGenerator {
    KeyId generate(@NotNull PublicKey publicKey);
}
