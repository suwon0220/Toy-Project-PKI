package toy.pki.kms.infrastructure.jca.keygenerator;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import toy.pki.kms.adapter.keymaterial.jca.JcaKeyGenerator;
import toy.pki.kms.domain.key.generation.KeyGenerationParameters;

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
