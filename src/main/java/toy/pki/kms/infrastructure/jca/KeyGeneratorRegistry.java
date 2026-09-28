package toy.pki.kms.infrastructure.jca;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import toy.pki.kms.domain.algorithm.KeyAlgorithm;

@Component
public class KeyGeneratorRegistry {

    private final Map<KeyAlgorithm, KeyGenerator> keyGenerators;

    public KeyGeneratorRegistry(List<KeyGenerator> keyGenerators) {
        Map<KeyAlgorithm, KeyGenerator> map = new EnumMap<>(KeyAlgorithm.class);

        for (KeyGenerator keyGenerator : keyGenerators) {
            // Do we need to check for duplicates?
            map.put(keyGenerator.supports(), keyGenerator);
        }

        this.keyGenerators = Map.copyOf(map);
    }

    public KeyGenerator get(KeyAlgorithm keyAlgorithm) {
        // Do we need to check for null?
        return keyGenerators.get(keyAlgorithm);
    }
}
