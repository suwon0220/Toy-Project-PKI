package toy.pki.kms.domain.parameter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import toy.pki.kms.domain.algorithm.DigestAlgorithm;
import toy.pki.kms.domain.algorithm.SignatureAlgorithm;

@Getter
@AllArgsConstructor
public class SignatureParameter {

    private SignatureAlgorithm signatureAlgorithm;
    private DigestAlgorithm digestAlgorithm;

    public String getAlgorithm() {
        return digestAlgorithm.getJcaName() + "with" + signatureAlgorithm.getJcaName();
    }
}
