package toy.pki.kms.domain.algorithm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RsaKeySize {
    RSA_2048(2048),
    RSA_3072(3072),
    RSA_4096(4096);

    private final int bits;
}
