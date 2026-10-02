package toy.pki.kms.infrastructure.jca;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import toy.pki.kms.domain.key.genparam.KeyGenerationParameters;

public interface JcaKeyGenerator {
    public boolean supports(KeyGenerationParameters parameters);
    public KeyPair generate(KeyGenerationParameters parameters)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;
}
