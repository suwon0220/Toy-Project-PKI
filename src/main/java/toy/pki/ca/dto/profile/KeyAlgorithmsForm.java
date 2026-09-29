package toy.pki.ca.dto.profile;

import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

import java.util.Set;

public record KeyAlgorithmsForm (
        Set<KeyAlgorithmPreset> keyAlgorithms
) {

}
