package toy.pki.kms.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum KeyAlgorithm {
    RSA,
    EC,
    Ed448,
    Ed25519;

    public String getJcaName() {
        return name();
    }

    public static KeyAlgorithm fromJcaName(String jcaName) throws IllegalArgumentException {
        return KeyAlgorithm.valueOf(jcaName);
    }
}
