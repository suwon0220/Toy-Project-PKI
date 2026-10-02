package toy.pki.kms.adapter.crypto.memory;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import toy.pki.kms.application.port.GeneratedKeyMaterial;
import toy.pki.kms.application.port.KeyMaterialProvider;
import toy.pki.kms.domain.key.KeyMaterialRef;
import toy.pki.kms.domain.key.KeyProviderId;
import toy.pki.kms.domain.key.genparam.KeyGenerationParameters;
import toy.pki.kms.domain.key.sigparam.SignatureParameters;
import toy.pki.kms.infrastructure.jca.JcaKeyGenerator;
import toy.pki.kms.infrastructure.jca.JcaKeyGeneratorRegistry;

@Component
public class InMemoryKeyMaterialProvider implements KeyMaterialProvider {

    private static final KeyProviderId PROVIDER_ID = new KeyProviderId("in-memory");
    private final Map<KeyMaterialRef, KeyPair> store = new ConcurrentHashMap<>();
    private final JcaKeyGeneratorRegistry jcaKeyGeneratorRegistry;

    public InMemoryKeyMaterialProvider(JcaKeyGeneratorRegistry jcaKeyGeneratorRegistry) {
        this.jcaKeyGeneratorRegistry = jcaKeyGeneratorRegistry;
    }

    @Override
    public KeyProviderId id() {
        return PROVIDER_ID;
    }

    @Override
    public GeneratedKeyMaterial generate(KeyGenerationParameters parameters)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPair keyPair = generateKeyPair(parameters);

        // TODO: Use a proper KeyId generation strategy based on the public key
        KeyMaterialRef reference = new KeyMaterialRef(UUID.randomUUID().toString());

        if (store.putIfAbsent(reference, keyPair) != null) {
            throw new IllegalStateException("Key with reference " + reference + " already exists");
        }

        return new GeneratedKeyMaterial(reference, keyPair.getPublic());
    }

    @Override
    public Optional<PublicKey> getPublicKey(KeyMaterialRef reference) {
        return Optional.ofNullable(store.get(reference)).map(KeyPair::getPublic);
    }

    @Override
    public byte[] sign(KeyMaterialRef reference, SignatureParameters parameters, byte[] data) {
        return new byte[0];
    }

    @Override
    public void delete(KeyMaterialRef reference) {
        store.remove(reference);
    }

    // ----------------------------------------------

    private KeyPair generateKeyPair(KeyGenerationParameters parameters)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        JcaKeyGenerator keyGenerator = jcaKeyGeneratorRegistry.get(parameters);
        return keyGenerator.generate(parameters);
    }
}
