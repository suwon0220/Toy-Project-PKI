package toy.pki.kms.adapter.keymaterial.jca;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import toy.pki.kms.domain.generation.KeyGenerationParameters;

public interface JcaKeyGenerator {
    boolean supports(KeyGenerationParameters parameters);
    KeyPair generate(KeyGenerationParameters parameters)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;
}
