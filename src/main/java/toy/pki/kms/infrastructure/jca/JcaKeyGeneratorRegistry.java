package toy.pki.kms.infrastructure.jca;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.algorithm.KeyAlgorithm;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import toy.pki.kms.domain.key.genparam.KeyGenerationParameters;

@Component
@RequiredArgsConstructor
public class JcaKeyGeneratorRegistry {

    private final List<JcaKeyGenerator> generators;

    public JcaKeyGenerator get(KeyGenerationParameters parameters) {
        return generators.stream()
                .filter(generator -> generator.supports(parameters))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No JcaKeyGenerator found for parameters: " + parameters));
    }
}
