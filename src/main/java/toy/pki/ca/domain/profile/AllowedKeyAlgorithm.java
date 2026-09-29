package toy.pki.ca.domain.profile;

import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

import java.util.Set;

public record AllowedKeyAlgorithm(
        Set<KeyAlgorithmPreset> keyAlgorithms
) {

}
