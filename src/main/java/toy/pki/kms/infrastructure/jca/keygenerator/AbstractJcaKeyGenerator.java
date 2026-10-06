package toy.pki.kms.infrastructure.jca.keygenerator;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import toy.pki.kms.adapter.keymaterial.jca.JcaKeyGenerator;
import toy.pki.kms.domain.key.generation.KeyGenerationParameters;

@RequiredArgsConstructor
public abstract class AbstractJcaKeyGenerator implements JcaKeyGenerator {

    @Autowired
    private Provider provider;

    @Override
    public KeyPair generate(KeyGenerationParameters parameters)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(parameters.algorithm().getJcaName(), provider);
        initializeKeyPairGenerator(keyPairGenerator, parameters);
        return keyPairGenerator.generateKeyPair();
    }

    protected abstract void initializeKeyPairGenerator(
        KeyPairGenerator keyPairGenerator,
        KeyGenerationParameters parameters)
        throws InvalidAlgorithmParameterException;
}
