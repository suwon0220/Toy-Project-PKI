package toy.pki.kms.adapter.id.jca;

import jakarta.validation.constraints.NotNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import toy.pki.kms.application.port.KeyIdGenerator;
import toy.pki.kms.domain.KeyId;

@Component
@Validated
public class JcaHashKeyIdGenerator implements KeyIdGenerator {

    @Override
    public KeyId generate(@NotNull PublicKey publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());

            return new KeyId("SHA-256:" + HexFormat.of().formatHex(hash));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
