package toy.pki.kms.infrastructure.jca;

import toy.pki.kms.domain.algorithm.KeyAlgorithm;
import toy.pki.kms.domain.algorithm.KeyGenerationProfile;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;

import java.security.GeneralSecurityException;

public interface KeyGenerator {

    KeyAlgorithm supports();

    ManagedKey generate(
            KeyGenerationProfile keyGenerationProfile,
            KeyGenerationParameter keyGenerationParameter
    ) throws GeneralSecurityException;

}
