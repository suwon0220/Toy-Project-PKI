package toy.pki.kms.domain.key;

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
}
