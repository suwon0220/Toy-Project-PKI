package toy.pki.kms.infrastructure.jca;

import java.security.GeneralSecurityException;

import toy.pki.kms.domain.algorithm.KeyAlgorithm;
import toy.pki.kms.domain.algorithm.KeyGenerationProfile;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;

public interface KeyGenerator {

    KeyAlgorithm supports();

    ManagedKey generate(
            KeyGenerationProfile keyGenerationProfile,
            KeyGenerationParameter keyGenerationParameter
    ) throws GeneralSecurityException;

}
