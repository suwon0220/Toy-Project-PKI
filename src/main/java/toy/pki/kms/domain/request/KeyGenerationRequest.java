package toy.pki.kms.domain.request;

import jakarta.annotation.Nonnull;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

public record KeyGenerationRequest(
        @Nonnull
        KeyAlgorithmPreset keyAlgorithmPreset
) {

}
