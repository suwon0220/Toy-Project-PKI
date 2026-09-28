package toy.pki.kms.domain.request;

import jakarta.annotation.Nonnull;
import toy.pki.kms.domain.algorithm.KeyGenerationProfile;

public record KeyGenerationRequest(
        @Nonnull
        KeyGenerationProfile keyGenerationProfile
) {

}
